package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Chat
import com.example.data.model.ChatRoom
import com.example.data.model.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing chats.")
    }

    private suspend fun resolveChatCollection(chatId: String): String {
        return try {
            val roomDoc = db.collection("chatRooms").document(chatId).get().await()
            if (roomDoc.exists()) "chatRooms" else "chats"
        } catch (e: Exception) {
            "chats"
        }
    }

    fun observeUserChats(userId: String): Flow<List<Chat>> {
        val chatsFlow = db.collection("chats")
            .whereArrayContains("memberIds", userId)
            .snapshots()
            .map { it.toObjects(Chat::class.java) }
            .catch { emit(emptyList()) }

        val roomsFlow = db.collection("chatRooms")
            .whereArrayContains("memberIds", userId)
            .snapshots()
            .map { it.toObjects(ChatRoom::class.java).map { room -> room.toChat() } }
            .catch { emit(emptyList()) }

        return combine(chatsFlow, roomsFlow) { directChats, groupRooms ->
            (directChats + groupRooms).sortedByDescending {
                it.lastTimestamp?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
            }
        }
    }

    fun observeUserChatRooms(userId: String): Flow<List<ChatRoom>> {
        val path = "chatRooms"
        return db.collection(path)
            .whereArrayContains("memberIds", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatRoom::class.java).sortedByDescending {
                    it.lastTimestamp?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L
                }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    fun observeChat(chatId: String): Flow<Chat?> {
        val chatFlow = db.collection("chats").document(chatId)
            .snapshots()
            .map { it.toObject(Chat::class.java) }
            .catch { emit(null) }

        val roomFlow = db.collection("chatRooms").document(chatId)
            .snapshots()
            .map { it.toObject(ChatRoom::class.java)?.toChat() }
            .catch { emit(null) }

        return combine(chatFlow, roomFlow) { directChat, roomChat ->
            directChat ?: roomChat
        }
    }

    suspend fun setTyping(chatId: String, isTyping: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No current user"))
        val collection = resolveChatCollection(chatId)
        val path = "$collection/$chatId"
        return try {
            db.collection(collection).document(chatId).update(
                mapOf(
                    "isTyping.$uid" to isTyping
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                db.collection(collection).document(chatId).set(
                    mapOf("isTyping" to mapOf(uid to isTyping)),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
                Result.success(Unit)
            } catch (mergeError: Exception) {
                handleFirestoreError(mergeError, OperationType.UPDATE, path)
                Result.failure(mergeError)
            }
        }
    }

    suspend fun getOrCreateDirectChat(
        targetUserId: String,
        targetUserName: String,
        currentUserName: String
    ): Result<String> {
        val uid = requireUserId()
        val path = "chats"
        return try {
            val existing = db.collection(path)
                .whereArrayContains("memberIds", uid)
                .get()
                .await()

            val found = existing.documents.firstOrNull { doc ->
                val type = doc.getString("type") ?: "direct"
                val members = (doc.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                type == "direct" && members.contains(targetUserId)
            }

            if (found != null && found.id.isNotBlank()) {
                Result.success(found.id)
            } else {
                val chatId = UUID.randomUUID().toString()
                val payload = mutableMapOf<String, Any>(
                    "id" to chatId,
                    "type" to "direct",
                    "memberIds" to listOf(uid, targetUserId),
                    "adminIds" to listOf(uid),
                    "participantNames" to mapOf(uid to currentUserName, targetUserId to targetUserName),
                    "lastMessageText" to "Conversation started",
                    "lastSenderId" to uid,
                    "lastSenderName" to currentUserName,
                    "isTyping" to emptyMap<String, Boolean>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastTimestamp" to FieldValue.serverTimestamp()
                )
                db.collection(path).document(chatId).set(payload).await()
                Result.success(chatId)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupRoom(
        name: String,
        description: String? = null,
        selectedMemberIds: List<String>,
        currentUserName: String
    ): Result<String> {
        val uid = requireUserId()
        val roomId = UUID.randomUUID().toString()
        val inviteCode = InviteRepository.generateInviteCode("GRP")
        val path = "chatRooms/$roomId"
        val allMembers = (selectedMemberIds + uid).distinct()

        return try {
            val payload = mutableMapOf<String, Any>(
                "id" to roomId,
                "name" to name.trim(),
                "inviteCode" to inviteCode,
                "memberIds" to allMembers,
                "adminIds" to listOf(uid),
                "createdBy" to uid,
                "lastMessageText" to "$currentUserName created group room \"$name\"",
                "lastSenderId" to uid,
                "lastSenderName" to currentUserName,
                "isTyping" to emptyMap<String, Boolean>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "lastTimestamp" to FieldValue.serverTimestamp()
            )
            description?.let {
                if (it.isNotBlank()) payload["description"] = it.trim()
            }

            db.collection("chatRooms").document(roomId).set(payload).await()
            InviteRepository.createInviteDocument(
                db = db,
                code = inviteCode,
                targetType = "group",
                targetId = roomId,
                targetName = name.trim(),
                targetDescription = description?.trim(),
                creatorId = uid
            )
            Result.success(roomId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createGroupChat(
        name: String,
        selectedMemberIds: List<String>,
        currentUserName: String
    ): Result<String> {
        return createGroupRoom(name, null, selectedMemberIds, currentUserName)
    }

    fun observeMessages(chatId: String): Flow<List<Message>> {
        val chatsMessages = db.collection("chats").document(chatId).collection("messages")
            .snapshots()
            .map { it.toObjects(Message::class.java) }
            .catch { emit(emptyList()) }

        val roomsMessages = db.collection("chatRooms").document(chatId).collection("messages")
            .snapshots()
            .map { it.toObjects(Message::class.java) }
            .catch { emit(emptyList()) }

        return combine(chatsMessages, roomsMessages) { msg1, msg2 ->
            (if (msg1.isNotEmpty()) msg1 else msg2).sortedBy {
                it.createdAt?.toDate()?.time ?: 0L
            }
        }
    }

    suspend fun sendMessage(
        chatId: String,
        senderName: String,
        text: String,
        type: String = "text",
        attachmentUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        durationSeconds: Int? = null,
        replyTo: Message? = null
    ): Result<String> {
        val uid = requireUserId()
        val messageId = UUID.randomUUID().toString()
        val collection = resolveChatCollection(chatId)
        val path = "$collection/$chatId/messages/$messageId"

        return try {
            val chatDoc = db.collection(collection).document(chatId).get().await()
            val memberIds = (chatDoc.get("memberIds") as? List<*>)?.filterIsInstance<String>()
                ?: listOf(uid)

            val messagePayload = mutableMapOf<String, Any>(
                "id" to messageId,
                "chatId" to chatId,
                "senderId" to uid,
                "senderName" to senderName,
                "text" to text.trim(),
                "type" to type,
                "status" to "sent",
                "readBy" to mapOf(uid to true),
                "reactions" to emptyMap<String, String>(),
                "memberIds" to memberIds,
                "deleted" to false,
                "createdAt" to FieldValue.serverTimestamp()
            )

            attachmentUrl?.let { messagePayload["attachmentUrl"] = it }
            fileName?.let { messagePayload["fileName"] = it }
            fileSize?.let { messagePayload["fileSize"] = it }
            durationSeconds?.let { messagePayload["durationSeconds"] = it }
            replyTo?.let {
                messagePayload["replyToId"] = it.id
                messagePayload["replyToText"] = if (it.deleted) "Deleted message" else it.text.take(120)
                messagePayload["replyToSender"] = it.senderName
            }

            db.collection(collection).document(chatId)
                .collection("messages").document(messageId)
                .set(messagePayload).await()

            // Update parent chat snippet
            val previewText = when (type) {
                "image" -> if (text.isNotBlank()) "📷 $text" else "📷 Photo"
                "video" -> if (text.isNotBlank()) "🎬 $text" else "🎬 Video"
                "file" -> if (!fileName.isNullOrBlank()) "📎 $fileName" else if (text.isNotBlank()) "📎 $text" else "📎 Attachment"
                "audio" -> if (durationSeconds != null && durationSeconds > 0) {
                    val mins = durationSeconds / 60
                    val secs = durationSeconds % 60
                    "🎤 Voice message ($mins:${secs.toString().padStart(2, '0')})"
                } else {
                    "🎤 Voice message"
                }
                else -> text.trim()
            }

            db.collection(collection).document(chatId).update(
                mapOf(
                    "lastMessageText" to previewText,
                    "lastSenderId" to uid,
                    "lastSenderName" to senderName,
                    "lastTimestamp" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(messageId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No current user"))
        val collection = resolveChatCollection(chatId)
        val path = "$collection/$chatId/messages"
        return try {
            val unreadSnapshot = db.collection(collection).document(chatId).collection("messages")
                .whereArrayContains("memberIds", uid)
                .get().await()

            for (doc in unreadSnapshot.documents) {
                val readByMap = (doc.get("readBy") as? Map<*, *>) ?: emptyMap<Any, Any>()
                val isAlreadyRead = readByMap[uid] == true
                if (!isAlreadyRead) {
                    doc.reference.update(
                        mapOf(
                            "readBy.$uid" to true,
                            "status" to "read"
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        val uid = requireUserId()
        val collection = resolveChatCollection(chatId)
        val path = "$collection/$chatId/messages/$messageId"
        return try {
            db.collection(collection).document(chatId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "deleted" to true,
                        "text" to "This message was deleted"
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMessageReaction(chatId: String, messageId: String, emoji: String): Result<Unit> {
        val uid = requireUserId()
        val collection = resolveChatCollection(chatId)
        val path = "$collection/$chatId/messages/$messageId"
        return try {
            val docRef = db.collection(collection).document(chatId)
                .collection("messages").document(messageId)
            val snapshot = docRef.get().await()
            val reactionsMap = (snapshot.get("reactions") as? Map<*, *>) ?: emptyMap<Any, Any>()
            val existingEmoji = reactionsMap[uid] as? String

            if (existingEmoji == emoji) {
                // Tapping the same emoji removes it
                docRef.update("reactions.$uid", FieldValue.delete()).await()
            } else {
                // Setting or replacing with new emoji
                docRef.update("reactions.$uid", emoji).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteChat(chatId: String): Result<Unit> {
        val collection = resolveChatCollection(chatId)
        val path = "$collection/$chatId"
        return try {
            val messagesSnapshot = db.collection(collection).document(chatId).collection("messages").get().await()
            for (doc in messagesSnapshot.documents) {
                try {
                    doc.reference.delete().await()
                } catch (ignored: Exception) {}
            }
            db.collection(collection).document(chatId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun clearChat(chatId: String): Result<Unit> {
        val uid = requireUserId()
        val collection = resolveChatCollection(chatId)
        val path = "$collection/$chatId"
        return try {
            val messagesSnapshot = db.collection(collection).document(chatId).collection("messages").get().await()
            for (doc in messagesSnapshot.documents) {
                try {
                    doc.reference.delete().await()
                } catch (ignored: Exception) {}
            }
            db.collection(collection).document(chatId).update(
                mapOf(
                    "lastMessageText" to "Chat cleared",
                    "lastSenderId" to uid,
                    "lastSenderName" to (auth.currentUser?.displayName ?: "User"),
                    "lastTimestamp" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setGroupAdminRole(
        chatId: String,
        targetUserId: String,
        makeAdmin: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val collection = resolveChatCollection(chatId)
        val path = "$collection/$chatId"
        return try {
            val docRef = db.collection(collection).document(chatId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(IllegalArgumentException("Group room not found."))
            }
            val memberIds = (snapshot.get("memberIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val adminIds = (snapshot.get("adminIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val createdBy = snapshot.getString("createdBy").orEmpty()

            val callerIsAdmin = uid == createdBy || adminIds.contains(uid)
            if (!callerIsAdmin) {
                return Result.failure(IllegalStateException("Only group admins can manage admin roles."))
            }
            if (!memberIds.contains(targetUserId)) {
                return Result.failure(IllegalArgumentException("User is not a member of this group room."))
            }
            if (!makeAdmin) {
                if (createdBy.isNotBlank() && targetUserId == createdBy) {
                    return Result.failure(IllegalStateException("Group creator cannot be removed as admin."))
                }
                if (adminIds.size <= 1 && adminIds.contains(targetUserId)) {
                    return Result.failure(IllegalStateException("Group must have at least one admin."))
                }
            }

            val updatedAdmins = if (makeAdmin) {
                (adminIds + targetUserId + createdBy).filter { it.isNotBlank() }.distinct()
            } else {
                adminIds.filterNot { it == targetUserId }
            }

            docRef.update(
                mapOf(
                    "adminIds" to updatedAdmins,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
