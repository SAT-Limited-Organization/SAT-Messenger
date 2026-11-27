package com.example.ui.screens.contacts

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AvailabilityStatus
import com.example.data.model.ContactEntry
import com.example.data.model.FriendRequest
import com.example.data.model.UserProfile
import com.example.data.repository.DeviceContactMatch
import com.example.ui.components.AvailabilityStatusPill
import com.example.ui.components.EmptyStateView
import com.example.ui.components.UserAvatar
import com.example.ui.components.availabilityColor
import com.example.ui.components.formatLastSeenRelative
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import com.example.ui.theme.SatWarningAmber
import com.example.util.UserSearchMatcher

private enum class ContactFilter(val label: String) {
    ALL("All"),
    ONLINE("Online"),
    FRIENDS("Friends"),
    ON_DEVICE("On Device"),
    REQUESTS("Requests"),
    COLLEAGUES("Colleagues")
}

private enum class FriendRelationState {
    FRIEND,
    INCOMING_REQUEST,
    OUTGOING_PENDING,
    NONE
}

@Composable
fun ContactsScreen(
    currentUserId: String,
    currentUserProfile: UserProfile?,
    directoryUsers: List<UserProfile>,
    savedContacts: List<ContactEntry>,
    onDirectMessageUser: (UserProfile) -> Unit,
    onUpdateAvailability: (AvailabilityStatus) -> Unit,
    onToggleFriend: (UserProfile) -> Unit,
    friendRequests: List<FriendRequest> = emptyList(),
    deviceContactMatches: Map<String, DeviceContactMatch> = emptyMap(),
    isSyncingDeviceContacts: Boolean = false,
    hasDeviceContactsPermission: Boolean = false,
    deviceContactsScannedCount: Int = 0,
    onSyncDeviceContacts: () -> Unit = {},
    onOpenFindFriends: () -> Unit = {},
    onSendFriendRequest: (UserProfile) -> Unit = onToggleFriend,
    onRespondToFriendRequest: (FriendRequest, Boolean) -> Unit = { _, _ -> },
    onCancelOrRemoveFriend: (UserProfile) -> Unit = onToggleFriend,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ContactFilter.ALL) }
    var inspectedContact by remember { mutableStateOf<UserProfile?>(null) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSyncDeviceContacts()
        } else {
            Toast.makeText(
                context,
                "Contacts permission denied. Enable Contacts access in Android Settings to discover device friends.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val friendIds = remember(savedContacts, friendRequests, currentUserId) {
        val savedFriendIds = savedContacts
            .filter { it.relationship == "friend" }
            .map { it.contactUserId }
        val acceptedRequestIds = friendRequests
            .filter { it.status == "accepted" }
            .map { req -> if (req.fromUserId == currentUserId) req.toUserId else req.fromUserId }
        (savedFriendIds + acceptedRequestIds).filter { it.isNotBlank() }.toSet()
    }

    val incomingPendingRequests = remember(friendRequests, currentUserId) {
        friendRequests.filter { it.status == "pending" && it.toUserId == currentUserId }
    }

    val outgoingPendingIds = remember(friendRequests, currentUserId) {
        friendRequests
            .filter { it.status == "pending" && it.fromUserId == currentUserId }
            .map { it.toUserId }
            .toSet()
    }

    val incomingBySenderId = remember(incomingPendingRequests) {
        incomingPendingRequests.associateBy { it.fromUserId }
    }

    val otherUsers = remember(directoryUsers, currentUserId) {
        directoryUsers.filter { it.userId != currentUserId }
    }

    val onlineCount = remember(otherUsers) {
        otherUsers.count { it.isOnline && it.resolvedAvailability() != AvailabilityStatus.OFFLINE }
    }

    val friendsCount = remember(otherUsers, friendIds) {
        otherUsers.count { friendIds.contains(it.userId) }
    }

    val onDeviceCount = remember(otherUsers, deviceContactMatches) {
        otherUsers.count { deviceContactMatches.containsKey(it.userId) }
    }

    val colleaguesCount = remember(otherUsers, friendIds) {
        otherUsers.count { !friendIds.contains(it.userId) }
    }

    val filteredContacts = remember(
        otherUsers,
        friendIds,
        incomingBySenderId,
        outgoingPendingIds,
        deviceContactMatches,
        searchQuery,
        selectedFilter
    ) {
        val query = searchQuery.trim()
        otherUsers
            .filter { user ->
                val isFriend = friendIds.contains(user.userId)
                val isOnDevice = deviceContactMatches.containsKey(user.userId)
                val hasRequest = incomingBySenderId.containsKey(user.userId) || outgoingPendingIds.contains(user.userId)
                val matchesFilter = when (selectedFilter) {
                    ContactFilter.ALL -> true
                    ContactFilter.ONLINE -> user.isOnline && user.resolvedAvailability() != AvailabilityStatus.OFFLINE
                    ContactFilter.FRIENDS -> isFriend
                    ContactFilter.ON_DEVICE -> isOnDevice
                    ContactFilter.REQUESTS -> hasRequest
                    ContactFilter.COLLEAGUES -> !isFriend
                }
                if (!matchesFilter) return@filter false

                UserSearchMatcher.matches(user, query, includeStatusFields = true)
            }
            .sortedWith(
                compareByDescending<UserProfile> { incomingBySenderId.containsKey(it.userId) }
                    .thenByDescending { friendIds.contains(it.userId) }
                    .thenByDescending { deviceContactMatches.containsKey(it.userId) }
                    .thenByDescending { it.isOnline }
                    .thenBy { availabilitySortPriority(it.resolvedAvailability()) }
                    .thenBy { it.username.lowercase() }
            )
    }

    val myStatus = currentUserProfile?.resolvedAvailability() ?: AvailabilityStatus.AVAILABLE
    val selectableStatuses = remember {
        listOf(
            AvailabilityStatus.AVAILABLE,
            AvailabilityStatus.BUSY,
            AvailabilityStatus.AWAY,
            AvailabilityStatus.IN_MEETING,
            AvailabilityStatus.DO_NOT_DISTURB
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Real-Time My Availability & Firestore Sync Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("my_availability_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    UserAvatar(
                        name = currentUserProfile?.username ?: "Me",
                        avatarUrl = currentUserProfile?.avatarUrl,
                        isOnline = currentUserProfile?.isOnline ?: true,
                        availabilityStatus = myStatus,
                        size = 44.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Live Availability",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = SatTeal,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Synced via Firestore • $onlineCount online now",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    AvailabilityStatusPill(
                        status = myStatus,
                        modifier = Modifier.testTag("my_current_availability_pill")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectableStatuses, key = { it.name }) { status ->
                        val isSelected = myStatus == status
                        val chipAccent = availabilityColor(status)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateAvailability(status) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isSelected) Color.White else chipAccent,
                                            CircleShape
                                        )
                                )
                            },
                            label = {
                                Text(
                                    text = status.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = chipAccent,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("availability_chip_${status.name.lowercase()}")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Contacts Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search name, @username, email, phone, status...") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SatTeal)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("contacts_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips: All / Online / Friends / On Device / Requests / Colleagues
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ContactFilter.entries) { filter ->
                val count = when (filter) {
                    ContactFilter.ALL -> otherUsers.size
                    ContactFilter.ONLINE -> onlineCount
                    ContactFilter.FRIENDS -> friendsCount
                    ContactFilter.ON_DEVICE -> onDeviceCount
                    ContactFilter.REQUESTS -> incomingPendingRequests.size + outgoingPendingIds.size
                    ContactFilter.COLLEAGUES -> colleaguesCount
                }
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text("${filter.label} ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SatElectricBlue,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("contact_filter_${filter.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Synced Contacts, Device Discovery & Friend Requests Scrollable Feed
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("contacts_lazy_column"),
            contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)
        ) {
            if (searchQuery.isBlank()) {
                item(key = "device_contacts_sync_card") {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SatTeal.copy(alpha = 0.10f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onOpenFindFriends() }
                            .testTag("device_contacts_sync_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    tint = SatTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (hasDeviceContactsPermission && deviceContactsScannedCount > 0) {
                                            "Find Friends: $onDeviceCount matched on device"
                                        } else {
                                            "Find Friends Directory"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (hasDeviceContactsPermission) {
                                            "Scanned $deviceContactsScannedCount contacts via phone & email"
                                        } else {
                                            "Scan your device contacts to match registered SAT Messenger users"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onOpenFindFriends,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SatTeal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_find_friends_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Find Friends", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.READ_CONTACTS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            onSyncDeviceContacts()
                                        } else {
                                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                        }
                                    },
                                    enabled = !isSyncingDeviceContacts,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("sync_device_contacts_button")
                                ) {
                                    if (isSyncingDeviceContacts) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(if (hasDeviceContactsPermission) "Resync" else "Scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (incomingPendingRequests.isNotEmpty() && selectedFilter != ContactFilter.FRIENDS) {
                item(key = "incoming_friend_requests_card") {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SatElectricBlue.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, SatElectricBlue.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("incoming_friend_requests_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Incoming Friend Requests (${incomingPendingRequests.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SatElectricBlue
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            incomingPendingRequests.take(3).forEach { req ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    UserAvatar(
                                        name = req.fromUserName.ifBlank { "User" },
                                        avatarUrl = req.fromUserAvatarUrl,
                                        isOnline = false,
                                        size = 34.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = req.fromUserName.ifBlank { req.fromUserId },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(
                                        onClick = { onRespondToFriendRequest(req, false) },
                                        modifier = Modifier.testTag("decline_friend_request_${req.id}")
                                    ) {
                                        Text("Decline", color = SatErrorRed, fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { onRespondToFriendRequest(req, true) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("accept_friend_request_${req.id}")
                                    ) {
                                        Text("Accept", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredContacts.isEmpty()) {
                item(key = "contacts_empty_state") {
                    val isFiltered = searchQuery.isNotBlank() || selectedFilter != ContactFilter.ALL
                    EmptyStateView(
                        icon = Icons.Default.People,
                        title = when {
                            searchQuery.isNotBlank() -> "No Contacts Match \"$searchQuery\""
                            selectedFilter == ContactFilter.FRIENDS -> "No Friends Yet"
                            selectedFilter == ContactFilter.ON_DEVICE -> "No Device Contacts Matched"
                            selectedFilter == ContactFilter.REQUESTS -> "No Pending Friend Requests"
                            selectedFilter == ContactFilter.ONLINE -> "No Contacts Online Right Now"
                            else -> "No Synced Contacts Yet"
                        },
                        message = when {
                            searchQuery.isNotBlank() -> "Try searching by registered name, @primary/@secondary username, email, phone number, or status."
                            selectedFilter == ContactFilter.FRIENDS -> "Send a friend request or tap the star icon next to any colleague to add them as a friend."
                            selectedFilter == ContactFilter.ON_DEVICE -> "Tap 'Find' or 'Resync' above to scan your phone contacts for registered SAT Messenger users."
                            selectedFilter == ContactFilter.REQUESTS -> "Incoming and sent friend requests appear here."
                            selectedFilter == ContactFilter.ONLINE -> "Switch back to All to see offline contacts and their last seen timestamps."
                            else -> "Profiles appear here automatically in real time as friends and colleagues sign in to SAT Messenger."
                        },
                        actionLabel = if (isFiltered) "Show All Contacts" else null,
                        onAction = if (isFiltered) {
                            {
                                searchQuery = ""
                                selectedFilter = ContactFilter.ALL
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            } else {
                items(filteredContacts, key = { it.userId }) { user ->
                    val incomingReq = incomingBySenderId[user.userId]
                    val relationState = when {
                        friendIds.contains(user.userId) -> FriendRelationState.FRIEND
                        incomingReq != null -> FriendRelationState.INCOMING_REQUEST
                        outgoingPendingIds.contains(user.userId) -> FriendRelationState.OUTGOING_PENDING
                        else -> FriendRelationState.NONE
                    }
                    val deviceMatch = deviceContactMatches[user.userId]

                    ContactRowItem(
                        user = user,
                        relationState = relationState,
                        deviceMatch = deviceMatch,
                        onInspect = { inspectedContact = user },
                        onToggleFriend = { onToggleFriend(user) },
                        onMessage = { onDirectMessageUser(user) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        modifier = Modifier.padding(start = 66.dp)
                    )
                }
            }
        }
    }

    inspectedContact?.let { contact ->
        val liveUser = directoryUsers.firstOrNull { it.userId == contact.userId } ?: contact
        val incomingReq = incomingBySenderId[liveUser.userId]
        val isFriend = friendIds.contains(liveUser.userId)
        val isOutgoingPending = outgoingPendingIds.contains(liveUser.userId)
        val deviceMatch = deviceContactMatches[liveUser.userId]
        val liveStatus = liveUser.resolvedAvailability()

        AlertDialog(
            onDismissRequest = { inspectedContact = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        name = liveUser.username,
                        avatarUrl = liveUser.avatarUrl,
                        isOnline = liveUser.isOnline,
                        availabilityStatus = liveStatus,
                        size = 52.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = liveUser.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Primary: ${liveUser.primaryUsernameHandle}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SatTeal
                        )
                        liveUser.secondaryUsernameHandle?.let { sec ->
                            Text(
                                text = "Secondary: $sec",
                                style = MaterialTheme.typography.labelSmall,
                                color = SatElectricBlue
                            )
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvailabilityStatusPill(
                            status = liveStatus,
                            lastSeen = liveUser.lastSeen,
                            showOfflineLastSeen = true
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isFriend -> SatWarningAmber.copy(alpha = 0.18f)
                                incomingReq != null -> SatOnlineGreen.copy(alpha = 0.16f)
                                isOutgoingPending -> SatTeal.copy(alpha = 0.16f)
                                else -> SatElectricBlue.copy(alpha = 0.14f)
                            }
                        ) {
                            Text(
                                text = when {
                                    isFriend -> "Friend"
                                    incomingReq != null -> "Requested You"
                                    isOutgoingPending -> "Request Sent"
                                    else -> "Colleague"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isFriend -> SatWarningAmber
                                    incomingReq != null -> SatOnlineGreen
                                    isOutgoingPending -> SatTeal
                                    else -> SatElectricBlue
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (deviceMatch != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SatOnlineGreen.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "On Your Device • Saved as \"${deviceMatch.deviceContactName}\" (${deviceMatch.matchedVia})",
                                style = MaterialTheme.typography.labelSmall,
                                color = SatOnlineGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!liveUser.statusMessage.isNullOrBlank()) {
                        Text(
                            text = "\"${liveUser.statusMessage}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = if (liveUser.isOnline) {
                            "Active now • Real-time sync enabled"
                        } else {
                            formatLastSeenRelative(liveUser.lastSeen)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (liveUser.email.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    copyToClipboard(context, "Email", liveUser.email)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = SatTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = liveUser.email,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy email", modifier = Modifier.size(14.dp))
                        }
                    }

                    if (!liveUser.phoneNumber.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    copyToClipboard(context, "Phone", liveUser.phoneNumber)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = SatElectricBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = liveUser.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy phone", modifier = Modifier.size(14.dp))
                        }
                    }

                    if (incomingReq != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onRespondToFriendRequest(incomingReq, false) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Decline", color = SatErrorRed)
                            }
                            Button(
                                onClick = { onRespondToFriendRequest(incomingReq, true) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("dialog_accept_friend_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (isFriend || isOutgoingPending) {
                                    onCancelOrRemoveFriend(liveUser)
                                } else {
                                    onSendFriendRequest(liveUser)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_toggle_friend_button")
                        ) {
                            Icon(
                                imageVector = when {
                                    isFriend -> Icons.Default.Star
                                    isOutgoingPending -> Icons.Default.HourglassTop
                                    else -> Icons.Default.PersonAdd
                                },
                                contentDescription = null,
                                tint = if (isFriend) SatWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when {
                                    isFriend -> "Remove from Friends"
                                    isOutgoingPending -> "Cancel Friend Request"
                                    else -> "Send Friend Request"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        inspectedContact = null
                        onDirectMessageUser(liveUser)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SatElectricBlue),
                    modifier = Modifier.testTag("dialog_message_contact_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Message")
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedContact = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ContactRowItem(
    user: UserProfile,
    relationState: FriendRelationState,
    deviceMatch: DeviceContactMatch?,
    onInspect: () -> Unit,
    onToggleFriend: () -> Unit,
    onMessage: () -> Unit
) {
    val availability = user.resolvedAvailability()
    val isFriend = relationState == FriendRelationState.FRIEND

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("contact_item_${user.userId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = user.username,
            avatarUrl = user.avatarUrl,
            isOnline = user.isOnline,
            availabilityStatus = availability,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (relationState) {
                        FriendRelationState.FRIEND -> SatWarningAmber.copy(alpha = 0.16f)
                        FriendRelationState.INCOMING_REQUEST -> SatOnlineGreen.copy(alpha = 0.16f)
                        FriendRelationState.OUTGOING_PENDING -> SatElectricBlue.copy(alpha = 0.14f)
                        FriendRelationState.NONE -> SatTeal.copy(alpha = 0.14f)
                    }
                ) {
                    Text(
                        text = when (relationState) {
                            FriendRelationState.FRIEND -> "Friend"
                            FriendRelationState.INCOMING_REQUEST -> "Request"
                            FriendRelationState.OUTGOING_PENDING -> "Pending"
                            FriendRelationState.NONE -> "Colleague"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (relationState) {
                            FriendRelationState.FRIEND -> SatWarningAmber
                            FriendRelationState.INCOMING_REQUEST -> SatOnlineGreen
                            FriendRelationState.OUTGOING_PENDING -> SatElectricBlue
                            FriendRelationState.NONE -> SatTeal
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Primary & Secondary Usernames
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = user.primaryUsernameHandle,
                    style = MaterialTheme.typography.labelSmall,
                    color = SatTeal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                user.secondaryUsernameHandle?.let { sec ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AlternateEmail,
                            contentDescription = null,
                            tint = SatElectricBlue,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = sec,
                            style = MaterialTheme.typography.labelSmall,
                            color = SatElectricBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (deviceMatch != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SatOnlineGreen.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "On Device • Saved as ${deviceMatch.deviceContactName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = SatOnlineGreen,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AvailabilityStatusPill(
                    status = availability,
                    lastSeen = user.lastSeen,
                    showOfflineLastSeen = true
                )
                val customMsg = user.statusMessage?.takeIf {
                    it.isNotBlank() && !it.equals(availability.label, ignoreCase = true)
                }
                if (customMsg != null) {
                    Text(
                        text = customMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (user.email.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onToggleFriend,
            modifier = Modifier
                .size(48.dp)
                .testTag("contact_friend_toggle_${user.userId}")
        ) {
            Icon(
                imageVector = when (relationState) {
                    FriendRelationState.FRIEND -> Icons.Default.Star
                    FriendRelationState.INCOMING_REQUEST -> Icons.Default.Check
                    FriendRelationState.OUTGOING_PENDING -> Icons.Default.HourglassTop
                    FriendRelationState.NONE -> Icons.Default.StarBorder
                },
                contentDescription = when (relationState) {
                    FriendRelationState.FRIEND -> "Remove from friends"
                    FriendRelationState.INCOMING_REQUEST -> "Accept friend request"
                    FriendRelationState.OUTGOING_PENDING -> "Cancel friend request"
                    FriendRelationState.NONE -> "Add friend"
                },
                tint = when (relationState) {
                    FriendRelationState.FRIEND -> SatWarningAmber
                    FriendRelationState.INCOMING_REQUEST -> SatOnlineGreen
                    FriendRelationState.OUTGOING_PENDING -> SatElectricBlue
                    FriendRelationState.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        IconButton(
            onClick = onMessage,
            modifier = Modifier
                .size(48.dp)
                .testTag("contact_message_button_${user.userId}")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = "Message ${user.username}",
                tint = SatElectricBlue
            )
        }
    }
}

private fun availabilitySortPriority(status: AvailabilityStatus): Int {
    return when (status) {
        AvailabilityStatus.AVAILABLE -> 0
        AvailabilityStatus.IN_MEETING -> 1
        AvailabilityStatus.BUSY -> 2
        AvailabilityStatus.AWAY -> 3
        AvailabilityStatus.DO_NOT_DISTURB -> 4
        AvailabilityStatus.OFFLINE -> 5
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}
