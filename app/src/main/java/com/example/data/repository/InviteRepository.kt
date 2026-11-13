package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Channel
import com.example.data.model.ChatRoom
import com.example.data.model.Community
import com.example.data.model.InviteLink
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

class InviteRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to manage or join invite links.")
    }

    suspend fun getOrCreateInviteLink(
        targetType: String,
        targetId: String,
        targetName: String,
        targetDescription: String? = null,
        existingCode: String? = null
    ): Result<InviteLink> {
        val uid = requireUserId()
        return try {
            val cleanExisting = existingCode?.trim()?.takeIf { it.isNotBlank() }
            if (cleanExisting != null) {
                val doc = db.collection("invites").document(cleanExisting).get().await()
                val existing = doc.toObject(InviteLink::class.java)
                if (existing != null && existing.active) {
                    return Result.success(existing)
                }
            }

            val prefix = when (targetType) {
                "channel" -> "CHN"
                "group" -> "GRP"
                "community" -> "COM"
                else -> "SAT"
            }
            val code = generateInviteCode(prefix)
            val payload = mutableMapOf<String, Any>(
                "code" to code,
                "targetType" to targetType,
                "targetId" to targetId,
                "targetName" to targetName.take(120).ifBlank { "SAT Space" },
                "creatorId" to uid,
                "active" to true,
                "usesCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            targetDescription?.takeIf { it.isNotBlank() }?.let {
                payload["targetDescription"] = it.take(500)
            }

            db.collection("invites").document(code).set(payload).await()
            updateTargetInviteCode(targetType, targetId, code)

            val created = InviteLink(
                code = code,
                targetType = targetType,
                targetId = targetId,
                targetName = targetName,
                targetDescription = targetDescription,
                creatorId = uid,
                active = true,
                usesCount = 0
            )
            Result.success(created)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "invites")
            Result.failure(e)
        }
    }

    suspend fun regenerateInviteLink(
        targetType: String,
        targetId: String,
        targetName: String,
        targetDescription: String? = null,
        oldCode: String? = null
    ): Result<InviteLink> {
        val uid = requireUserId()
        return try {
            val cleanOld = oldCode?.trim()?.takeIf { it.isNotBlank() }
            if (cleanOld != null) {
                try {
                    db.collection("invites").document(cleanOld).delete().await()
                } catch (_: Exception) {
                    // Best-effort cleanup of prior invite link if owned by user
                }
            }

            val prefix = when (targetType) {
                "channel" -> "CHN"
                "group" -> "GRP"
                "community" -> "COM"
                else -> "SAT"
            }
            val code = generateInviteCode(prefix)
            val payload = mutableMapOf<String, Any>(
                "code" to code,
                "targetType" to targetType,
                "targetId" to targetId,
                "targetName" to targetName.take(120).ifBlank { "SAT Space" },
                "creatorId" to uid,
                "active" to true,
                "usesCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            targetDescription?.takeIf { it.isNotBlank() }?.let {
                payload["targetDescription"] = it.take(500)
            }

            db.collection("invites").document(code).set(payload).await()
            updateTargetInviteCode(targetType, targetId, code)

            Result.success(
                InviteLink(
                    code = code,
                    targetType = targetType,
                    targetId = targetId,
                    targetName = targetName,
                    targetDescription = targetDescription,
                    creatorId = uid,
                    active = true,
                    usesCount = 0
                )
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "invites")
            Result.failure(e)
        }
    }

    suspend fun resolveInviteLink(rawInput: String): Result<InviteLink> {
        requireUserId()
        val code = extractInviteCode(rawInput)
        if (code.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter a valid invite code or link."))
        }
        val path = "invites/$code"
        return try {
            val snapshot = db.collection("invites").document(code).get().await()
            val invite = snapshot.toObject(InviteLink::class.java)
            if (invite == null || !invite.active) {
                Result.failure(IllegalStateException("This invite link is invalid, expired, or has been reset."))
            } else {
                Result.success(invite)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun redeemInviteLink(invite: InviteLink): Result<InviteLink> {
        val uid = requireUserId()
        return try {
            when (invite.targetType) {
                "channel" -> {
                    val docRef = db.collection("channels").document(invite.targetId)
                    val channel = docRef.get().await().toObject(Channel::class.java)
                        ?: return Result.failure(IllegalStateException("Channel no longer exists."))
                    if (!channel.subscriberIds.contains(uid)) {
                        docRef.update(
                            mapOf(
                                "subscriberIds" to FieldValue.arrayUnion(uid),
                                "followerCount" to channel.followerCount + 1
                            )
                        ).await()
                    }
                }
                "group" -> {
                    val docRef = db.collection("chatRooms").document(invite.targetId)
                    val roomSnapshot = try {
                        docRef.get().await()
                    } catch (_: Exception) {
                        null
                    }
                    val existingMembers = (roomSnapshot?.get("memberIds") as? List<*>)
                        ?.filterIsInstance<String>()
                        ?: emptyList()
                    if (!existingMembers.contains(uid)) {
                        docRef.update(
                            mapOf(
                                "memberIds" to FieldValue.arrayUnion(uid),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
                "community" -> {
                    val docRef = db.collection("communities").document(invite.targetId)
                    val community = docRef.get().await().toObject(Community::class.java)
                        ?: return Result.failure(IllegalStateException("Community no longer exists."))
                    if (!community.memberIds.contains(uid)) {
                        docRef.update("memberIds", FieldValue.arrayUnion(uid)).await()
                    }
                    // Also attempt to join linked subgroup rooms that have active invite codes
                    for (subId in community.subgroupChatIds) {
                        try {
                            val alreadyMember = try {
                                val subDoc = db.collection("chatRooms").document(subId).get().await()
                                val room = subDoc.toObject(ChatRoom::class.java)
                                room?.memberIds?.contains(uid) == true
                            } catch (_: Exception) {
                                // PERMISSION_DENIED on get() means current user is not yet in memberIds
                                false
                            }
                            if (!alreadyMember) {
                                db.collection("chatRooms").document(subId).update(
                                    mapOf(
                                        "memberIds" to FieldValue.arrayUnion(uid),
                                        "updatedAt" to FieldValue.serverTimestamp()
                                    )
                                ).await()
                            }
                        } catch (_: Exception) {
                            // Ignore individual subgroup if no active invite or room deleted
                        }
                    }
                }
                else -> {
                    return Result.failure(IllegalArgumentException("Unsupported invite type: ${invite.targetType}"))
                }
            }

            try {
                db.collection("invites").document(invite.code)
                    .update("usesCount", FieldValue.increment(1))
                    .await()
            } catch (_: Exception) {
                // Non-fatal if analytics counter increment is skipped
            }

            Result.success(invite)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "${invite.targetType}/${invite.targetId}")
            Result.failure(e)
        }
    }

    private suspend fun updateTargetInviteCode(targetType: String, targetId: String, code: String) {
        val collection = when (targetType) {
            "channel" -> "channels"
            "group" -> "chatRooms"
            "community" -> "communities"
            else -> return
        }
        try {
            db.collection(collection).document(targetId)
                .update("inviteCode", code)
                .await()
        } catch (_: Exception) {
            // Non-fatal if target doc was already updated or deleted
        }
    }

    companion object {
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        fun generateInviteCode(prefix: String): String {
            val suffix = (1..6).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
            return "$prefix-$suffix"
        }

        fun extractInviteCode(rawInput: String): String {
            val trimmed = rawInput.trim()
            if (trimmed.isBlank()) return ""
            val withoutQuery = trimmed.substringBefore("?").substringBefore("#").trimEnd('/')
            val lastSegment = withoutQuery.substringAfterLast("/").trim()
            return lastSegment.uppercase()
        }

        suspend fun createInviteDocument(
            db: FirebaseFirestore,
            code: String,
            targetType: String,
            targetId: String,
            targetName: String,
            targetDescription: String?,
            creatorId: String
        ) {
            val payload = mutableMapOf<String, Any>(
                "code" to code,
                "targetType" to targetType,
                "targetId" to targetId,
                "targetName" to targetName.take(120).ifBlank { "SAT Space" },
                "creatorId" to creatorId,
                "active" to true,
                "usesCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
            targetDescription?.takeIf { it.isNotBlank() }?.let {
                payload["targetDescription"] = it.take(500)
            }
            db.collection("invites").document(code).set(payload).await()
        }
    }
}
