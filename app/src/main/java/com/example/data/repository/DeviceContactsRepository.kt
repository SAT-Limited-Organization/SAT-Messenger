package com.example.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.example.data.model.UserProfile
import com.example.util.UserSearchMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContactInfo(
    val displayName: String,
    val normalizedPhones: Set<String> = emptySet(),
    val emails: Set<String> = emptySet(),
    val photoUri: String? = null
)

data class DeviceContactMatch(
    val user: UserProfile,
    val deviceContactName: String,
    val matchedVia: String // "Phone" or "Email"
)

class DeviceContactsRepository {

    suspend fun loadDeviceContacts(context: Context): List<DeviceContactInfo> =
        withContext(Dispatchers.IO) {
            val byContactId = linkedMapOf<String, MutableDeviceContact>()
            val resolver = context.contentResolver

            try {
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val contactId = if (idIdx != -1) cursor.getString(idIdx).orEmpty() else ""
                        val name = if (nameIdx != -1) cursor.getString(nameIdx).orEmpty().trim() else ""
                        val rawNum = if (numIdx != -1) cursor.getString(numIdx).orEmpty() else ""
                        val digits = UserSearchMatcher.normalizePhoneDigits(rawNum)
                        if (digits.length >= 7) {
                            val key = contactId.ifBlank { name.ifBlank { digits } }
                            val entry = byContactId.getOrPut(key) {
                                MutableDeviceContact(name.ifBlank { rawNum })
                            }
                            if (entry.displayName.isBlank() && name.isNotBlank()) {
                                entry.displayName = name
                            }
                            entry.phones.addAll(phoneMatchVariants(digits))
                        }
                    }
                }

                resolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Email.ADDRESS
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
                    val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    while (cursor.moveToNext()) {
                        val contactId = if (idIdx != -1) cursor.getString(idIdx).orEmpty() else ""
                        val name = if (nameIdx != -1) cursor.getString(nameIdx).orEmpty().trim() else ""
                        val email = if (emailIdx != -1) cursor.getString(emailIdx).orEmpty().trim().lowercase() else ""
                        if (email.contains("@")) {
                            val key = contactId.ifBlank { name.ifBlank { email } }
                            val entry = byContactId.getOrPut(key) {
                                MutableDeviceContact(name.ifBlank { email })
                            }
                            if (entry.displayName.isBlank() && name.isNotBlank()) {
                                entry.displayName = name
                            }
                            entry.emails.add(email)
                        }
                    }
                }
            } catch (_: SecurityException) {
                return@withContext emptyList()
            } catch (_: Exception) {
                return@withContext emptyList()
            }

            byContactId.values.map {
                DeviceContactInfo(
                    displayName = it.displayName,
                    normalizedPhones = it.phones.toSet(),
                    emails = it.emails.toSet()
                )
            }
        }

    fun matchRegisteredUsers(
        deviceContacts: List<DeviceContactInfo>,
        directoryUsers: List<UserProfile>,
        currentUserId: String
    ): Map<String, DeviceContactMatch> {
        if (deviceContacts.isEmpty() || directoryUsers.isEmpty()) return emptyMap()

        val phoneIndex = hashMapOf<String, String>()
        val emailIndex = hashMapOf<String, String>()
        for (contact in deviceContacts) {
            val label = contact.displayName.ifBlank { "Device Contact" }
            for (phoneVariant in contact.normalizedPhones) {
                if (phoneVariant.length >= 7) {
                    phoneIndex.putIfAbsent(phoneVariant, label)
                }
            }
            for (email in contact.emails) {
                val clean = email.trim().lowercase()
                if (clean.isNotBlank()) {
                    emailIndex.putIfAbsent(clean, label)
                }
            }
        }

        val matches = linkedMapOf<String, DeviceContactMatch>()
        for (user in directoryUsers) {
            if (user.userId == currentUserId) continue
            val userEmail = user.email.trim().lowercase()
            val emailMatchName = if (userEmail.isNotBlank()) emailIndex[userEmail] else null
            if (emailMatchName != null) {
                matches[user.userId] = DeviceContactMatch(
                    user = user,
                    deviceContactName = emailMatchName,
                    matchedVia = "Email"
                )
                continue
            }

            val userDigits = UserSearchMatcher.normalizePhoneDigits(user.phoneNumber)
            if (userDigits.length >= 7) {
                val variants = phoneMatchVariants(userDigits)
                val phoneMatchName = variants.firstNotNullOfOrNull { phoneIndex[it] }
                if (phoneMatchName != null) {
                    matches[user.userId] = DeviceContactMatch(
                        user = user,
                        deviceContactName = phoneMatchName,
                        matchedVia = "Phone"
                    )
                }
            }
        }
        return matches
    }

    companion object {
        fun phoneMatchVariants(rawOrDigits: String): Set<String> {
            val digits = UserSearchMatcher.normalizePhoneDigits(rawOrDigits)
            if (digits.length < 7) return emptySet()
            val variants = linkedSetOf(digits)
            if (digits.length >= 10) {
                variants.add(digits.takeLast(10))
            }
            if (digits.length >= 9) {
                variants.add(digits.takeLast(9))
            }
            variants.add(digits.takeLast(7))
            return variants
        }
    }

    private class MutableDeviceContact(
        var displayName: String,
        val phones: LinkedHashSet<String> = linkedSetOf(),
        val emails: LinkedHashSet<String> = linkedSetOf()
    )
}
