package com.example.data.service

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.ContactsContract
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
import com.example.data.model.UserProfile
import com.example.ui.screens.contacts.FindFriendsScreen
import com.example.ui.theme.SATMessengerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h1200dp")
class ContactDiscoveryServiceAndFindFriendsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(
                app,
                FirebaseOptions.Builder()
                    .setApplicationId("1:815554441605:android:test")
                    .setApiKey("AIzaSyTestKeyForUnitTestsOnly")
                    .setProjectId("sat-limited")
                    .build()
            )
        }
    }

    @Test
    fun contactDiscoveryService_scansContactsContractAndCorrelatesByEmailAndPhone() = runBlocking {
        shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)

        val provider = FakeContactsContentProvider(
            phoneRows = listOf(
                arrayOf("101", "Alice Work", "(555) 234-5678", "+15552345678", null),
                arrayOf("102", "Unregistered Neighbor", "+1-555-000-9999", "+15550009999", null)
            ),
            emailRows = listOf(
                arrayOf("101", "Alice Work", "alice.moyo@gmail.com", null),
                arrayOf("103", "Bob Satellite Ops", "bob.ops@satlimited.com", null)
            )
        )
        val providerInfo = android.content.pm.ProviderInfo().apply {
            authority = ContactsContract.AUTHORITY
        }
        provider.attachInfo(app, providerInfo)
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(
            ContactsContract.AUTHORITY,
            provider
        )

        val alice = UserProfile(
            userId = "u_alice",
            displayName = "Alice Moyo",
            email = "alice.moyo@gmail.com",
            primaryUsername = "alice.moyo",
            secondaryUsername = "orbital_alice",
            phoneNumber = "+1 555 234 5678",
            isOnline = true,
            availabilityStatus = AvailabilityStatus.AVAILABLE.label
        )
        val bob = UserProfile(
            userId = "u_bob",
            displayName = "Bob Dube",
            email = "bob.ops@satlimited.com",
            primaryUsername = "bob.ops",
            isOnline = false
        )
        val stranger = UserProfile(
            userId = "u_stranger",
            displayName = "Stranger",
            email = "stranger@nowhere.com",
            phoneNumber = "+44 7700 900077"
        )

        val service = ContactDiscoveryService()
        val state = service.scanAndPopulateDirectory(
            context = app,
            directoryUsers = listOf(alice, bob, stranger),
            currentUserId = "me_1"
        )

        assertTrue(state.hasPermission)
        assertFalse(state.isScanning)
        assertEquals(3, state.scannedContactCount)
        assertEquals(2, state.entries.size)

        val aliceEntry = state.entries.first { it.user.userId == "u_alice" }
        assertEquals("Alice Work", aliceEntry.deviceContactName)
        assertEquals("Phone & Email", aliceEntry.matchedVia)

        val bobEntry = state.entries.first { it.user.userId == "u_bob" }
        assertEquals("Bob Satellite Ops", bobEntry.deviceContactName)
        assertEquals("Email", bobEntry.matchedVia)

        // Real-time directory refresh updates correlation without re-scanning ContentResolver
        val charlie = UserProfile(
            userId = "u_charlie",
            displayName = "Charlie",
            email = "charlie@nowhere.com",
            phoneNumber = "+15550009999"
        )
        val updatedEntries = service.recorrelateWithRegisteredUsers(
            registeredUsers = listOf(alice, bob, charlie),
            currentUserId = "me_1"
        )
        assertEquals(3, updatedEntries.size)
        val charlieEntry = updatedEntries.first { it.user.userId == "u_charlie" }
        assertEquals("Unregistered Neighbor", charlieEntry.deviceContactName)
        assertEquals("Phone", charlieEntry.matchedVia)
    }

    @Test
    fun findFriendsScreen_showsPermissionPromptWhenNotGranted() {
        composeRule.setContent {
            SATMessengerTheme {
                FindFriendsScreen(
                    currentUserId = "me_1",
                    directoryState = FindFriendsDirectoryState(hasPermission = false),
                    savedContacts = emptyList(),
                    friendRequests = emptyList(),
                    onBack = {},
                    onScanContacts = {},
                    onSendFriendRequest = {},
                    onRespondToFriendRequest = { _, _ -> },
                    onCancelOrRemoveFriend = {},
                    onDirectMessageUser = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("find_friends_permission_card").assertIsDisplayed()
        composeRule.onNodeWithTag("find_friends_grant_permission_button").assertIsDisplayed()
    }

    @Test
    fun findFriendsScreen_populatesDirectoryFiltersSearchesAndAddsFriends() {
        var addedFriendId: String? = null
        var messagedUserId: String? = null

        val alice = UserProfile(
            userId = "u_alice",
            displayName = "Alice Moyo",
            email = "alice.moyo@gmail.com",
            primaryUsername = "alice.moyo",
            secondaryUsername = "sat_alice",
            phoneNumber = "+15552345678",
            isOnline = true
        )
        val bob = UserProfile(
            userId = "u_bob",
            displayName = "Bob Dube",
            email = "bob.ops@satlimited.com",
            primaryUsername = "bob.ops",
            isOnline = false
        )

        val entries = listOf(
            FindFriendsEntry(
                user = alice,
                deviceContactName = "Alice Work",
                matchedVia = "Phone",
                matchedIdentifier = "+15552345678"
            ),
            FindFriendsEntry(
                user = bob,
                deviceContactName = "Bob Satellite",
                matchedVia = "Email",
                matchedIdentifier = "bob.ops@satlimited.com"
            )
        )

        val directoryState = FindFriendsDirectoryState(
            hasPermission = true,
            isScanning = false,
            entries = entries,
            scannedContactCount = 24
        )

        composeRule.setContent {
            SATMessengerTheme {
                FindFriendsScreen(
                    currentUserId = "me_1",
                    directoryState = directoryState,
                    savedContacts = emptyList(),
                    friendRequests = listOf(
                        FriendRequest(
                            id = "me_1_u_bob",
                            fromUserId = "me_1",
                            toUserId = "u_bob",
                            participantIds = listOf("me_1", "u_bob"),
                            status = "accepted"
                        )
                    ),
                    onBack = {},
                    onScanContacts = {},
                    onSendFriendRequest = { addedFriendId = it.userId },
                    onRespondToFriendRequest = { _, _ -> },
                    onCancelOrRemoveFriend = {},
                    onDirectMessageUser = { messagedUserId = it.userId }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("find_friends_summary_card").assertIsDisplayed()
        composeRule.onNodeWithTag("find_friends_item_u_alice").assertIsDisplayed()
        composeRule.onNodeWithTag("find_friends_item_u_bob").assertIsDisplayed()
        composeRule.onNodeWithText("In contacts as \"Alice Work\" • Phone").assertIsDisplayed()

        // Filter to Email matches -> only Bob remains
        composeRule.onNodeWithTag("find_friends_filter_email").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("find_friends_item_u_bob").assertIsDisplayed()
        composeRule.onNodeWithTag("find_friends_item_u_alice").assertDoesNotExist()

        // Back to All and search by saved device contact name
        composeRule.onNodeWithTag("find_friends_filter_all").performClick()
        composeRule.onNodeWithTag("find_friends_search_input").performTextInput("Alice Work")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("find_friends_item_u_alice").assertIsDisplayed()
        composeRule.onNodeWithTag("find_friends_item_u_bob").assertDoesNotExist()

        // Add Friend & Direct Message
        composeRule.onNodeWithTag("find_friends_add_button_u_alice").performClick()
        composeRule.onNodeWithTag("find_friends_message_button_u_alice").performClick()
        composeRule.waitForIdle()

        assertEquals("u_alice", addedFriendId)
        assertEquals("u_alice", messagedUserId)
    }

    private class FakeContactsContentProvider(
        private val phoneRows: List<Array<Any?>>,
        private val emailRows: List<Array<Any?>>
    ) : ContentProvider() {

        override fun onCreate(): Boolean = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
        ): Cursor {
            return when (uri) {
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI -> {
                    MatrixCursor(
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
                            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                        )
                    ).apply {
                        phoneRows.forEach { addRow(it) }
                    }
                }

                ContactsContract.CommonDataKinds.Email.CONTENT_URI -> {
                    MatrixCursor(
                        arrayOf(
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Email.ADDRESS,
                            ContactsContract.CommonDataKinds.Email.PHOTO_THUMBNAIL_URI
                        )
                    ).apply {
                        emailRows.forEach { addRow(it) }
                    }
                }

                else -> MatrixCursor(arrayOf())
            }
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
    }
}
