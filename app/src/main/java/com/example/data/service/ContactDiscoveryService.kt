package com.example.data.service

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.model.UserProfile
import com.example.data.repository.DeviceContactInfo
import com.example.data.repository.DeviceContactMatch
import com.example.data.repository.DeviceContactsRepository
import com.example.util.UserSearchMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Represents a correlated device address-book contact matched with an existing SAT Messenger user.
 */
data class FindFriendsEntry(
    val user: UserProfile,
    val deviceContactName: String,
    val matchedVia: String, // "Email", "Phone", or "Phone & Email"
    val matchedIdentifier: String = "",
    val devicePhotoUri: String? = null
) {
    fun toDeviceContactMatch(): DeviceContactMatch = DeviceContactMatch(
        user = user,
        deviceContactName = deviceContactName,
        matchedVia = matchedVia
    )
}

data class FindFriendsDirectoryState(
    val hasPermission: Boolean = false,
    val isScanning: Boolean = false,
    val entries: List<FindFriendsEntry> = emptyList(),
    val scannedContactCount: Int = 0,
    val lastSyncedAtMs: Long? = null,
    val errorMessage: String? = null
)

/**
 * Service that scans device contacts via [ContactsContract] and correlates them with registered
 * app users by normalized phone number or email address to populate the 'Find Friends' directory.
 */
class ContactDiscoveryService {

    private val scanMutex = Mutex()
    private val _directoryState = MutableStateFlow(FindFriendsDirectoryState())
    val directoryState: StateFlow<FindFriendsDirectoryState> = _directoryState.asStateFlow()

    @Volatile
    private var cachedDeviceContacts: List<DeviceContactInfo> = emptyList()

    private var contactsObserver: ContentObserver? = null
    private var observedResolver: ContentResolver? = null

