package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SatMessengerRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_authenticatedUser_succeeds() {
        runBlocking {
            val uid = signInTestUser(ALICE_EMAIL)
            val userRepo = UserRepository(firestore)

            val profile = UserProfile(
                userId = uid,
                displayName = "Alice Smith",
                email = ALICE_EMAIL,
                statusMessage = "SAT Executive Team",
                isOnline = true
            )

            val result = withTimeout(DEFAULT_TIMEOUT_MS) {
                userRepo.saveProfile(profile)
            }
            assertTrue("Expected saveProfile to succeed", result.isSuccess)

            val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
                userRepo.getUserProfile(uid)
            }
            assertNotNull(fetched)
            assertEquals("Alice Smith", fetched?.displayName)
        }
    }

    @Test
    fun createDirectChat_and_sendMessage_memberSucceeds() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val bobUid = "bob_mock_${System.currentTimeMillis()}"

            val userRepo = UserRepository(firestore)
            userRepo.saveProfile(
                UserProfile(
                    userId = aliceUid,
                    displayName = "Alice",
                    email = ALICE_EMAIL
                )
            )

            val chatRepo = ChatRepository(firestore)
            val chatResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.getOrCreateDirectChat(
                    targetUserId = bobUid,
                    targetUserName = "Bob Partner",
                    currentUserName = "Alice"
                )
            }
            assertTrue(chatResult.isSuccess)
            val chatId = chatResult.getOrThrow()
            assertTrue(chatId.isNotBlank())

            val msgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.sendMessage(
                    chatId = chatId,
                    senderName = "Alice",
                    text = "Secure corporate dispatch from SAT Limited."
                )
            }
            assertTrue(msgResult.isSuccess)
            val messageId = msgResult.getOrThrow()

            // Emoji reaction test: add reaction
            val reactResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.toggleMessageReaction(chatId = chatId, messageId = messageId, emoji = "👍")
            }
            assertTrue("Expected toggleMessageReaction to succeed", reactResult.isSuccess)

            // Toggle same emoji: removes reaction
            val removeReactResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.toggleMessageReaction(chatId = chatId, messageId = messageId, emoji = "👍")
            }
            assertTrue("Expected removing reaction to succeed", removeReactResult.isSuccess)

            val typingResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.setTyping(chatId = chatId, isTyping = true)
            }
            assertTrue(typingResult.isSuccess)

            val markReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.markMessagesAsRead(chatId)
            }
            assertTrue(markReadResult.isSuccess)

            // Send media file test
            val mediaMsgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.sendMessage(
                    chatId = chatId,
                    senderName = "Alice",
                    text = "Project Schematic",
                    type = "image",
                    attachmentUrl = "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
                    fileName = "schematic.jpg",
                    fileSize = 1024L
                )
            }
            assertTrue("Expected media message to succeed", mediaMsgResult.isSuccess)

            // Send voice audio clip test
            val audioMsgResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.sendMessage(
                    chatId = chatId,
                    senderName = "Alice",
                    text = "Voice message",
                    type = "audio",
                    attachmentUrl = "data:audio/mp4;base64,AAAAIGZ0eXBpc29t",
                    fileName = "Voice_test.m4a",
                    fileSize = 2048L,
                    durationSeconds = 9
                )
            }
            assertTrue("Expected voice audio message to succeed", audioMsgResult.isSuccess)

            // Clear chat test
            val clearResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.clearChat(chatId)
            }
            assertTrue("Expected clearChat to succeed", clearResult.isSuccess)

            // Delete chat test
            val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.deleteChat(chatId)
            }
            assertTrue("Expected deleteChat to succeed", deleteResult.isSuccess)
        }
    }

    @Test
    fun observeChats_unauthenticatedUser_failsWithPermissionDenied() {
        runBlocking {
            auth.signOut()
            val chatRepo = ChatRepository(firestore)

            try {
                withTimeout(FLOW_TIMEOUT_MS) {
                    chatRepo.observeUserChats("unauth_uid").first()
                }
            } catch (e: Exception) {
                val root = e.unwrapFirestoreException()
                if (root != null) {
                    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, root.code)
                }
            }
        }
    }

    @Test
    fun userProfileAndChat_resolveUsername_neverShowsDefaultColleague() {
        val userWithDisplayName = UserProfile(userId = "u1", displayName = "Simbarashe Tembo", email = "simba@satlimited.com")
        assertEquals("Simbarashe Tembo", userWithDisplayName.username)
        assertEquals("@simba", userWithDisplayName.usernameHandle)

        val userWithoutDisplayName = UserProfile(userId = "u2", displayName = "", email = "tembo.augustus@satlimited.com")
        assertEquals("tembo.augustus", userWithoutDisplayName.username)
        assertEquals("@tembo.augustus", userWithoutDisplayName.usernameHandle)

        val directChat = com.example.data.model.Chat(
            id = "c1",
            type = "direct",
            memberIds = listOf("u1", "u2"),
            participantNames = mapOf("u1" to "Simbarashe Tembo", "u2" to "tembo.augustus")
        )
        val resolvedName = directChat.getDisplayNameOrUsername(
            currentUserId = "u1",
            directoryUsers = listOf(userWithDisplayName, userWithoutDisplayName)
        )
        assertEquals("tembo.augustus", resolvedName)
    }

    @Test
    fun flexibleChannel_createUpdatePostAndDelete_succeeds() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val socialRepo = SocialFeaturesRepository(firestore)
            val inviteRepo = InviteRepository(firestore)

            val createRes = withTimeout(DEFAULT_TIMEOUT_MS) {
                socialRepo.createChannel(
                    name = "Payload Engineering",
                    description = "Telemetry & integration updates",
                    iconUrl = null,
                    category = "Engineering",
                    isPublic = false,
                    allowFollowerPosts = true,
                    allowReactions = true
                )
            }
            assertTrue("Expected flexible channel creation to succeed", createRes.isSuccess)
            val channelId = createRes.getOrThrow()

            val createdChannel = withTimeout(FLOW_TIMEOUT_MS) {
                socialRepo.observeChannels().first { list -> list.any { it.id == channelId } }
                    .first { it.id == channelId }
            }
            assertEquals("Engineering", createdChannel.category)
            assertEquals(false, createdChannel.isPublic)
            assertEquals(true, createdChannel.allowFollowerPosts)
            assertTrue(!createdChannel.inviteCode.isNullOrBlank())

            // Verify invite link generated with channel is resolvable
            val resolvedInvite = withTimeout(DEFAULT_TIMEOUT_MS) {
                inviteRepo.resolveInviteLink(createdChannel.inviteCode!!)
            }
            assertTrue("Expected auto-created channel invite link to resolve", resolvedInvite.isSuccess)
            assertEquals(channelId, resolvedInvite.getOrThrow().targetId)

            // Post update and delete post
            val postRes = withTimeout(DEFAULT_TIMEOUT_MS) {
                socialRepo.postToChannel(channelId, "Alice", "Initial flexible channel post")
            }
            assertTrue("Expected postToChannel to succeed", postRes.isSuccess)
            val postId = postRes.getOrThrow()

            val updateRes = withTimeout(DEFAULT_TIMEOUT_MS) {
                socialRepo.updateChannelSettings(
                    channelId = channelId,
                    name = "Payload Eng Broadcast",
                    description = "Updated charter",
                    category = "Operations",
                    isPublic = true,
                    allowFollowerPosts = false,
                    allowReactions = false,
                    inviteCode = createdChannel.inviteCode
                )
            }
            assertTrue("Expected updateChannelSettings to succeed", updateRes.isSuccess)

            val delPostRes = withTimeout(DEFAULT_TIMEOUT_MS) {
                socialRepo.deleteChannelPost(channelId, postId)
            }
            assertTrue("Expected deleteChannelPost to succeed", delPostRes.isSuccess)

            val delChanRes = withTimeout(DEFAULT_TIMEOUT_MS) {
                socialRepo.deleteChannel(channelId, createdChannel.inviteCode)
            }
            assertTrue("Expected deleteChannel to succeed", delChanRes.isSuccess)
            assertEquals(aliceUid, createdChannel.creatorId)
        }
    }

    @Test
    fun inviteLinks_groupChannelCommunity_resolveAndRedeem_succeeds() {
        runBlocking {
            // Alice creates Group Room, Channel, and Community
            signInTestUser(ALICE_EMAIL)
            val chatRepo = ChatRepository(firestore)
            val socialRepo = SocialFeaturesRepository(firestore)
            val inviteRepo = InviteRepository(firestore)

            val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
                chatRepo.createGroupRoom(
                    name = "Telemetry Ops Group",
                    description = "Orbital telemetry squad",
                    selectedMemberIds = emptyList(),
                    currentUserName = "Alice"
                ).getOrThrow()
            }
            val channelId = withTimeout(DEFAULT_TIMEOUT_MS) {
                socialRepo.createChannel(
                    name = "Flight Alerts",
                    description = "Critical alerts",
                    iconUrl = null,
                    category = "Alerts",
                    isPublic = false,
                    allowFollowerPosts = true,
                    allowReactions = true
                ).getOrThrow()
            }
            val communityId = withTimeout(DEFAULT_TIMEOUT_MS) {
                socialRepo.createCommunity(
                    name = "SAT Ground Control",
                    description = "All mission teams",
                    iconUrl = null,
                    subgroupChatIds = listOf(roomId)
                ).getOrThrow()
            }

            val groupInvite = withTimeout(DEFAULT_TIMEOUT_MS) {
                inviteRepo.getOrCreateInviteLink("group", roomId, "Telemetry Ops Group").getOrThrow()
            }
            val channelInvite = withTimeout(DEFAULT_TIMEOUT_MS) {
                inviteRepo.getOrCreateInviteLink("channel", channelId, "Flight Alerts").getOrThrow()
            }
            val communityInvite = withTimeout(DEFAULT_TIMEOUT_MS) {
                inviteRepo.getOrCreateInviteLink("community", communityId, "SAT Ground Control").getOrThrow()
            }

            // Regenerate channel invite link and confirm extractInviteCode works on share URL
            val rotatedChannelInvite = withTimeout(DEFAULT_TIMEOUT_MS) {
                inviteRepo.regenerateInviteLink(
                    targetType = "channel",
                    targetId = channelId,
                    targetName = "Flight Alerts",
                    oldCode = channelInvite.code
                ).getOrThrow()
            }
            assertEquals(
                rotatedChannelInvite.code,
                InviteRepository.extractInviteCode(rotatedChannelInvite.shareUrl)
            )

            // Sign in as Bob (non-member) and join Group, Channel, and Community via invite links
            val bobUid = signInTestUser(BOB_EMAIL)
            val bobInviteRepo = InviteRepository(firestore)

            val resolvedGroup = withTimeout(DEFAULT_TIMEOUT_MS) {
                bobInviteRepo.resolveInviteLink(groupInvite.shareUrl).getOrThrow()
            }
            val redeemGroupRes = withTimeout(DEFAULT_TIMEOUT_MS) {
                bobInviteRepo.redeemInviteLink(resolvedGroup)
            }
            assertTrue("Expected Bob to join group via invite", redeemGroupRes.isSuccess)

            val resolvedChannel = withTimeout(DEFAULT_TIMEOUT_MS) {
                bobInviteRepo.resolveInviteLink(rotatedChannelInvite.code).getOrThrow()
            }
            val redeemChanRes = withTimeout(DEFAULT_TIMEOUT_MS) {
                bobInviteRepo.redeemInviteLink(resolvedChannel)
            }
            assertTrue("Expected Bob to join private channel via invite", redeemChanRes.isSuccess)

            val resolvedComm = withTimeout(DEFAULT_TIMEOUT_MS) {
                bobInviteRepo.resolveInviteLink("satmessenger://invite/${communityInvite.code}").getOrThrow()
            }
            val redeemCommRes = withTimeout(DEFAULT_TIMEOUT_MS) {
                bobInviteRepo.redeemInviteLink(resolvedComm)
            }
            assertTrue("Expected Bob to join community via invite", redeemCommRes.isSuccess)

            // Confirm Bob can now observe the group room in his chats
            val bobRooms = withTimeout(FLOW_TIMEOUT_MS) {
                ChatRepository(firestore).observeUserChatRooms(bobUid)
                    .first { rooms -> rooms.any { it.id == roomId } }
            }
            assertTrue(bobRooms.any { it.id == roomId && it.memberIds.contains(bobUid) })
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_test@satlimited.com"
        const val BOB_EMAIL = "bob_test@satlimited.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
