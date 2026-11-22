package com.example.ui.screens.chats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AvailabilityStatus
import com.example.data.model.Channel
import com.example.data.model.Chat
import com.example.data.model.Community
import com.example.data.model.ContactEntry
import com.example.data.model.StatusItem
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.UserAvatar
import com.example.ui.screens.channels.ChannelsScreen
import com.example.ui.screens.communities.CommunitiesScreen
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.status.StatusScreen
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainNavTab {
    CHATS,
    UPDATES,
    COMMUNITIES,
    CONTACTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    currentUserId: String,
    userProfile: UserProfile?,
    chats: List<Chat>,
    directoryUsers: List<UserProfile>,
    savedContacts: List<ContactEntry> = emptyList(),
    statuses: List<StatusItem>,
    channels: List<Channel>,
    communities: List<Community>,
    unreadCounts: Map<String, Int> = emptyMap(),
    totalUnreadCount: Int = 0,
    onChatClick: (Chat) -> Unit,
    onDeleteChat: ((String) -> Unit)? = null,
    onClearChat: ((String) -> Unit)? = null,
    onProfileClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onManualClick: () -> Unit,
    onPostStatus: (text: String?, mediaUrl: String?, backgroundColor: String?) -> Unit,
    onDeleteStatus: (String) -> Unit,
    onMarkStatusViewed: (String) -> Unit,
    onReplyToStatus: (authorId: String, text: String) -> Unit,
    onToggleFollowChannel: (Channel) -> Unit,
    onCreateChannel: (name: String, desc: String?, iconUrl: String?, category: String, isPublic: Boolean, allowFollowerPosts: Boolean, allowReactions: Boolean) -> Unit,
    onOpenChannel: (Channel) -> Unit,
    onShareChannelInvite: (Channel) -> Unit = {},
    onCreateCommunity: (name: String, desc: String?, subgroups: List<String>) -> Unit,
    onJoinCommunity: (String) -> Unit,
    onShareCommunityInvite: (Community) -> Unit = {},
    onOpenJoinInvite: () -> Unit = {},
    onOpenUserSearch: () -> Unit,
    onDirectMessageUser: (UserProfile) -> Unit,
    onUpdateAvailability: (AvailabilityStatus) -> Unit = {},
    onToggleFriendContact: (UserProfile) -> Unit = {},
    onOpenSatAi: () -> Unit = {},
    onToggleCommunityAdmin: (communityId: String, targetUserId: String, makeAdmin: Boolean) -> Unit = { _, _, _ -> },
    showNotificationPermissionBanner: Boolean = false,
    onEnableNotificationsClick: () -> Unit = {},
    onDismissNotificationBanner: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedNavTab by remember { mutableStateOf(MainNavTab.CHATS) }
    var updatesSubTab by remember { mutableIntStateOf(0) } // 0: Status, 1: Channels
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Direct", "Group Rooms"

    val activeStatusUserIds = remember(statuses) {
        statuses.map { it.userId }.toSet()
    }

    val filteredChats = remember(chats, searchQuery, directoryUsers, selectedFilter) {
        chats.filter { chat ->
            val matchesFilter = when (selectedFilter) {
                "Direct" -> chat.type != "group"
                "Group Rooms" -> chat.type == "group"
                else -> true
            }
            if (!matchesFilter) return@filter false

            val query = searchQuery.trim()
            if (query.isBlank()) {
                true
            } else {
                val otherMemberId = chat.memberIds.firstOrNull { it != currentUserId }
                val otherUser = directoryUsers.firstOrNull { it.userId == otherMemberId }
                val title = if (chat.type == "group") {
                    chat.name ?: "Group Chat"
                } else {
                    otherUser?.displayName ?: "Direct Message"
                }
                title.contains(query, ignoreCase = true) ||
                        (otherUser?.email?.contains(query, ignoreCase = true) == true) ||
                        (otherUser?.phoneNumber != null && otherUser.phoneNumber.contains(query, ignoreCase = true)) ||
                        (chat.lastMessageText?.contains(query, ignoreCase = true) == true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive && selectedNavTab == MainNavTab.CHATS) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by name, email, phone...") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input")
                        )
                    } else {
                        Column {
                            Text(
                                text = when (selectedNavTab) {
                                    MainNavTab.CHATS -> stringResource(R.string.app_name)
                                    MainNavTab.UPDATES -> "Updates"
                                    MainNavTab.COMMUNITIES -> "Communities"
                                    MainNavTab.CONTACTS -> stringResource(R.string.contacts_title)
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SAT LIMITED ENTERPRISE",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    if (selectedNavTab == MainNavTab.CHATS) {
                        IconButton(
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            },
                            modifier = Modifier.testTag("toggle_search_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    }

                    // SAT Limited AI button
                    IconButton(
                        onClick = onOpenSatAi,
                        modifier = Modifier.testTag("topbar_sat_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Open SAT Limited AI",
                            tint = SatTeal
                        )
                    }

                    // Join via Invite Link button
                    IconButton(
                        onClick = onOpenJoinInvite,
                        modifier = Modifier.testTag("topbar_join_invite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Join via Invite Link",
                            tint = SatElectricBlue
                        )
                    }

                    // Global User Search button
                    IconButton(
                        onClick = onOpenUserSearch,
                        modifier = Modifier.testTag("open_global_user_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = "Search Users by Name, Email, Phone",
                            tint = SatTeal
                        )
                    }

                    IconButton(
                        onClick = onManualClick,
                        modifier = Modifier.testTag("open_manual_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "App Manual & User Guide",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onProfileClick() }
                            .testTag("open_profile_button")
                    ) {
                        UserAvatar(
                            name = userProfile?.displayName ?: "Me",
                            avatarUrl = userProfile?.avatarUrl,
                            isOnline = userProfile?.isOnline ?: true,
                            availabilityStatus = userProfile?.resolvedAvailability(),
                            size = 36.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("whatsapp_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = selectedNavTab == MainNavTab.CHATS,
                    onClick = { selectedNavTab = MainNavTab.CHATS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnreadCount > 0) {
                                    Badge(
                                        containerColor = SatOnlineGreen,
                                        contentColor = Color.White
                                    ) {
                                        Text(if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = "Chats")
                        }
                    },
                    label = { Text("Chats", fontWeight = if (selectedNavTab == MainNavTab.CHATS) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SatElectricBlue,
                        indicatorColor = SatElectricBlue.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_chats")
                )

                NavigationBarItem(
                    selected = selectedNavTab == MainNavTab.UPDATES,
                    onClick = { selectedNavTab = MainNavTab.UPDATES },
                    icon = { Icon(imageVector = Icons.Default.DonutLarge, contentDescription = "Updates") },
                    label = { Text("Updates", fontWeight = if (selectedNavTab == MainNavTab.UPDATES) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SatTeal,
                        indicatorColor = SatTeal.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_updates")
                )

                NavigationBarItem(
                    selected = selectedNavTab == MainNavTab.COMMUNITIES,
                    onClick = { selectedNavTab = MainNavTab.COMMUNITIES },
                    icon = { Icon(imageVector = Icons.Default.Groups, contentDescription = "Communities") },
                    label = { Text("Communities", fontWeight = if (selectedNavTab == MainNavTab.COMMUNITIES) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SatElectricBlue,
                        indicatorColor = SatElectricBlue.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_communities")
                )

                NavigationBarItem(
                    selected = selectedNavTab == MainNavTab.CONTACTS,
                    onClick = { selectedNavTab = MainNavTab.CONTACTS },
                    icon = { Icon(imageVector = Icons.Default.People, contentDescription = stringResource(R.string.contacts_tab_label)) },
                    label = {
                        Text(
                            text = stringResource(R.string.contacts_tab_label),
                            fontWeight = if (selectedNavTab == MainNavTab.CONTACTS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SatTeal,
                        indicatorColor = SatTeal.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("tab_contacts")
                )
            }
        },
        floatingActionButton = {
            if (selectedNavTab == MainNavTab.CHATS) {
                FloatingActionButton(
                    onClick = onNewChatClick,
                    containerColor = SatElectricBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("new_chat_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavTab) {
                MainNavTab.CHATS -> {
                    ChatsTabContent(
                        chats = chats,
                        filteredChats = filteredChats,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        currentUserId = currentUserId,
                        directoryUsers = directoryUsers,
                        activeStatusUserIds = activeStatusUserIds,
                        unreadCounts = unreadCounts,
                        onDeleteChat = onDeleteChat,
                        onClearChat = onClearChat,
                        onFilterChange = { selectedFilter = it },
                        onChatClick = onChatClick,
                        onNewChatClick = onNewChatClick,
                        onOpenSatAi = onOpenSatAi,
                        showNotificationPermissionBanner = showNotificationPermissionBanner,
                        onEnableNotificationsClick = onEnableNotificationsClick,
                        onDismissNotificationBanner = onDismissNotificationBanner
                    )
                }

                MainNavTab.UPDATES -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(selectedTabIndex = updatesSubTab) {
                            Tab(
                                selected = updatesSubTab == 0,
                                onClick = { updatesSubTab = 0 },
                                text = { Text("Status (${statuses.size})") }
                            )
                            Tab(
                                selected = updatesSubTab == 1,
                                onClick = { updatesSubTab = 1 },
                                text = { Text("Channels (${channels.size})") }
                            )
                        }

                        if (updatesSubTab == 0) {
                            StatusScreen(
                                currentUserId = currentUserId,
                                currentUserProfile = userProfile,
                                statuses = statuses,
                                onPostStatus = onPostStatus,
                                onDeleteStatus = onDeleteStatus,
                                onMarkStatusViewed = onMarkStatusViewed,
                                onReplyToStatus = onReplyToStatus,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ChannelsScreen(
                                currentUserId = currentUserId,
                                channels = channels,
                                onToggleFollow = onToggleFollowChannel,
                                onCreateChannel = onCreateChannel,
                                onOpenChannel = onOpenChannel,
                                onShareInvite = onShareChannelInvite,
                                onOpenJoinInvite = onOpenJoinInvite,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                MainNavTab.COMMUNITIES -> {
                    CommunitiesScreen(
                        currentUserId = currentUserId,
                        communities = communities,
                        allChats = chats,
                        directoryUsers = directoryUsers,
                        onOpenChat = onChatClick,
                        onCreateCommunity = onCreateCommunity,
                        onJoinCommunity = onJoinCommunity,
                        onShareCommunityInvite = onShareCommunityInvite,
                        onOpenJoinInvite = onOpenJoinInvite,
                        onToggleCommunityAdmin = onToggleCommunityAdmin,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainNavTab.CONTACTS -> {
                    ContactsScreen(
                        currentUserId = currentUserId,
                        currentUserProfile = userProfile,
                        directoryUsers = directoryUsers,
                        savedContacts = savedContacts,
                        onDirectMessageUser = onDirectMessageUser,
                        onUpdateAvailability = onUpdateAvailability,
                        onToggleFriend = onToggleFriendContact,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatsTabContent(
    chats: List<Chat>,
    filteredChats: List<Chat>,
    searchQuery: String,
    selectedFilter: String,
    currentUserId: String,
    directoryUsers: List<UserProfile>,
    activeStatusUserIds: Set<String>,
    unreadCounts: Map<String, Int> = emptyMap(),
    onDeleteChat: ((String) -> Unit)? = null,
    onClearChat: ((String) -> Unit)? = null,
    onFilterChange: (String) -> Unit,
    onChatClick: (Chat) -> Unit,
    onNewChatClick: () -> Unit,
    onOpenSatAi: () -> Unit = {},
    showNotificationPermissionBanner: Boolean = false,
    onEnableNotificationsClick: () -> Unit = {},
    onDismissNotificationBanner: () -> Unit = {}
) {
    var selectedChatForActions by remember { mutableStateOf<Chat?>(null) }
    var showClearConfirm by remember { mutableStateOf<Chat?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Chat?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showNotificationPermissionBanner) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SatElectricBlue.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("notification_permission_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = SatElectricBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Phone Notifications",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Get incoming message alerts in your phone's Notification Center.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onEnableNotificationsClick,
                        modifier = Modifier.testTag("enable_notifications_banner_button")
                    ) {
                        Text("Allow", fontWeight = FontWeight.Bold, color = SatElectricBlue)
                    }
                    IconButton(onClick = onDismissNotificationBanner) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        val directCount = remember(chats) { chats.count { it.type != "group" } }
        val groupCount = remember(chats) { chats.count { it.type == "group" } }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "All",
                    onClick = { onFilterChange("All") },
                    label = { Text("All (${chats.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SatElectricBlue,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_all")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "Direct",
                    onClick = { onFilterChange("Direct") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("Direct ($directCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SatElectricBlue,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_direct")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "Group Rooms",
                    onClick = { onFilterChange("Group Rooms") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text("Groups ($groupCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SatTeal,
                        selectedLabelColor = Color.Black
                    ),
                    modifier = Modifier.testTag("filter_chip_groups")
                )
            }
        }

        // Pinned SAT Limited AI Assistant Entry Card
        Card(
            onClick = onOpenSatAi,
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = SatElectricBlue.copy(alpha = 0.12f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("pinned_sat_ai_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SatElectricBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "SAT Limited AI",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SAT Limited AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SatTeal.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "Gemini + Search",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SatTeal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Ask AI, analyze photos, or search live web info",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (filteredChats.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = if (searchQuery.isNotBlank()) "No Matching Chats"
                    else if (selectedFilter == "Group Rooms") "No Group Rooms Yet"
                    else "No Conversations Yet",
                    message = if (searchQuery.isNotBlank()) "Try searching by colleague name, email address, or phone number."
                    else if (selectedFilter == "Group Rooms") "Create a departmental group room in 'chatRooms' by tapping '+'."
                    else "Connect with your SAT Limited team members. Tap the '+' button below to start a direct message or create a group room.",
                    actionLabel = if (searchQuery.isBlank()) "Start Conversation" else null,
                    onAction = if (searchQuery.isBlank()) onNewChatClick else null,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredChats, key = { it.id }) { chat ->
                        val isGroup = chat.type == "group"
                        val otherMemberId = chat.memberIds.firstOrNull { it != currentUserId }
                        val otherUser = directoryUsers.firstOrNull { it.userId == otherMemberId }

                        val otherNameOrUsername = when {
                            otherUser != null && otherUser.displayName.isNotBlank() -> otherUser.displayName
                            otherUser != null && otherUser.email.isNotBlank() -> otherUser.email.substringBefore("@")
                            !otherMemberId.isNullOrBlank() && !chat.participantNames[otherMemberId].isNullOrBlank() -> chat.participantNames[otherMemberId]!!
                            chat.lastSenderId == otherMemberId && !chat.lastSenderName.isNullOrBlank() -> chat.lastSenderName
                            !otherMemberId.isNullOrBlank() -> "User_${otherMemberId.take(6)}"
                            else -> "User"
                        }

                        val otherUsernameHandle = if (!isGroup) {
                            otherUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }?.let { "@$it" }
                        } else null

                        val title = if (isGroup) {
                            chat.name?.takeIf { it.isNotBlank() } ?: "Group Room"
                        } else {
                            otherNameOrUsername
                        }

                        val isOtherOnline = otherUser?.isOnline ?: false
                        val timeStr = formatTimestamp(chat.lastTimestamp?.toDate())

                        // Active typing in this chat
                        val isAnyoneTyping = chat.isTyping.any { (uid, typing) -> uid != currentUserId && typing }

                        // WhatsApp Status Ring Indicator on Avatar
                        val hasActiveStatus = otherMemberId != null && activeStatusUserIds.contains(otherMemberId)

                        val unread = unreadCounts[chat.id] ?: 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onChatClick(chat) },
                                    onLongClick = { selectedChatForActions = chat }
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("chat_item_${chat.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                name = title,
                                avatarUrl = otherUser?.avatarUrl,
                                isOnline = if (isGroup) false else isOtherOnline,
                                availabilityStatus = if (isGroup) null else otherUser?.resolvedAvailability(),
                                isGroup = isGroup,
                                hasStatus = hasActiveStatus,
                                statusRingColor = SatOnlineGreen,
                                size = 52.dp
                            )

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (otherUsernameHandle != null && otherUsernameHandle != "@$title") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = otherUsernameHandle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SatTeal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    if (isGroup) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SatTeal.copy(alpha = 0.18f),
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                text = "${chat.memberIds.size} members",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SatTeal,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (timeStr.isNotBlank()) {
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (unread > 0) SatOnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isAnyoneTyping) {
                                        Text(
                                            text = "Typing...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SatTeal,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        val prefix = if (isGroup && !chat.lastSenderName.isNullOrBlank()) {
                                            "${chat.lastSenderName}: "
                                        } else ""
                                        Text(
                                            text = prefix + (chat.lastMessageText ?: "No messages yet"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (unread > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    if (unread > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = SatOnlineGreen,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .testTag("chat_unread_badge_${chat.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (unread > 99) "99+" else unread.toString(),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 82.dp)
                        )
                    }
                }
            }
        }
    }

    if (selectedChatForActions != null) {
        val targetChat = selectedChatForActions!!
        val isGroupTarget = targetChat.type == "group"
        val otherMemberId = targetChat.memberIds.firstOrNull { it != currentUserId }
        val otherUser = directoryUsers.firstOrNull { it.userId == otherMemberId }
        val chatName = if (isGroupTarget) targetChat.name ?: "Group Room" else otherUser?.displayName ?: "Chat"

        AlertDialog(
            onDismissRequest = { selectedChatForActions = null },
            title = { Text(chatName) },
            text = {
                Column {
                    Text("Select an action for this conversation:")
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = {
                            selectedChatForActions = null
                            onChatClick(targetChat)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("action_open_chat")
                    ) {
                        Text("Open Conversation", modifier = Modifier.fillMaxWidth())
                    }
                    if (onClearChat != null) {
                        TextButton(
                            onClick = {
                                val c = targetChat
                                selectedChatForActions = null
                                showClearConfirm = c
                            },
                            modifier = Modifier.fillMaxWidth().testTag("action_clear_chat")
                        ) {
                            Text("Clear Chat History", color = SatErrorRed, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (onDeleteChat != null) {
                        TextButton(
                            onClick = {
                                val c = targetChat
                                selectedChatForActions = null
                                showDeleteConfirm = c
                            },
                            modifier = Modifier.fillMaxWidth().testTag("action_delete_chat")
                        ) {
                            Text(if (isGroupTarget) "Delete Group Room" else "Delete Chat", color = SatErrorRed, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedChatForActions = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showClearConfirm != null) {
        val chatToClear = showClearConfirm!!
        AlertDialog(
            onDismissRequest = { showClearConfirm = null },
            title = { Text("Clear Chat History?") },
            text = { Text("Are you sure you want to clear all messages in this conversation? Messages will be deleted for all participants.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cid = chatToClear.id
                        showClearConfirm = null
                        onClearChat?.invoke(cid)
                    }
                ) {
                    Text("Clear", color = SatErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm != null) {
        val chatToDelete = showDeleteConfirm!!
        val isGroupDel = chatToDelete.type == "group"
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text(if (isGroupDel) "Delete Group Room?" else "Delete Chat?") },
            text = { Text("Are you sure you want to delete this ${if (isGroupDel) "group room" else "chat"}? All messages and records will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cid = chatToDelete.id
                        showDeleteConfirm = null
                        onDeleteChat?.invoke(cid)
                    }
                ) {
                    Text("Delete", color = SatErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatTimestamp(date: Date?): String {
    if (date == null) return ""
    val now = Date()
    val diffMs = now.time - date.time
    val oneDayMs = 24 * 60 * 60 * 1000L
    return when {
        diffMs < oneDayMs -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        diffMs < 7 * oneDayMs -> SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}
