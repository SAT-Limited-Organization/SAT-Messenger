package com.example.ui.screens.contacts

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AvailabilityStatus
import com.example.data.model.FriendRequest
import com.example.data.model.PrivacySettings
import com.example.data.model.StatusItem
import com.example.data.model.UserProfile
import com.example.data.repository.DeviceContactMatch
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.search.UserSearchDialog
import com.example.ui.screens.status.StatusScreen
import com.example.ui.theme.SATMessengerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h1200dp")
class FriendsAndStatusUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:815554441605:android:test")
                    .setApiKey("AIzaSyTestKeyForUnitTestsOnly")
                    .setProjectId("sat-limited")
                    .build()
            )
        }
    }

    @Test
    fun contactsScreen_showsDeviceMatchesAndFriendRequestAcceptDecline() {
        var acceptedId: String? = null
        val me = UserProfile(userId = "me_1", displayName = "Simba", email = "simba@gmail.com")
        val alice = UserProfile(
            userId = "u_alice",
            displayName = "Alice Moyo",
            email = "alice.moyo@gmail.com",
            primaryUsername = "alice.moyo",
            secondaryUsername = "sat_alice",
            phoneNumber = "+15551234567",
            isOnline = true,
            availabilityStatus = AvailabilityStatus.AVAILABLE.label
        )
        val req = FriendRequest(
            id = "me_1_u_alice",
            fromUserId = "u_alice",
            fromUserName = "Alice Moyo",
            toUserId = "me_1",
            toUserName = "Simba",
            participantIds = listOf("me_1", "u_alice"),
            status = "pending"
        )
        val deviceMatch = DeviceContactMatch(
            user = alice,
            deviceContactName = "Alice Work Cell",
            matchedVia = "Phone"
        )

        composeRule.setContent {
            SATMessengerTheme {
                ContactsScreen(
                    currentUserId = me.userId,
                    currentUserProfile = me,
                    directoryUsers = listOf(alice),
                    savedContacts = emptyList(),
                    onDirectMessageUser = {},
                    onUpdateAvailability = {},
                    onToggleFriend = {},
                    friendRequests = listOf(req),
                    deviceContactMatches = mapOf("u_alice" to deviceMatch),
                    hasDeviceContactsPermission = true,
                    deviceContactsScannedCount = 12,
                    onRespondToFriendRequest = { r, accept ->
                        if (accept) acceptedId = r.id
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("device_contacts_sync_card").assertIsDisplayed()
        composeRule.onNodeWithTag("incoming_friend_requests_card").assertIsDisplayed()
        composeRule.onNodeWithTag("contact_filter_on_device").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("contact_item_u_alice").assertIsDisplayed()

        // Search by secondary username
        composeRule.onNodeWithTag("contacts_search_input").performTextInput("@sat_alice")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("contact_item_u_alice").assertIsDisplayed()

        // Accept friend request
        composeRule.onNodeWithTag("accept_friend_request_me_1_u_alice").performClick()
        composeRule.waitForIdle()
        assertEquals("me_1_u_alice", acceptedId)
    }

    @Test
    fun userSearchDialog_findsBySecondaryUsernameAndSendsFriendRequest() {
        var addedFriendId: String? = null
        val user = UserProfile(
            userId = "u_tariro",
            displayName = "Tariro Ndlovu",
            email = "tariro.ops@gmail.com",
            primaryUsername = "tariro.ops",
            secondaryUsername = "t_commander",
            phoneNumber = "+263771112233",
            isOnline = true
        )

        composeRule.setContent {
            SATMessengerTheme {
                UserSearchDialog(
                    users = listOf(user),
                    currentUserId = "me_1",
                    onDismiss = {},
                    onSelectUser = {},
                    onAddFriend = { addedFriendId = it.userId }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("user_search_input_field").performTextInput("@t_commander")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("user_search_result_u_tariro").assertIsDisplayed()
        composeRule.onNodeWithText("Matched: Secondary Username").assertIsDisplayed()

        composeRule.onNodeWithTag("user_search_add_friend_u_tariro").performClick()
        composeRule.waitForIdle()
        assertEquals("u_tariro", addedFriendId)
    }

    @Test
    fun profileScreen_allowsClaimingSecondaryUsername() {
        var submittedUsername: String? = null
        val profile = UserProfile(
            userId = "me_1",
            displayName = "Simba",
            email = "simba.sat@gmail.com",
            primaryUsername = "simba.sat"
        )

        composeRule.setContent {
            SATMessengerTheme {
                ProfileScreen(
                    userProfile = profile,
                    privacySettings = PrivacySettings(userId = "me_1"),
                    blockedUsers = emptyList(),
                    onBack = {},
                    onUpdateProfile = { _, _, _ -> },
                    onUpdateSecondaryUsername = { submittedUsername = it },
                    onUpdatePrivacy = { _, _ -> },
                    onUnblockUser = {},
                    onSignOut = {},
                    onDeleteAccount = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_primary_username_text").assertIsDisplayed()
        composeRule.onNodeWithTag("secondary_username_input").performTextInput("simba_cmd")
        composeRule.onNodeWithTag("save_secondary_username_button").performClick()
        composeRule.waitForIdle()
        assertEquals("simba_cmd", submittedUsername)
    }

    @Test
    fun statusScreen_opensStoryViewerAndSendsReply() {
        var repliedPayload: Pair<String, String>? = null
        var markedViewed = false
        val peerStatus = StatusItem(
            id = "st_1",
            userId = "u_alice",
            userName = "Alice",
            text = "Payload telemetry nominal"
        )

        composeRule.setContent {
            SATMessengerTheme {
                StatusScreen(
                    currentUserId = "me_1",
                    currentUserProfile = UserProfile(userId = "me_1", displayName = "Simba"),
                    statuses = listOf(peerStatus),
                    onPostStatus = { _, _, _ -> },
                    onDeleteStatus = {},
                    onMarkStatusViewed = { markedViewed = true },
                    onReplyToStatus = { authorId, reply -> repliedPayload = authorId to reply }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("status_item_st_1").performClick()
        composeRule.waitForIdle()
        assertTrue(markedViewed)

        composeRule.onNodeWithTag("status_reply_input").performTextInput("Awesome update!")
        composeRule.onNodeWithTag("status_reply_send_button").performClick()
        composeRule.waitForIdle()
        assertEquals("u_alice" to "Awesome update!", repliedPayload)
    }
}
