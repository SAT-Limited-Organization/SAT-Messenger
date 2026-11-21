package com.example.ui.screens.contacts

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AvailabilityStatus
import com.example.data.model.ContactEntry
import com.example.data.model.UserProfile
import com.example.ui.screens.manual.AppManualScreen
import com.example.ui.theme.SATMessengerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ContactsScreenAndManualTest {

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
    fun contactsScreen_displaysRealtimeAvailability_filtersAndActions() {
        var updatedStatus: AvailabilityStatus? = null
        var toggledFriendId: String? = null
        var messagedUserId: String? = null

        val me = UserProfile(
            userId = "me_1",
            displayName = "Simbarashe",
            email = "simba@satlimited.com",
            isOnline = true,
            availabilityStatus = AvailabilityStatus.AVAILABLE.label
        )
        val friendUser = UserProfile(
            userId = "u_friend",
            displayName = "Alice Moyo",
            email = "alice@satlimited.com",
            isOnline = true,
            availabilityStatus = AvailabilityStatus.IN_MEETING.label,
            statusMessage = "Q4 Planning"
        )
        val colleagueUser = UserProfile(
            userId = "u_colleague",
            displayName = "Brian Dube",
            email = "brian@satlimited.com",
            isOnline = true,
            availabilityStatus = AvailabilityStatus.BUSY.label
        )
        val offlineUser = UserProfile(
            userId = "u_offline",
            displayName = "Chido Ndlovu",
            email = "chido@satlimited.com",
            isOnline = false,
            availabilityStatus = AvailabilityStatus.OFFLINE.label,
            lastSeen = Timestamp(Date(System.currentTimeMillis() - 15 * 60_000L))
        )
        val savedContacts = listOf(
            ContactEntry(contactUserId = "u_friend", relationship = "friend")
        )

        composeRule.setContent {
            SATMessengerTheme {
                ContactsScreen(
                    currentUserId = me.userId,
                    currentUserProfile = me,
                    directoryUsers = listOf(me, friendUser, colleagueUser, offlineUser),
                    savedContacts = savedContacts,
                    onDirectMessageUser = { messagedUserId = it.userId },
                    onUpdateAvailability = { updatedStatus = it },
                    onToggleFriend = { toggledFriendId = it.userId }
                )
            }
        }
        composeRule.waitForIdle()

        // My availability card and synced peer statuses render
        composeRule.onNodeWithTag("my_availability_card").assertIsDisplayed()
        composeRule.onNodeWithTag("contacts_lazy_column").performScrollToNode(hasTestTag("contact_item_u_friend"))
        composeRule.onNodeWithTag("contact_item_u_friend").assertIsDisplayed()
        composeRule.onNodeWithTag("contacts_lazy_column").performScrollToNode(hasTestTag("contact_item_u_colleague"))
        composeRule.onNodeWithTag("contact_item_u_colleague").assertIsDisplayed()
        composeRule.onNodeWithTag("contacts_lazy_column").performScrollToNode(hasTestTag("contact_item_u_offline"))
        composeRule.onNodeWithTag("contact_item_u_offline").assertIsDisplayed()

        // Update my live availability
        composeRule.onNodeWithTag("availability_chip_busy").performClick()
        composeRule.waitForIdle()
        assertEquals(AvailabilityStatus.BUSY, updatedStatus)

        // Filter to Friends only
        composeRule.onNodeWithTag("contact_filter_friends").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("contact_item_u_friend").assertIsDisplayed()
        composeRule.onNodeWithTag("contact_item_u_colleague").assertDoesNotExist()
        composeRule.onNodeWithTag("contact_item_u_offline").assertDoesNotExist()

        // Filter to Online only
        composeRule.onNodeWithTag("contact_filter_online").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("contact_item_u_friend").assertIsDisplayed()
        composeRule.onNodeWithTag("contacts_lazy_column").performScrollToNode(hasTestTag("contact_item_u_colleague"))
        composeRule.onNodeWithTag("contact_item_u_colleague").assertIsDisplayed()
        composeRule.onNodeWithTag("contact_item_u_offline").assertDoesNotExist()

        // Back to All, search, star toggle & direct message
        composeRule.onNodeWithTag("contact_filter_all").performClick()
        composeRule.onNodeWithTag("contacts_search_input").performTextInput("Brian")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("contact_item_u_colleague").assertIsDisplayed()
        composeRule.onNodeWithTag("contact_item_u_friend").assertDoesNotExist()

        composeRule.onNodeWithTag("contact_friend_toggle_u_colleague").performClick()
        composeRule.onNodeWithTag("contact_message_button_u_colleague").performClick()
        composeRule.waitForIdle()

        assertEquals("u_colleague", toggledFriendId)
        assertEquals("u_colleague", messagedUserId)
    }

    @Test
    fun appManualScreen_includesAllLatestFeaturesAndFiltersBySearch() {
        composeRule.setContent {
            SATMessengerTheme {
                AppManualScreen(onBack = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("manual_search_field").performTextInput("Microsoft")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Registration & Sign-In (Google & Microsoft)").assertIsDisplayed()

        composeRule.onNodeWithTag("manual_search_field").performTextClearance()
        composeRule.onNodeWithTag("manual_search_field").performTextInput("Contacts Sync")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Contacts Sync & Real-Time Availability").assertIsDisplayed()

        composeRule.onNodeWithTag("manual_search_field").performTextClearance()
        composeRule.onNodeWithTag("manual_search_field").performTextInput("Voice Recording")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Voice Recording & In-Chat Audio Playback").assertIsDisplayed()
        composeRule.onNodeWithText("Registration & Sign-In (Google & Microsoft)").assertDoesNotExist()

        composeRule.onNodeWithTag("manual_search_field").performTextClearance()
        composeRule.onNodeWithTag("manual_search_field").performTextInput("Channel, Group")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Channel, Group & Community Invite Links").assertIsDisplayed()
    }
}
