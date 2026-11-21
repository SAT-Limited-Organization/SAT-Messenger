package com.example.ui.screens.contacts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
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
import com.example.data.model.AvailabilityStatus
import com.example.data.model.ContactEntry
import com.example.data.model.UserProfile
import com.example.ui.components.AvailabilityStatusPill
import com.example.ui.components.EmptyStateView
import com.example.ui.components.UserAvatar
import com.example.ui.components.availabilityColor
import com.example.ui.components.formatLastSeenRelative
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import com.example.ui.theme.SatWarningAmber

private enum class ContactFilter(val label: String) {
    ALL("All"),
    ONLINE("Online"),
    FRIENDS("Friends"),
    COLLEAGUES("Colleagues")
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ContactFilter.ALL) }
    var inspectedContact by remember { mutableStateOf<UserProfile?>(null) }

    val friendIds = remember(savedContacts) {
        savedContacts
            .filter { it.relationship == "friend" }
            .map { it.contactUserId }
            .toSet()
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

    val colleaguesCount = remember(otherUsers, friendIds) {
        otherUsers.count { !friendIds.contains(it.userId) }
    }

    val filteredContacts = remember(otherUsers, friendIds, searchQuery, selectedFilter) {
        val query = searchQuery.trim()
        otherUsers
            .filter { user ->
                val isFriend = friendIds.contains(user.userId)
                val matchesFilter = when (selectedFilter) {
                    ContactFilter.ALL -> true
                    ContactFilter.ONLINE -> user.isOnline && user.resolvedAvailability() != AvailabilityStatus.OFFLINE
                    ContactFilter.FRIENDS -> isFriend
                    ContactFilter.COLLEAGUES -> !isFriend
                }
                if (!matchesFilter) return@filter false

                if (query.isBlank()) {
                    true
                } else {
                    user.displayName.contains(query, ignoreCase = true) ||
                        user.usernameHandle.contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true) ||
                        (user.phoneNumber?.contains(query, ignoreCase = true) == true) ||
                        (user.statusMessage?.contains(query, ignoreCase = true) == true) ||
                        user.resolvedAvailability().label.contains(query, ignoreCase = true)
                }
            }
            .sortedWith(
                compareByDescending<UserProfile> { it.isOnline }
                    .thenBy { availabilitySortPriority(it.resolvedAvailability()) }
                    .thenByDescending { friendIds.contains(it.userId) }
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

        Spacer(modifier = Modifier.height(10.dp))

        // Search Contacts Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search friends & colleagues by name, email, status...") },
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

        // Filter Chips: All / Online / Friends / Colleagues
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

        // Synced Contacts List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (filteredContacts.isEmpty()) {
                val isFiltered = searchQuery.isNotBlank() || selectedFilter != ContactFilter.ALL
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = when {
                        searchQuery.isNotBlank() -> "No Contacts Match \"$searchQuery\""
                        selectedFilter == ContactFilter.FRIENDS -> "No Friends Starred Yet"
                        selectedFilter == ContactFilter.ONLINE -> "No Contacts Online Right Now"
                        else -> "No Synced Contacts Yet"
                    },
                    message = when {
                        searchQuery.isNotBlank() -> "Try searching by display name, @handle, corporate email, or availability status."
                        selectedFilter == ContactFilter.FRIENDS -> "Tap the star icon next to any colleague or friend to keep them pinned in your Friends list."
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
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("contacts_lazy_column"),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredContacts, key = { it.userId }) { user ->
                        val isFriend = friendIds.contains(user.userId)
                        ContactRowItem(
                            user = user,
                            isFriend = isFriend,
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
    }

    inspectedContact?.let { contact ->
        val liveUser = directoryUsers.firstOrNull { it.userId == contact.userId } ?: contact
        val isFriend = friendIds.contains(liveUser.userId)
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
                            text = liveUser.usernameHandle,
                            style = MaterialTheme.typography.labelMedium,
                            color = SatTeal
                        )
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
                            color = if (isFriend) SatWarningAmber.copy(alpha = 0.18f) else SatElectricBlue.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = if (isFriend) "Friend" else "Colleague",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isFriend) SatWarningAmber else SatElectricBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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

                    OutlinedButton(
                        onClick = { onToggleFriend(liveUser) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_toggle_friend_button")
                    ) {
                        Icon(
                            imageVector = if (isFriend) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (isFriend) SatWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isFriend) "Remove from Friends" else "Add to Friends")
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
    isFriend: Boolean,
    onInspect: () -> Unit,
    onToggleFriend: () -> Unit,
    onMessage: () -> Unit
) {
    val availability = user.resolvedAvailability()

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
                    color = if (isFriend) SatWarningAmber.copy(alpha = 0.16f) else SatTeal.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isFriend) "Friend" else "Colleague",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFriend) SatWarningAmber else SatTeal,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                imageVector = if (isFriend) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = if (isFriend) "Remove from friends" else "Add to friends",
                tint = if (isFriend) SatWarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
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
