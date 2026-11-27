package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.BuildConfig
import com.example.data.model.Channel
import com.example.data.model.Chat
import com.example.data.model.Community
import com.example.data.model.Message
import com.example.data.model.PrivacySettings
import com.example.data.model.UserProfile
import com.example.data.repository.GroundingSource
import com.example.data.repository.SatAiMessage
import com.example.data.repository.SatAiMode
import com.example.data.repository.SatAiUiState
import com.example.ui.screens.ai.SatLimitedAiScreen
import com.example.ui.screens.channels.ChannelFeedDialog
import com.example.ui.screens.chat.ChatDetailsScreen
import com.example.ui.screens.communities.CommunitiesScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.status.StatusScreen
import com.example.ui.theme.SATMessengerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h1200dp")
class FeaturesUiAndAiTest {

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
    fun satLimitedAiScreen_rendersModesGroundingErrorAndSendsPrompt() {
        var sentPrompt: String? = null
        var selectedMode: SatAiMode? = null
        var toggledGrounding: Boolean? = null

        val messages = listOf(
            SatAiMessage(
                id = "m1",
                role = "model",
                text = "Here is the SAT telemetry summary.",
                modelLabel = "Smart",
                webSearchQueries = listOf("SAT Limited orbital status"),
                groundingSources = listOf(
                    GroundingSource(title = "SAT Operations Portal", uri = "https://satlimited.example.com")
                )
            )
        )

        composeRule.setContent {
            SATMessengerTheme {
                SatLimitedAiScreen(
                    messages = messages,
                    uiState = SatAiUiState.Error("Temporary network issue"),
                    selectedMode = SatAiMode.SMART,
                    webGroundingEnabled = true,
                    onBack = {},
                    onSendPrompt = { prompt, _, _ -> sentPrompt = prompt },
                    onSelectMode = { selectedMode = it },
                    onToggleWebGrounding = { toggledGrounding = it },
                    onClearConversation = {},
                    onDismissError = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("SAT Limited AI").assertIsDisplayed()
        composeRule.onNodeWithTag("sat_ai_mode_fast").assertIsDisplayed().performClick()
        assertEquals(SatAiMode.FAST, selectedMode)

        composeRule.onNodeWithTag("sat_ai_grounding_toggle").assertIsDisplayed().performClick()
        assertEquals(false, toggledGrounding)

        composeRule.onNodeWithTag("sat_ai_error_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("sat_ai_source_m1_0").assertIsDisplayed()

        composeRule.onNodeWithTag("sat_ai_input").performTextInput("Summarize today's orbital plan")
        composeRule.onNodeWithTag("sat_ai_send_button").performClick()
        composeRule.waitForIdle()

        assertEquals("Summarize today's orbital plan", sentPrompt)
    }

    @Test
    fun chatDetailsScreen_creatorSeesPromoteAndDismissControls_nonAdminDoesNot() {
        val alice = UserProfile(userId = "alice", displayName = "Alice Creator", email = "alice@satlimited.com")
        val bob = UserProfile(userId = "bob", displayName = "Bob Member", email = "bob@satlimited.com")
        val charlie = UserProfile(userId = "charlie", displayName = "Charlie Admin", email = "charlie@satlimited.com")
        val chat = Chat(
            id = "room_1",
            type = "group",
            name = "Launch Ops",
            memberIds = listOf("alice", "bob", "charlie"),
            adminIds = listOf("alice", "charlie"),
            createdBy = "alice"
        )
        val recordedCalls = mutableListOf<Pair<String, Boolean>>()

        composeRule.setContent {
            SATMessengerTheme {
                ChatDetailsScreen(
                    chat = chat,
                    currentUserId = "alice",
                    messages = listOf(
                        Message(
                            id = "v1",
                            chatId = "room_1",
                            senderId = "alice",
                            senderName = "Alice Creator",
                            type = "video",
                            attachmentUrl = "data:video/mp4;base64,AAAA",
                            text = "Launchpad clip"
                        )
                    ),
                    directoryUsers = listOf(alice, bob, charlie),
                    onBack = {},
                    onDirectMessageUser = {},
                    onBlockUser = { _, _ -> },
                    onReportUser = { _, _ -> },
                    onToggleGroupAdmin = { target, makeAdmin ->
                        recordedCalls.add(target to makeAdmin)
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("chat_details_lazy_column")
            .performScrollToNode(hasTestTag("chat_details_group_members_card"))
        composeRule.waitForIdle()

        // Alice (creator) cannot toggle herself
        composeRule.onNodeWithTag("toggle_group_admin_alice").assertDoesNotExist()
        // Bob is regular member -> Make Admin; Charlie is admin -> Dismiss
        composeRule.onNodeWithTag("toggle_group_admin_bob").performClick()
        composeRule.onNodeWithTag("toggle_group_admin_charlie").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("bob" to true, "charlie" to false), recordedCalls)
    }

    @Test
    fun channelFeedDialog_allowsAdminPromotionAndDismissal() {
        val alice = UserProfile(userId = "alice", displayName = "Alice Creator", email = "alice@satlimited.com")
        val bob = UserProfile(userId = "bob", displayName = "Bob Engineer", email = "bob@satlimited.com")
        val channel = Channel(
            id = "chan_1",
            name = "Telemetry Broadcast",
            creatorId = "alice",
            adminIds = listOf("alice"),
            subscriberIds = listOf("alice", "bob"),
            followerCount = 2
        )
        val toggledAdmins = mutableListOf<String>()

        composeRule.setContent {
            SATMessengerTheme {
                ChannelFeedDialog(
                    channel = channel,
                    currentUserId = "alice",
                    posts = emptyList(),
                    directoryUsers = listOf(alice, bob),
                    onDismiss = {},
                    onPostMessage = { _, _ -> },
                    onToggleChannelAdmin = { target, makeAdmin ->
                        toggledAdmins.add("channel:$target:$makeAdmin")
                    },
                    onReact = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("channel_manage_admins_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("toggle_channel_admin_bob").performClick()
        composeRule.waitForIdle()
        assertTrue(toggledAdmins.contains("channel:bob:true"))
    }

    @Test
    fun communitiesScreen_allowsAdminPromotionAndDismissal() {
        val alice = UserProfile(userId = "alice", displayName = "Alice Creator", email = "alice@satlimited.com")
        val bob = UserProfile(userId = "bob", displayName = "Bob Engineer", email = "bob@satlimited.com")
        val community = Community(
            id = "comm_1",
            name = "SAT Ground Division",
            creatorId = "alice",
            adminIds = listOf("alice"),
            memberIds = listOf("alice", "bob")
        )
        val toggledAdmins = mutableListOf<String>()

        composeRule.setContent {
            SATMessengerTheme {
                CommunitiesScreen(
                    currentUserId = "alice",
                    communities = listOf(community),
                    allChats = emptyList(),
                    directoryUsers = listOf(alice, bob),
                    onOpenChat = {},
                    onCreateCommunity = { _, _, _ -> },
                    onJoinCommunity = {},
                    onToggleCommunityAdmin = { cid, target, makeAdmin ->
                        toggledAdmins.add("community:$cid:$target:$makeAdmin")
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("community_manage_admins_comm_1").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("toggle_community_admin_bob").performClick()
        composeRule.waitForIdle()
        assertTrue(toggledAdmins.contains("community:comm_1:bob:true"))
    }

    @Test
    fun statusScreen_createDialogShowsZeroPermissionMediaPicker() {
        composeRule.setContent {
            SATMessengerTheme {
                StatusScreen(
                    currentUserId = "alice",
                    currentUserProfile = UserProfile(userId = "alice", displayName = "Alice"),
                    statuses = emptyList(),
                    onPostStatus = { _, _, _ -> },
                    onDeleteStatus = {},
                    onMarkStatusViewed = {},
                    onReplyToStatus = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("create_status_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("status_pick_media_button").assertIsDisplayed()
        composeRule.onNodeWithTag("status_media_url_input").assertIsDisplayed()
    }

    @Test
    fun androidManifest_doesNotRequestBroadStoragePermissions() {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions?.toSet() ?: emptySet()
        assertNotNull(requested)
        assertFalse(requested.contains("android.permission.READ_EXTERNAL_STORAGE"))
        assertFalse(requested.contains("android.permission.WRITE_EXTERNAL_STORAGE"))
        assertFalse(requested.contains("android.permission.READ_MEDIA_IMAGES"))
        assertFalse(requested.contains("android.permission.READ_MEDIA_VIDEO"))
    }

    @Test
    fun profileScreen_displaysPhotoControlsAndRemovesAvatar() {
        var updatedAvatar: String? = "initial"

        composeRule.setContent {
            SATMessengerTheme {
                ProfileScreen(
                    userProfile = UserProfile(
                        userId = "u1",
                        displayName = "Alice Creator",
                        email = "alice@satlimited.com",
                        avatarUrl = "data:image/jpeg;base64,AAAA"
                    ),
                    privacySettings = PrivacySettings(userId = "u1"),
                    blockedUsers = emptyList(),
                    onBack = {},
                    onUpdateProfile = { _, _, _ -> },
                    onUpdateAvatar = { updatedAvatar = it },
                    onUpdatePrivacy = { _, _ -> },
                    onUnblockUser = {},
                    onSignOut = {},
                    onDeleteAccount = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("profile_change_photo_button").assertIsDisplayed()
        composeRule.onNodeWithTag("profile_remove_photo_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(null, updatedAvatar)

        composeRule.onNodeWithTag("profile_app_version_text").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "SAT Messenger v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun chatDetailsScreen_groupAdminCanRemoveIcon_nonAdminCannotEdit() {
        var updatedGroupIcon: String? = "initial"
        val chat = Chat(
            id = "room_icon_1",
            type = "group",
            name = "Ops Group",
            iconUrl = "data:image/jpeg;base64,AAAA",
            memberIds = listOf("alice", "bob"),
            adminIds = listOf("alice"),
            createdBy = "alice"
        )

        composeRule.setContent {
            SATMessengerTheme {
                ChatDetailsScreen(
                    chat = chat,
                    currentUserId = "alice",
                    messages = emptyList(),
                    directoryUsers = listOf(
                        UserProfile(userId = "alice", displayName = "Alice Creator"),
                        UserProfile(userId = "bob", displayName = "Bob Member")
                    ),
                    onBack = {},
                    onDirectMessageUser = {},
                    onBlockUser = { _, _ -> },
                    onReportUser = { _, _ -> },
                    onUpdateGroupIcon = { updatedGroupIcon = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("group_change_icon_button").assertIsDisplayed()
        composeRule.onNodeWithTag("group_remove_icon_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(null, updatedGroupIcon)
    }

    @Test
    fun chatDetailsScreen_nonAdminDoesNotSeeGroupIconControls() {
        val chat = Chat(
            id = "room_icon_2",
            type = "group",
            name = "Ops Group",
            iconUrl = "data:image/jpeg;base64,AAAA",
            memberIds = listOf("alice", "bob"),
            adminIds = listOf("alice"),
            createdBy = "alice"
        )

        composeRule.setContent {
            SATMessengerTheme {
                ChatDetailsScreen(
                    chat = chat,
                    currentUserId = "bob",
                    messages = emptyList(),
                    directoryUsers = listOf(
                        UserProfile(userId = "alice", displayName = "Alice Creator"),
                        UserProfile(userId = "bob", displayName = "Bob Member")
                    ),
                    onBack = {},
                    onDirectMessageUser = {},
                    onBlockUser = { _, _ -> },
                    onReportUser = { _, _ -> },
                    onUpdateGroupIcon = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("group_change_icon_button").assertDoesNotExist()
        composeRule.onNodeWithTag("group_remove_icon_button").assertDoesNotExist()
    }

    @Test
    fun channelFeedDialog_adminCanRemoveChannelIcon() {
        var updatedIcon: String? = "initial"
        val channel = Channel(
            id = "chan_icon_1",
            name = "Telemetry Broadcast",
            iconUrl = "data:image/jpeg;base64,AAAA",
            creatorId = "alice",
            adminIds = listOf("alice"),
            subscriberIds = listOf("alice"),
            followerCount = 1
        )

        composeRule.setContent {
            SATMessengerTheme {
                ChannelFeedDialog(
                    channel = channel,
                    currentUserId = "alice",
                    posts = emptyList(),
                    onDismiss = {},
                    onPostMessage = { _, _ -> },
                    onUpdateChannelIcon = { updatedIcon = it },
                    onReact = { _, _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("channel_feed_settings_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("channel_change_icon_button").assertIsDisplayed()
        composeRule.onNodeWithTag("channel_remove_icon_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(null, updatedIcon)
    }

    @Test
    fun communitiesScreen_adminCanRemoveCommunityIcon() {
        var updatedCommunityId: String? = null
        var updatedIcon: String? = "initial"
        val community = Community(
            id = "comm_icon_1",
            name = "SAT Engineering",
            iconUrl = "data:image/jpeg;base64,AAAA",
            creatorId = "alice",
            adminIds = listOf("alice"),
            memberIds = listOf("alice")
        )

        composeRule.setContent {
            SATMessengerTheme {
                CommunitiesScreen(
                    currentUserId = "alice",
                    communities = listOf(community),
                    allChats = emptyList(),
                    onOpenChat = {},
                    onCreateCommunity = { _, _, _ -> },
                    onJoinCommunity = {},
                    onUpdateCommunityIcon = { cid, icon ->
                        updatedCommunityId = cid
                        updatedIcon = icon
                    }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("community_edit_icon_comm_icon_1").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("community_change_icon_button").assertIsDisplayed()
        composeRule.onNodeWithTag("community_remove_icon_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals("comm_icon_1", updatedCommunityId)
        assertEquals(null, updatedIcon)
    }
}
