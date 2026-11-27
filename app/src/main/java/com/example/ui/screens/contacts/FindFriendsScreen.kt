package com.example.ui.screens.contacts

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ContactEntry
import com.example.data.model.FriendRequest
import com.example.data.model.UserProfile
import com.example.data.service.FindFriendsDirectoryState
import com.example.data.service.FindFriendsEntry
import com.example.ui.components.AvailabilityStatusPill
import com.example.ui.components.EmptyStateView
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import com.example.ui.theme.SatWarningAmber
import com.example.util.UserSearchMatcher

private enum class FindFriendsFilter(val label: String) {
    ALL("All Matches"),
    DISCOVERABLE("Not Friends Yet"),
    FRIENDS("Already Friends"),
    PHONE("Phone Match"),
    EMAIL("Email Match")
}

private enum class DirectoryRelation {
    FRIEND,
    INCOMING_REQUEST,
    OUTGOING_PENDING,
    NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindFriendsScreen(
    currentUserId: String,
    directoryState: FindFriendsDirectoryState,
    savedContacts: List<ContactEntry>,
    friendRequests: List<FriendRequest>,
    onBack: () -> Unit,
    onScanContacts: () -> Unit,
    onSendFriendRequest: (UserProfile) -> Unit,
    onRespondToFriendRequest: (FriendRequest, Boolean) -> Unit,
    onCancelOrRemoveFriend: (UserProfile) -> Unit,
    onDirectMessageUser: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(FindFriendsFilter.ALL) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onScanContacts()
        } else {
            Toast.makeText(
                context,
                "Contacts permission denied. Enable Contacts access in Settings to find friends.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val triggerScanOrPermission = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted || directoryState.hasPermission) {
            onScanContacts()
        } else {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val friendIds = remember(savedContacts, friendRequests, currentUserId) {
        val savedFriends = savedContacts
            .filter { it.relationship == "friend" }
            .map { it.contactUserId }
        val accepted = friendRequests
            .filter { it.status == "accepted" }
            .map { req -> if (req.fromUserId == currentUserId) req.toUserId else req.fromUserId }
        (savedFriends + accepted).filter { it.isNotBlank() }.toSet()
    }

    val incomingBySender = remember(friendRequests, currentUserId) {
        friendRequests
            .filter { it.status == "pending" && it.toUserId == currentUserId }
            .associateBy { it.fromUserId }
    }

    val outgoingPendingIds = remember(friendRequests, currentUserId) {
        friendRequests
            .filter { it.status == "pending" && it.fromUserId == currentUserId }
            .map { it.toUserId }
            .toSet()
    }

    val filteredEntries = remember(
        directoryState.entries,
        searchQuery,
        selectedFilter,
        friendIds
    ) {
        val query = searchQuery.trim()
        directoryState.entries.filter { entry ->
            val isFriend = friendIds.contains(entry.user.userId)
            val matchesFilter = when (selectedFilter) {
                FindFriendsFilter.ALL -> true
                FindFriendsFilter.DISCOVERABLE -> !isFriend
                FindFriendsFilter.FRIENDS -> isFriend
                FindFriendsFilter.PHONE -> entry.matchedVia.contains("Phone", ignoreCase = true)
                FindFriendsFilter.EMAIL -> entry.matchedVia.contains("Email", ignoreCase = true)
            }
            if (!matchesFilter) return@filter false

            if (query.isBlank()) {
                true
            } else {
                entry.deviceContactName.contains(query, ignoreCase = true) ||
                    entry.matchedIdentifier.contains(query, ignoreCase = true) ||
                    UserSearchMatcher.matches(entry.user, query, includeStatusFields = true)
            }
        }
    }

    val phoneMatchCount = remember(directoryState.entries) {
        directoryState.entries.count { it.matchedVia.contains("Phone", ignoreCase = true) }
    }
    val emailMatchCount = remember(directoryState.entries) {
        directoryState.entries.count { it.matchedVia.contains("Email", ignoreCase = true) }
    }
    val notAddedCount = remember(directoryState.entries, friendIds) {
        directoryState.entries.count { !friendIds.contains(it.user.userId) }
    }
    val alreadyFriendsCount = remember(directoryState.entries, friendIds) {
        directoryState.entries.count { friendIds.contains(it.user.userId) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("find_friends_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Find Friends",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Device Contacts Directory",
                            style = MaterialTheme.typography.labelSmall,
                            color = SatTeal
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = triggerScanOrPermission,
                        enabled = !directoryState.isScanning,
                        modifier = Modifier.testTag("find_friends_scan_button")
                    ) {
                        if (directoryState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan Device Contacts",
                                tint = SatElectricBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Hero Summary Header
                Card(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SatTeal.copy(alpha = 0.25f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("find_friends_summary_card")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        SatTeal.copy(alpha = 0.14f),
                                        SatElectricBlue.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = SatTeal.copy(alpha = 0.18f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = null,
                                        tint = SatTeal,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        directoryState.isScanning -> "Scanning address book..."
                                        !directoryState.hasPermission -> "Connect Your Contacts"
                                        else -> "${directoryState.entries.size} Friend(s) Matched"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when {
                                        directoryState.isScanning -> "Correlating phone numbers & emails with SAT Messenger users"
                                        !directoryState.hasPermission -> "Scan ContactsContract locally to match phone numbers & emails"
                                        else -> "Scanned ${directoryState.scannedContactCount} device contacts • $phoneMatchCount phone • $emailMatchCount email"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        AnimatedVisibility(visible = directoryState.isScanning) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                color = SatTeal
                            )
                        }

                        if (!directoryState.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = directoryState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = SatErrorRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!directoryState.hasPermission) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("find_friends_permission_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = SatElectricBlue,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Discover People You Already Know",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "SAT Messenger scans your device address book via ContactsContract and correlates phone numbers and email addresses locally to build your Find Friends directory.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = triggerScanOrPermission,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SatElectricBlue),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("find_friends_grant_permission_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Allow Contacts & Scan Directory", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search matched friends, phone name, @username...") },
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
                            .testTag("find_friends_search_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(FindFriendsFilter.entries) { filter ->
                            val count = when (filter) {
                                FindFriendsFilter.ALL -> directoryState.entries.size
                                FindFriendsFilter.DISCOVERABLE -> notAddedCount
                                FindFriendsFilter.FRIENDS -> alreadyFriendsCount
                                FindFriendsFilter.PHONE -> phoneMatchCount
                                FindFriendsFilter.EMAIL -> emailMatchCount
                            }
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text("${filter.label} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SatElectricBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("find_friends_filter_${filter.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("find_friends_directory_list"),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp)
                    ) {
                        if (filteredEntries.isEmpty() && !directoryState.isScanning) {
                            item(key = "find_friends_empty") {
                                val hasFilterOrQuery = searchQuery.isNotBlank() || selectedFilter != FindFriendsFilter.ALL
                                EmptyStateView(
                                    icon = Icons.Default.People,
                                    title = when {
                                        searchQuery.isNotBlank() -> "No Friends Match \"$searchQuery\""
                                        directoryState.entries.isEmpty() -> "No Matching App Users Found"
                                        else -> "No Matches in This Filter"
                                    },
                                    message = when {
                                        searchQuery.isNotBlank() -> "Try searching by the name saved in your phone, registered name, @username, email, or phone number."
                                        directoryState.entries.isEmpty() -> "We scanned ${directoryState.scannedContactCount} device contacts, but none matched registered SAT Messenger emails or phone numbers yet."
                                        else -> "Switch filters to view all friends discovered from your address book."
                                    },
                                    actionLabel = if (hasFilterOrQuery) "Show All Matches" else "Rescan Contacts",
                                    onAction = {
                                        if (hasFilterOrQuery) {
                                            searchQuery = ""
                                            selectedFilter = FindFriendsFilter.ALL
                                        } else {
                                            triggerScanOrPermission()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp)
                                )
                            }
                        } else {
                            items(filteredEntries, key = { it.user.userId }) { entry ->
                                val incomingReq = incomingBySender[entry.user.userId]
                                val relation = when {
                                    friendIds.contains(entry.user.userId) -> DirectoryRelation.FRIEND
                                    incomingReq != null -> DirectoryRelation.INCOMING_REQUEST
                                    outgoingPendingIds.contains(entry.user.userId) -> DirectoryRelation.OUTGOING_PENDING
                                    else -> DirectoryRelation.NONE
                                }
                                FindFriendsRowItem(
                                    entry = entry,
                                    relation = relation,
                                    onAddFriend = { onSendFriendRequest(entry.user) },
                                    onAcceptIncoming = {
                                        incomingReq?.let { onRespondToFriendRequest(it, true) }
                                    },
                                    onCancelOrUnfriend = { onCancelOrRemoveFriend(entry.user) },
                                    onMessage = { onDirectMessageUser(entry.user) }
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(start = 66.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FindFriendsRowItem(
    entry: FindFriendsEntry,
    relation: DirectoryRelation,
    onAddFriend: () -> Unit,
    onAcceptIncoming: () -> Unit,
    onCancelOrUnfriend: () -> Unit,
    onMessage: () -> Unit
) {
    val user = entry.user
    val availability = user.resolvedAvailability()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMessage() }
            .padding(vertical = 10.dp, horizontal = 2.dp)
            .testTag("find_friends_item_${user.userId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            name = user.username,
            avatarUrl = user.avatarUrl ?: entry.devicePhotoUri,
            isOnline = user.isOnline,
            availabilityStatus = availability,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                AvailabilityStatusPill(
                    status = availability,
                    lastSeen = user.lastSeen,
                    showOfflineLastSeen = false
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

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

            Spacer(modifier = Modifier.height(4.dp))

            // Device Contact Correlation Pill
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = SatOnlineGreen.copy(alpha = 0.13f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = if (entry.matchedVia == "Email") Icons.Default.Email else Icons.Default.Phone,
                        contentDescription = null,
                        tint = SatOnlineGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "In contacts as \"${entry.deviceContactName}\" • ${entry.matchedVia}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SatOnlineGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        when (relation) {
            DirectoryRelation.FRIEND -> {
                OutlinedButton(
                    onClick = onCancelOrUnfriend,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("find_friends_friend_badge_${user.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Friends",
                        tint = SatOnlineGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Friends", fontSize = 12.sp, color = SatOnlineGreen, fontWeight = FontWeight.Bold)
                }
            }

            DirectoryRelation.INCOMING_REQUEST -> {
                Button(
                    onClick = onAcceptIncoming,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SatOnlineGreen),
                    modifier = Modifier.testTag("find_friends_accept_button_${user.userId}")
                ) {
                    Text("Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            DirectoryRelation.OUTGOING_PENDING -> {
                OutlinedButton(
                    onClick = onCancelOrUnfriend,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("find_friends_pending_button_${user.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Pending",
                        tint = SatWarningAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pending", fontSize = 12.sp, color = SatWarningAmber)
                }
            }

            DirectoryRelation.NONE -> {
                Button(
                    onClick = onAddFriend,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SatElectricBlue),
                    modifier = Modifier.testTag("find_friends_add_button_${user.userId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        IconButton(
            onClick = onMessage,
            modifier = Modifier
                .size(48.dp)
                .testTag("find_friends_message_button_${user.userId}")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = "Message ${user.username}",
                tint = SatElectricBlue
            )
        }
    }
}
