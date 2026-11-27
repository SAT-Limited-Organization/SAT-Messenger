package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AvailabilityStatus
import com.example.data.model.BlockedUser
import com.example.data.model.ContactEntry
import com.example.data.model.FriendRequest
import com.example.data.model.PrivacySettings
import com.example.data.model.Report
import com.example.data.model.UserProfile
import com.example.util.UserSearchMatcher
import com.example.util.UsernameChangeEvaluation
import com.example.util.UsernamePolicy
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
            val primaryUsername = profile.resolvedPrimaryUsername
            if (!existingSnapshot.exists()) {
                val safeName = profile.displayName.trim()
                    .ifBlank { primaryUsername.ifBlank { "SAT Member" } }
                    .take(100)
                val payload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to profile.email,
                    "primaryUsername" to primaryUsername,
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
                if (existingSnapshot.getString("primaryUsername").isNullOrBlank() && primaryUsername.isNotBlank()) {
                    updates["primaryUsername"] = primaryUsername
                }
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
            val primaryUsername = profile.resolvedPrimaryUsername
            val safeName = profile.displayName.trim()
                .ifBlank { primaryUsername.ifBlank { "SAT Member" } }
                .take(100)
            val payload = mutableMapOf<String, Any>(
                "userId" to uid,
                "displayName" to safeName,
                "email" to profile.email,
                "primaryUsername" to primaryUsername,
                "availabilityStatus" to profile.availabilityStatus,
                "isOnline" to profile.isOnline,
                "lastSeen" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            profile.phoneNumber?.let { payload["phoneNumber"] = it }
            profile.avatarUrl?.let { payload["avatarUrl"] = it }
            profile.statusMessage?.let { payload["statusMessage"] = it }
            profile.secondaryUsername?.takeIf { it.isNotBlank() }?.let {
                payload["secondaryUsername"] = it
                payload["secondaryUsernameLower"] = it.lowercase()
            }
            profile.secondaryUsernameDayKey?.let { payload["secondaryUsernameDayKey"] = it }
            if (profile.secondaryUsernameChangesInDay > 0) {
                payload["secondaryUsernameChangesInDay"] = profile.secondaryUsernameChangesInDay
            }
            profile.secondaryUsernameLastChangedAtMs?.let {
                payload["secondaryUsernameLastChangedAtMs"] = it
            }

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

    suspend fun updateSecondaryUsername(
        candidateRaw: String,
        nowMs: Long = System.currentTimeMillis()
    ): Result<UsernameChangeEvaluation.Allowed> {
        val uid = requireUserId()
        val path = "users/$uid"
        val userDoc = db.collection("users").document(uid)
        return try {
            val snapshot = userDoc.get().await()
            val currentProfile = snapshot.toObject(UserProfile::class.java)
                ?: return Result.failure(IllegalStateException("Profile not synced yet."))

            val normalized = UsernamePolicy.normalize(candidateRaw)
            val lower = normalized.lowercase()

            // Verify uniqueness against all registered users before applying policy
            val conflictDocs = db.collection("users")
                .whereEqualTo("secondaryUsernameLower", lower)
                .get()
                .await()
            val otherClaimedUsers = conflictDocs.toObjects(UserProfile::class.java)

            when (val evaluation = UsernamePolicy.evaluateChange(currentProfile, candidateRaw, nowMs, otherClaimedUsers)) {
                is UsernameChangeEvaluation.Rejected -> {
                    Result.failure(IllegalArgumentException(evaluation.reason))
                }
                is UsernameChangeEvaluation.Allowed -> {
                    val updates = mutableMapOf<String, Any>(
                        "secondaryUsername" to evaluation.normalizedUsername,
                        "secondaryUsernameLower" to evaluation.normalizedLower,
                        "secondaryUsernameDayKey" to evaluation.nextDayKey,
                        "secondaryUsernameChangesInDay" to evaluation.nextChangesInDay,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    evaluation.nextLastChangedAtMs?.let {
                        updates["secondaryUsernameLastChangedAtMs"] = it
                    }
                    userDoc.update(updates).await()
                    Result.success(evaluation)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
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
                    list.filter { user -> UserSearchMatcher.matches(user, trimmed) }
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun fetchAllDirectoryUsers(): List<UserProfile> {
        val path = "users"
        return try {
            db.collection("users").get().await().toObjects(UserProfile::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            emptyList()
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

    suspend fun updateProfileAvatar(avatarUrl: String?): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid"
        return try {
            val cleanUrl = avatarUrl?.trim()?.takeIf { it.isNotBlank() }
            val payload = mapOf<String, Any>(
                "avatarUrl" to (cleanUrl ?: FieldValue.delete()),
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

    // ==========================================
    // Friend Requests & Mutual Friendships
    // ==========================================

    fun observeFriendRequests(userId: String): Flow<List<FriendRequest>> {
        val path = "friendRequests"
        return db.collection("friendRequests")
            .whereArrayContains("participantIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FriendRequest::class.java)
                    .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendFriendRequest(
        targetUser: UserProfile,
        currentProfile: UserProfile
    ): Result<String> {
        val uid = requireUserId()
        if (targetUser.userId.isBlank() || targetUser.userId == uid) {
            return Result.failure(IllegalArgumentException("Cannot send a friend request to yourself."))
        }
        val pairId = FriendRequest.pairId(uid, targetUser.userId)
        val path = "friendRequests/$pairId"
        val docRef = db.collection("friendRequests").document(pairId)
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val existingReq = existing.toObject(FriendRequest::class.java)
                if (existingReq != null) {
                    when (existingReq.status) {
                        "accepted" -> return Result.success("already_friends")
                        "pending" -> {
                            // If target already sent us a pending request, accept it immediately!
                            if (existingReq.toUserId == uid) {
                                docRef.update(
                                    mapOf(
                                        "status" to "accepted",
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                ).await()
                                setContactRelationship(targetUser.userId, "friend")
                                return Result.success("accepted")
                            }
                            return Result.success("pending")
                        }
                        "declined" -> {
                            // Allow re-request by recreating or resetting when sender
                            if (existingReq.fromUserId == uid) {
                                docRef.update(
                                    mapOf(
                                        "status" to "pending",
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                ).await()
                                return Result.success("pending")
                            } else {
                                docRef.delete().await()
                            }
                        }
                    }
                }
            }

            val payload = mutableMapOf<String, Any>(
                "id" to pairId,
                "fromUserId" to uid,
                "fromUserName" to currentProfile.username.take(100),
                "toUserId" to targetUser.userId,
                "toUserName" to targetUser.username.take(100),
                "participantIds" to listOf(uid, targetUser.userId),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            currentProfile.avatarUrl?.takeIf { it.isNotBlank() }?.let {
                payload["fromUserAvatarUrl"] = it
            }
            targetUser.avatarUrl?.takeIf { it.isNotBlank() }?.let {
                payload["toUserAvatarUrl"] = it
            }
            docRef.set(payload).await()
            Result.success("pending")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(request: FriendRequest, accept: Boolean): Result<Unit> {
        val uid = requireUserId()
        val path = "friendRequests/${request.id}"
        return try {
            val nextStatus = if (accept) "accepted" else "declined"
            db.collection("friendRequests").document(request.id)
                .update(
                    mapOf(
                        "status" to nextStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            if (accept) {
                val otherId = if (request.fromUserId == uid) request.toUserId else request.fromUserId
                if (otherId.isNotBlank()) {
                    setContactRelationship(otherId, "friend")
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun cancelOrRemoveFriend(targetUserId: String): Result<Unit> {
        val uid = requireUserId()
        val pairId = FriendRequest.pairId(uid, targetUserId)
        val path = "friendRequests/$pairId"
        return try {
            val docRef = db.collection("friendRequests").document(pairId)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.delete().await()
            }
            try {
                removeSavedContact(targetUserId)
            } catch (_: Exception) {
            }
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
