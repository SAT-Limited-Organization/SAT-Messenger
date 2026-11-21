package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AvailabilityStatus
import com.example.data.model.BlockedUser
import com.example.data.model.ContactEntry
import com.example.data.model.PrivacySettings
import com.example.data.model.Report
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing user actions.")
    }

    suspend fun ensureProfileSynced(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        val userDoc = db.collection("users").document(uid)
        return try {
            val existingSnapshot = userDoc.get().await()
            if (!existingSnapshot.exists()) {
                val safeName = profile.displayName.trim()
                    .ifBlank { profile.email.substringBefore("@").ifBlank { "SAT Member" } }
                    .take(100)
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to profile.email,
                    "availabilityStatus" to profile.availabilityStatus,
                    "isOnline" to true,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                profile.phoneNumber?.let { payload["phoneNumber"] = it }
                profile.avatarUrl?.let { payload["avatarUrl"] = it }
                profile.statusMessage?.let { payload["statusMessage"] = it }
                userDoc.set(payload).await()
            } else {
                val existingStatus = existingSnapshot.getString("availabilityStatus")
                val nextStatus = if (existingStatus == AvailabilityStatus.OFFLINE.label || existingStatus.isNullOrBlank()) {
                    AvailabilityStatus.AVAILABLE.label
                } else {
                    existingStatus
                }
                val updates = mutableMapOf<String, Any>(
                    "isOnline" to true,
                    "availabilityStatus" to nextStatus,
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (existingSnapshot.getString("avatarUrl").isNullOrBlank() && !profile.avatarUrl.isNullOrBlank()) {
                    updates["avatarUrl"] = profile.avatarUrl
                }
                userDoc.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        val userDoc = db.collection("users").document(uid)
        return try {
            val existingSnapshot = userDoc.get().await()
            val safeName = profile.displayName.trim()
                .ifBlank { profile.email.substringBefore("@").ifBlank { "SAT Member" } }
                .take(100)
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to safeName,
                "email" to profile.email,
                "availabilityStatus" to profile.availabilityStatus,
                "isOnline" to profile.isOnline,
                "lastSeen" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            profile.phoneNumber?.let { payload["phoneNumber"] = it }
            profile.avatarUrl?.let { payload["avatarUrl"] = it }
            profile.statusMessage?.let { payload["statusMessage"] = it }

            if (!existingSnapshot.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                userDoc.set(payload).await()
            } else {
                userDoc.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        val path = "users/$userId"
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    fun searchUsers(query: String): Flow<List<UserProfile>> {
        val path = "users"
        val trimmed = query.trim()
        return db.collection("users")
            .snapshots()
            .map { snapshot ->
                val list = snapshot.toObjects(UserProfile::class.java)
                if (trimmed.isBlank()) {
                    list
                } else {
                    list.filter { user ->
                        user.displayName.contains(trimmed, ignoreCase = true) ||
                                user.email.contains(trimmed, ignoreCase = true) ||
                                (user.phoneNumber != null && user.phoneNumber.contains(trimmed, ignoreCase = true)) ||
                                (user.statusMessage?.contains(trimmed, ignoreCase = true) == true)
                    }
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun updateUserProfile(displayName: String, statusMessage: String, phoneNumber: String? = null): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val payload = mutableMapOf<String, Any>(
                "displayName" to displayName,
                "statusMessage" to statusMessage,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (phoneNumber != null) {
                payload["phoneNumber"] = phoneNumber
            }
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePresence(isOnline: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No current user"))
        val path = "users/$uid"
        return try {
            val payload = mutableMapOf<String, Any>(
                "isOnline" to isOnline,
                "lastSeen" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAvailabilityStatus(status: AvailabilityStatus): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "availabilityStatus" to status.label,
                    "isOnline" to (status != AvailabilityStatus.OFFLINE),
                    "lastSeen" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeSavedContacts(userId: String): Flow<List<ContactEntry>> {
        val path = "users/$userId/contacts"
        return db.collection("users").document(userId)
            .collection("contacts")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ContactEntry::class.java)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun setContactRelationship(
        targetUserId: String,
        relationship: String = "friend",
        nickname: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/contacts/$targetUserId"
        val docRef = db.collection("users").document(uid)
            .collection("contacts").document(targetUserId)
        return try {
            val existing = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "contactUserId" to targetUserId,
                "relationship" to relationship,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            nickname?.trim()?.takeIf { it.isNotEmpty() }?.let {
                payload["nickname"] = it.take(80)
            }
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.update(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeSavedContact(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/contacts/$targetUserId"
        return try {
            db.collection("users").document(uid)
                .collection("contacts").document(targetUserId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacySettings(settings: PrivacySettings): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/settings/privacy"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "readReceipts" to settings.readReceipts,
                "lastSeenVisible" to settings.lastSeenVisible,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("settings").document("privacy")
                .set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observePrivacySettings(userId: String): Flow<PrivacySettings> {
        val path = "users/$userId/settings/privacy"
        return db.collection("users").document(userId)
            .collection("settings").document("privacy")
            .snapshots()
            .map { it.toObject(PrivacySettings::class.java) ?: PrivacySettings(userId = userId) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(PrivacySettings(userId = userId))
            }
    }

    suspend fun blockUser(targetUserId: String, targetUserName: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/blocked/$targetUserId"
        return try {
            val payload = mapOf(
                "blockedUserId" to targetUserId,
                "blockedUserName" to targetUserName,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("blocked").document(targetUserId)
                .set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/blocked/$targetUserId"
        return try {
            db.collection("users").document(uid)
                .collection("blocked").document(targetUserId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeBlockedUsers(userId: String): Flow<List<BlockedUser>> {
        val path = "users/$userId/blocked"
        return db.collection("users").document(userId)
            .collection("blocked")
            .snapshots()
            .map { it.toObjects(BlockedUser::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun reportUser(reportedUserId: String, reason: String, details: String): Result<Unit> {
        val uid = requireUserId()
        val reportId = UUID.randomUUID().toString()
        val path = "reports/$reportId"
        return try {
            val payload = mapOf(
                "id" to reportId,
                "reporterId" to uid,
                "reportedUserId" to reportedUserId,
                "reason" to reason,
                "details" to details,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("reports").document(reportId).set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).delete().await()
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
