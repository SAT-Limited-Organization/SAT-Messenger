package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val avatarUrl: String? = null,
    val statusMessage: String? = "Available",
    val isOnline: Boolean = false,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val username: String
        get() = when {
            displayName.isNotBlank() -> displayName
            email.isNotBlank() -> email.substringBefore("@")
            userId.isNotBlank() -> "user_${userId.take(6)}"
            else -> "User"
        }

    val usernameHandle: String
        get() = when {
            email.isNotBlank() -> "@${email.substringBefore("@")}"
            userId.isNotBlank() -> "@user_${userId.take(6)}"
            else -> "@user"
        }
}

@IgnoreExtraProperties
data class PrivacySettings(
    val userId: String = "",
    val readReceipts: Boolean = true,
    val lastSeenVisible: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockedUser(
    val blockedUserId: String = "",
    val blockedUserName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Chat(
    val id: String = "",
    val type: String = "direct", // "direct" or "group"
    val name: String? = null,
    val description: String? = null,
    val inviteCode: String? = null,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val lastMessageText: String? = null,
    val lastSenderId: String? = null,
    val lastSenderName: String? = null,
    val isTyping: Map<String, Boolean> = emptyMap(),
    val participantNames: Map<String, String> = emptyMap(),
    @ServerTimestamp val lastTimestamp: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun getDisplayNameOrUsername(
        currentUserId: String,
        directoryUsers: List<UserProfile>
    ): String {
        if (type == "group") {
            return name?.takeIf { it.isNotBlank() } ?: "Group Room"
        }
        val otherMemberId = memberIds.firstOrNull { it != currentUserId }
        val otherUser = directoryUsers.firstOrNull { it.userId == otherMemberId }
        return when {
            otherUser != null && otherUser.displayName.isNotBlank() -> otherUser.displayName
            otherUser != null && otherUser.email.isNotBlank() -> otherUser.email.substringBefore("@")
            !otherMemberId.isNullOrBlank() && !participantNames[otherMemberId].isNullOrBlank() -> participantNames[otherMemberId]!!
            lastSenderId == otherMemberId && !lastSenderName.isNullOrBlank() -> lastSenderName
            !otherMemberId.isNullOrBlank() -> "User_${otherMemberId.take(6)}"
            else -> "User"
        }
    }

    fun getUsernameHandle(
        currentUserId: String,
        directoryUsers: List<UserProfile>
    ): String? {
        if (type == "group") return null
        val otherMemberId = memberIds.firstOrNull { it != currentUserId }
        val otherUser = directoryUsers.firstOrNull { it.userId == otherMemberId }
        return when {
            otherUser != null && otherUser.email.isNotBlank() -> "@${otherUser.email.substringBefore("@")}"
            !otherMemberId.isNullOrBlank() -> "@user_${otherMemberId.take(6)}"
            else -> null
        }
    }
}

@IgnoreExtraProperties
data class ChatRoom(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val inviteCode: String? = null,
    val memberIds: List<String> = emptyList(),
    val adminIds: List<String> = emptyList(),
    val createdBy: String = "",
    val lastMessageText: String? = null,
    val lastSenderId: String? = null,
    val lastSenderName: String? = null,
    val isTyping: Map<String, Boolean> = emptyMap(),
    @ServerTimestamp val lastTimestamp: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toChat(): Chat = Chat(
        id = id,
        type = "group",
        name = name,
        description = description,
        inviteCode = inviteCode,
        memberIds = memberIds,
        adminIds = adminIds,
        lastMessageText = lastMessageText,
        lastSenderId = lastSenderId,
        lastSenderName = lastSenderName,
        isTyping = isTyping,
        participantNames = emptyMap(),
        lastTimestamp = lastTimestamp,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@IgnoreExtraProperties
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val type: String = "text", // "text", "image", "file", "audio"
    val attachmentUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val durationSeconds: Int? = null,
    val replyToId: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val status: String = "sent", // "sent", "delivered", "read"
    val readBy: Map<String, Boolean> = emptyMap(),
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val memberIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    val deleted: Boolean = false
) {
    fun getReactionsGrouped(): Map<String, List<String>> {
        if (reactions.isEmpty()) return emptyMap()
        val grouped = mutableMapOf<String, MutableList<String>>()
        reactions.forEach { (uid, emoji) ->
            if (emoji.isNotBlank()) {
                grouped.getOrPut(emoji) { mutableListOf() }.add(uid)
            }
        }
        return grouped
    }

    fun hasUserReacted(userId: String, emoji: String): Boolean {
        return reactions[userId] == emoji
    }

    fun getUserReaction(userId: String): String? {
        return reactions[userId]
    }
}

@IgnoreExtraProperties
data class StatusItem(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val text: String? = null,
    val mediaUrl: String? = null,
    val backgroundColor: String? = "#0A2540",
    val viewerIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val expiresAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Channel(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val iconUrl: String? = null,
    val category: String = "General",
    @get:PropertyName("isPublic")
    @field:PropertyName("isPublic")
    val isPublic: Boolean = true,
    val allowFollowerPosts: Boolean = false,
    val allowReactions: Boolean = true,
    val inviteCode: String? = null,
    val creatorId: String = "",
    val adminIds: List<String> = emptyList(),
    val subscriberIds: List<String> = emptyList(),
    val followerCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChannelPost(
    val id: String = "",
    val channelId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val mediaUrl: String? = null,
    val reactions: Map<String, Int> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Community(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val iconUrl: String? = null,
    val inviteCode: String? = null,
    val creatorId: String = "",
    val adminIds: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val subgroupChatIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InviteLink(
    val code: String = "",
    val targetType: String = "", // "channel", "group", "community"
    val targetId: String = "",
    val targetName: String = "",
    val targetDescription: String? = null,
    val creatorId: String = "",
    val active: Boolean = true,
    val usesCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val shareUrl: String
        get() = "https://satmessenger.app/invite/$code"
}

@IgnoreExtraProperties
data class Report(
    val id: String = "",
    val reporterId: String = "",
    val reportedUserId: String = "",
    val reason: String = "",
    val details: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)
