package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Channel
import com.example.data.model.ChannelPost
import com.example.data.model.Community
import com.example.data.model.StatusItem
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class SocialFeaturesRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated to perform this operation.")
    }

    // ==========================================
    // 1. Statuses (WhatsApp 24-hr disappearing updates)
    // ==========================================

    fun observeStatuses(): Flow<List<StatusItem>> {
        val path = "statuses"
        val now = Timestamp.now()
        return db.collection("statuses")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StatusItem::class.java).filter { status ->
                    // Exclude statuses that expired (24 hours)
                    status.expiresAt == null || status.expiresAt.seconds > now.seconds
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun postStatus(
        userName: String,
        userAvatarUrl: String?,
        text: String?,
        mediaUrl: String?,
        backgroundColor: String? = "#0A2540"
    ): Result<String> {
        val uid = requireUserId()
        val statusId = UUID.randomUUID().toString()
        val path = "statuses/$statusId"
        return try {
            val nowMs = System.currentTimeMillis()
            val expiresAtDate = Date(nowMs + 24 * 60 * 60 * 1000L) // 24 hours later
            val payload = mutableMapOf<String, Any>(
                "id" to statusId,
                "userId" to uid,
                "userName" to userName,
                "viewerIds" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to Timestamp(expiresAtDate)
            )
            userAvatarUrl?.let { payload["userAvatarUrl"] = it }
            text?.let { payload["text"] = it }
            mediaUrl?.let { payload["mediaUrl"] = it }
            backgroundColor?.let { payload["backgroundColor"] = it }

            db.collection("statuses").document(statusId).set(payload).await()
            Result.success(statusId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markStatusViewed(statusId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "statuses/$statusId"
        return try {
            db.collection("statuses").document(statusId)
                .update("viewerIds", FieldValue.arrayUnion(uid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStatus(statusId: String): Result<Unit> {
        val path = "statuses/$statusId"
        return try {
            db.collection("statuses").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // ==========================================
    // 2. Channels (WhatsApp-style Broadcast Channels)
    // ==========================================

    fun observeChannels(): Flow<List<Channel>> {
        val path = "channels"
        return db.collection("channels")
            .orderBy("followerCount", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(Channel::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun createChannel(
        name: String,
        description: String?,
        iconUrl: String?,
        category: String = "General",
        isPublic: Boolean = true,
        allowFollowerPosts: Boolean = false,
        allowReactions: Boolean = true
    ): Result<String> {
        val uid = requireUserId()
        val channelId = UUID.randomUUID().toString()
        val inviteCode = InviteRepository.generateInviteCode("CHN")
        val path = "channels/$channelId"
        return try {
            val payload = mutableMapOf<String, Any>(
                "id" to channelId,
                "name" to name.trim(),
                "category" to category.trim().ifBlank { "General" },
                "isPublic" to isPublic,
                "allowFollowerPosts" to allowFollowerPosts,
                "allowReactions" to allowReactions,
                "inviteCode" to inviteCode,
                "creatorId" to uid,
                "adminIds" to listOf(uid),
                "subscriberIds" to listOf(uid),
                "followerCount" to 1,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            description?.takeIf { it.isNotBlank() }?.let { payload["description"] = it.trim() }
            iconUrl?.takeIf { it.isNotBlank() }?.let { payload["iconUrl"] = it }

            db.collection("channels").document(channelId).set(payload).await()
            InviteRepository.createInviteDocument(
                db = db,
                code = inviteCode,
                targetType = "channel",
                targetId = channelId,
                targetName = name.trim(),
                targetDescription = description?.trim(),
                creatorId = uid
            )
            Result.success(channelId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateChannelSettings(
        channelId: String,
        name: String,
        description: String?,
        category: String,
        isPublic: Boolean,
        allowFollowerPosts: Boolean,
        allowReactions: Boolean,
        inviteCode: String? = null
    ): Result<Unit> {
        val path = "channels/$channelId"
        return try {
            val updates = mutableMapOf<String, Any>(
                "name" to name.trim(),
                "category" to category.trim().ifBlank { "General" },
                "isPublic" to isPublic,
                "allowFollowerPosts" to allowFollowerPosts,
                "allowReactions" to allowReactions,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanDesc = description?.trim()
            if (!cleanDesc.isNullOrBlank()) {
                updates["description"] = cleanDesc
            } else {
                updates["description"] = FieldValue.delete()
            }

            db.collection("channels").document(channelId).update(updates).await()

            if (!inviteCode.isNullOrBlank()) {
                try {
                    val inviteUpdates = mutableMapOf<String, Any>(
                        "targetName" to name.trim().take(120)
                    )
                    if (!cleanDesc.isNullOrBlank()) {
                        inviteUpdates["targetDescription"] = cleanDesc.take(500)
                    }
                    db.collection("invites").document(inviteCode).update(inviteUpdates).await()
                } catch (_: Exception) {
                    // Best-effort sync of invite metadata
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChannel(channelId: String, inviteCode: String? = null): Result<Unit> {
        val path = "channels/$channelId"
        return try {
            val postsSnapshot = db.collection("channels").document(channelId)
                .collection("posts").get().await()
            for (doc in postsSnapshot.documents) {
                try {
                    doc.reference.delete().await()
                } catch (_: Exception) {
                }
            }
            db.collection("channels").document(channelId).delete().await()
            if (!inviteCode.isNullOrBlank()) {
                try {
                    db.collection("invites").document(inviteCode).delete().await()
                } catch (_: Exception) {
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollowChannel(channel: Channel): Result<Boolean> {
        val uid = requireUserId()
        val path = "channels/${channel.id}"
        val isFollowing = channel.subscriberIds.contains(uid)
        return try {
            if (isFollowing) {
                val updatedCount = (channel.subscriberIds.size - 1).coerceAtLeast(0)
                db.collection("channels").document(channel.id).update(
                    mapOf(
                        "subscriberIds" to FieldValue.arrayRemove(uid),
                        "followerCount" to updatedCount
                    )
                ).await()
                Result.success(false)
            } else {
                val updatedCount = channel.subscriberIds.size + 1
                db.collection("channels").document(channel.id).update(
                    mapOf(
                        "subscriberIds" to FieldValue.arrayUnion(uid),
                        "followerCount" to updatedCount
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeChannelPosts(channelId: String): Flow<List<ChannelPost>> {
        val path = "channels/$channelId/posts"
        return db.collection("channels").document(channelId)
            .collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(ChannelPost::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun postToChannel(
        channelId: String,
        authorName: String,
        text: String,
        mediaUrl: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val postId = UUID.randomUUID().toString()
        val path = "channels/$channelId/posts/$postId"
        return try {
            val payload = mutableMapOf<String, Any>(
                "id" to postId,
                "channelId" to channelId,
                "authorId" to uid,
                "authorName" to authorName,
                "text" to text.trim(),
                "reactions" to emptyMap<String, Int>(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            mediaUrl?.takeIf { it.isNotBlank() }?.let { payload["mediaUrl"] = it }

            db.collection("channels").document(channelId)
                .collection("posts").document(postId)
                .set(payload).await()
            Result.success(postId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChannelPost(channelId: String, postId: String): Result<Unit> {
        val path = "channels/$channelId/posts/$postId"
        return try {
            db.collection("channels").document(channelId)
                .collection("posts").document(postId)
                .delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun reactToPost(
        channelId: String,
        postId: String,
        currentReactions: Map<String, Int>,
        emoji: String
    ): Result<Unit> {
        val path = "channels/$channelId/posts/$postId"
        return try {
            val newReactions = currentReactions.toMutableMap()
            val currentCount = newReactions[emoji] ?: 0
            newReactions[emoji] = currentCount + 1
            db.collection("channels").document(channelId)
                .collection("posts").document(postId)
                .update("reactions", newReactions).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ==========================================
    // 3. Communities (WhatsApp-style Communities)
    // ==========================================

    fun observeCommunities(): Flow<List<Community>> {
        val path = "communities"
        return db.collection("communities")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { it.toObjects(Community::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun createCommunity(
        name: String,
        description: String?,
        iconUrl: String?,
        subgroupChatIds: List<String>
    ): Result<String> {
        val uid = requireUserId()
        val communityId = UUID.randomUUID().toString()
        val inviteCode = InviteRepository.generateInviteCode("COM")
        val path = "communities/$communityId"
        return try {
            val payload = mutableMapOf<String, Any>(
                "id" to communityId,
                "name" to name.trim(),
                "inviteCode" to inviteCode,
                "creatorId" to uid,
                "adminIds" to listOf(uid),
                "memberIds" to listOf(uid),
                "subgroupChatIds" to subgroupChatIds,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            description?.takeIf { it.isNotBlank() }?.let { payload["description"] = it.trim() }
            iconUrl?.takeIf { it.isNotBlank() }?.let { payload["iconUrl"] = it }

            db.collection("communities").document(communityId).set(payload).await()
            InviteRepository.createInviteDocument(
                db = db,
                code = inviteCode,
                targetType = "community",
                targetId = communityId,
                targetName = name.trim(),
                targetDescription = description?.trim(),
                creatorId = uid
            )
            Result.success(communityId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun joinCommunity(communityId: String): Result<Unit> {
        val uid = requireUserId()
        val path = "communities/$communityId"
        return try {
            db.collection("communities").document(communityId)
                .update("memberIds", FieldValue.arrayUnion(uid))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