    fun hasReadContactsPermission(context: Context): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        _directoryState.update { it.copy(hasPermission = granted) }
        return granted
    }

    /**
     * Scans device contacts via [ContactsContract.CommonDataKinds.Phone] and
     * [ContactsContract.CommonDataKinds.Email], then correlates them against [directoryUsers]
     * (falling back to [fetchUsersFallback] if [directoryUsers] is empty).
     */
    suspend fun scanAndPopulateDirectory(
        context: Context,
        directoryUsers: List<UserProfile>,
        currentUserId: String,
        fetchUsersFallback: (suspend () -> List<UserProfile>)? = null
    ): FindFriendsDirectoryState {
        val granted = hasReadContactsPermission(context)
        if (!granted) {
            val deniedState = FindFriendsDirectoryState(
                hasPermission = false,
                isScanning = false,
                entries = emptyList(),
                scannedContactCount = 0
            )
            _directoryState.value = deniedState
            return deniedState
        }

        return scanMutex.withLock {
            _directoryState.update { it.copy(hasPermission = true, isScanning = true, errorMessage = null) }
            try {
                val scanned = scanDeviceContactsViaContactsContract(context)
                cachedDeviceContacts = scanned

                val candidateUsers = if (directoryUsers.isNotEmpty() || fetchUsersFallback == null) {
                    directoryUsers
                } else {
                    fetchUsersFallback()
                }

                val correlated = correlateContactsWithUsers(
                    deviceContacts = scanned,
                    registeredUsers = candidateUsers,
                    currentUserId = currentUserId
                )

                val readyState = FindFriendsDirectoryState(
                    hasPermission = true,
                    isScanning = false,
                    entries = correlated,
                    scannedContactCount = scanned.size,
                    lastSyncedAtMs = System.currentTimeMillis(),
                    errorMessage = null
                )
                _directoryState.value = readyState
                readyState
            } catch (e: SecurityException) {
                val errState = _directoryState.value.copy(
                    hasPermission = false,
                    isScanning = false,
                    errorMessage = "Contacts permission is required to scan your address book."
                )
                _directoryState.value = errState
                errState
            } catch (e: Exception) {
                val errState = _directoryState.value.copy(
                    isScanning = false,
                    errorMessage = e.localizedMessage ?: "Failed to scan device contacts."
                )
                _directoryState.value = errState
                errState
            }
        }
    }

    /**
     * Re-runs correlation against cached scanned device contacts whenever the registered
     * user directory updates in real time, avoiding redundant ContentResolver scans.
     */
    fun recorrelateWithRegisteredUsers(
        registeredUsers: List<UserProfile>,
        currentUserId: String
    ): List<FindFriendsEntry> {
        val snapshotContacts = cachedDeviceContacts
        if (snapshotContacts.isEmpty()) return _directoryState.value.entries

        val correlated = correlateContactsWithUsers(
            deviceContacts = snapshotContacts,
            registeredUsers = registeredUsers,
            currentUserId = currentUserId
        )
        _directoryState.update { state ->
            state.copy(
                entries = correlated,
                scannedContactCount = snapshotContacts.size
            )
        }
        return correlated
    }

    fun clear() {
        cachedDeviceContacts = emptyList()
        _directoryState.value = FindFriendsDirectoryState()
    }

    fun startObservingContacts(context: Context, onContactsChanged: () -> Unit) {
        if (!hasReadContactsPermission(context)) return
        if (contactsObserver != null) return

        val resolver = context.applicationContext.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onContactsChanged()
            }
        }
        try {
            resolver.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, observer)
            contactsObserver = observer
            observedResolver = resolver
        } catch (_: SecurityException) {
        }
    }

    fun stopObservingContacts() {
        val observer = contactsObserver ?: return
        try {
            observedResolver?.unregisterContentObserver(observer)
        } catch (_: Exception) {
        } finally {
            contactsObserver = null
            observedResolver = null
        }
    }

    /**
     * Queries Android's [ContactsContract] for phone numbers (including normalized E.164 variants)
     * and email addresses across all visible device contacts.
     */
    suspend fun scanDeviceContactsViaContactsContract(context: Context): List<DeviceContactInfo> =
        withContext(Dispatchers.IO) {
            val byContactKey = linkedMapOf<String, MutableScannedContact>()
            val resolver = context.contentResolver

            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val contactId = if (idIdx != -1) cursor.getString(idIdx).orEmpty() else ""
                    val name = if (nameIdx != -1) cursor.getString(nameIdx).orEmpty().trim() else ""
                    val rawNum = if (numIdx != -1) cursor.getString(numIdx).orEmpty().trim() else ""
                    val normNum = if (normIdx != -1) cursor.getString(normIdx).orEmpty().trim() else ""
                    val photoUri = if (photoIdx != -1) cursor.getString(photoIdx)?.takeIf { it.isNotBlank() } else null

                    val variants = linkedSetOf<String>()
                    variants.addAll(DeviceContactsRepository.phoneMatchVariants(rawNum))
                    variants.addAll(DeviceContactsRepository.phoneMatchVariants(normNum))

                    if (variants.isNotEmpty()) {
                        val key = contactId.ifBlank { name.ifBlank { rawNum.ifBlank { normNum } } }
                        val entry = byContactKey.getOrPut(key) {
                            MutableScannedContact(displayName = name.ifBlank { rawNum.ifBlank { normNum } })
                        }
                        if (entry.displayName.isBlank() && name.isNotBlank()) {
                            entry.displayName = name
                        }
                        if (entry.photoUri == null && photoUri != null) {
                            entry.photoUri = photoUri
                        }
                        entry.phones.addAll(variants)
                    }
                }
            }

            resolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Email.ADDRESS,
                    ContactsContract.CommonDataKinds.Email.PHOTO_THUMBNAIL_URI
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
                val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val contactId = if (idIdx != -1) cursor.getString(idIdx).orEmpty() else ""
                    val name = if (nameIdx != -1) cursor.getString(nameIdx).orEmpty().trim() else ""
                    val email = if (emailIdx != -1) cursor.getString(emailIdx).orEmpty().trim().lowercase() else ""
                    val photoUri = if (photoIdx != -1) cursor.getString(photoIdx)?.takeIf { it.isNotBlank() } else null

                    if (email.contains("@")) {
                        val key = contactId.ifBlank { name.ifBlank { email } }
                        val entry = byContactKey.getOrPut(key) {
                            MutableScannedContact(displayName = name.ifBlank { email })
                        }
                        if (entry.displayName.isBlank() && name.isNotBlank()) {
                            entry.displayName = name
                        }
                        if (entry.photoUri == null && photoUri != null) {
                            entry.photoUri = photoUri
                        }
                        entry.emails.add(email)
                    }
                }
            }

            byContactKey.values.map {
                DeviceContactInfo(
                    displayName = it.displayName,
                    normalizedPhones = it.phones.toSet(),
                    emails = it.emails.toSet(),
                    photoUri = it.photoUri
                )
            }
        }

    /**
     * Pure correlation function matching scanned device contacts against existing app users
     * via email address or normalized phone number.
     */
    fun correlateContactsWithUsers(
        deviceContacts: List<DeviceContactInfo>,
        registeredUsers: List<UserProfile>,
        currentUserId: String
    ): List<FindFriendsEntry> {
        if (deviceContacts.isEmpty() || registeredUsers.isEmpty()) return emptyList()

        val phoneIndex = hashMapOf<String, DeviceContactInfo>()
        val emailIndex = hashMapOf<String, DeviceContactInfo>()

        for (contact in deviceContacts) {
            for (phoneVariant in contact.normalizedPhones) {
                if (phoneVariant.length >= 7) {
                    phoneIndex.putIfAbsent(phoneVariant, contact)
                }
            }
            for (email in contact.emails) {
                val clean = email.trim().lowercase()
                if (clean.isNotBlank()) {
                    emailIndex.putIfAbsent(clean, contact)
                }
            }
        }

        val entries = mutableListOf<FindFriendsEntry>()
        for (user in registeredUsers) {
            if (user.userId == currentUserId) continue

            val userEmail = user.email.trim().lowercase()
            val emailContact = if (userEmail.isNotBlank()) emailIndex[userEmail] else null

            val userDigits = UserSearchMatcher.normalizePhoneDigits(user.phoneNumber)
            val phoneContact = if (userDigits.length >= 7) {
                DeviceContactsRepository.phoneMatchVariants(userDigits)
                    .firstNotNullOfOrNull { phoneIndex[it] }
            } else {
                null
            }

            if (emailContact != null || phoneContact != null) {
                val matchedVia = when {
                    emailContact != null && phoneContact != null -> "Phone & Email"
                    emailContact != null -> "Email"
                    else -> "Phone"
                }
                val matchedIdentifier = when {
                    phoneContact != null && !user.phoneNumber.isNullOrBlank() -> user.phoneNumber.orEmpty()
                    emailContact != null -> user.email
                    else -> ""
                }
                val bestContact = phoneContact ?: emailContact!!
                entries.add(
                    FindFriendsEntry(
                        user = user,
                        deviceContactName = bestContact.displayName.ifBlank { "Device Contact" },
                        matchedVia = matchedVia,
                        matchedIdentifier = matchedIdentifier,
                        devicePhotoUri = bestContact.photoUri
                    )
                )
            }
        }

        return entries.sortedWith(
            compareByDescending<FindFriendsEntry> { it.user.isOnline }
                .thenBy { it.deviceContactName.lowercase() }
                .thenBy { it.user.username.lowercase() }
        )
    }

    private class MutableScannedContact(
        var displayName: String,
        val phones: LinkedHashSet<String> = linkedSetOf(),
        val emails: LinkedHashSet<String> = linkedSetOf(),
        var photoUri: String? = null
    )
}
