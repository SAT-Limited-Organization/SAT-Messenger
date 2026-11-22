package com.example.ui.screens.manual

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatNavyDark
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal

data class ManualSection(
    val id: String,
    val category: String,
    val title: String,
    val icon: ImageVector,
    val summary: String,
    val steps: List<String>,
    val tips: List<String> = emptyList(),
    val badge: String? = null
)

private val MANUAL_SECTIONS = listOf(
    ManualSection(
        id = "sign_in",
        category = "Getting Started",
        title = "Registration & Sign-In (Google & Microsoft)",
        icon = Icons.Default.Lock,
        badge = "Zero-Trust",
        summary = "Register or sign in to SAT Messenger using either your Google or Microsoft account.",
        steps = listOf(
            "On the Sign-In / Registration screen, choose 'Continue with Google' or 'Continue with Microsoft'.",
            "Microsoft Sign-In uses OpenID Connect scopes (openid, profile, email, User.Read) and supports organizational & personal accounts via the configured tenant ('common' by default).",
            "Complete authentication; your profile is automatically created and synced to Cloud Firestore on first sign-in.",
            "You can also link a Microsoft account later from Profile > Sign-In Methods or tap 'Microsoft Sign-In Setup Guide' for the Firebase redirect URI.",
            "Need help before signing in? Tap 'App Manual & User Guide' directly on the Registration / Sign-In screen at any time."
        ),
        tips = listOf(
            "Your display name, @username handle, avatar, and real-time availability sync immediately upon registration.",
            "Workspace admins enabling Microsoft OAuth in Firebase Console use redirect URI https://sat-limited.firebaseapp.com/__/auth/handler.",
            "Anonymous sign-in is disabled to preserve zero-trust security across SAT Limited."
        )
    ),
    ManualSection(
        id = "contacts_and_availability",
        category = "Contacts & Availability",
        title = "Contacts Sync & Real-Time Availability",
        icon = Icons.Default.People,
        badge = "Live Sync",
        summary = "View all Firestore-synced friends and colleagues with real-time online status and availability badges.",
        steps = listOf(
            "Tap the 'Contacts' tab on the bottom navigation bar to open your live contact directory.",
            "Set your own real-time availability at the top ('Available', 'Busy', 'Away', 'In a Meeting', or 'Do Not Disturb') — changes sync instantly to other users.",
            "Filter contacts by All, Online, Friends, or Colleagues, or search by name, @handle, email, phone, or availability.",
            "Tap the Star icon next to any contact to add or remove them from your personal Friends list.",
            "Tap any contact row to inspect their live availability, Last Seen timestamp, email, or phone number, or tap Message to open a chat."
        ),
        tips = listOf(
            "Green = Available, Blue = In a Meeting, Amber = Away, Red = Busy / Do Not Disturb, Gray = Offline with relative Last Seen time."
        )
    ),
    ManualSection(
        id = "direct_chats",
        category = "Direct Chats",
        title = "1-on-1 Messaging & Emoji Reactions",
        icon = Icons.Default.Person,
        badge = "Direct",
        summary = "Communicate privately in real time with colleagues and friends using replies and emoji reactions.",
        steps = listOf(
            "Tap the '+' (New Message) floating button or pick someone from the Contacts tab to start a 1-on-1 chat.",
            "Chats display each contact's real name or @username handle rather than generic placeholders.",
            "Long-press any message bubble to add an emoji reaction (👍, ❤️, 😂, 😮, 😢, 🙏, 🔥, 🎉, 👏, 🚀) or tap '+' for the extended emoji grid.",
            "Long-press a message to Reply with a quoted snippet, Copy text, or Delete for everyone."
        ),
        tips = listOf(
            "Tap any reaction pill under a message to toggle your own reaction or view everyone who reacted.",
            "Use the top carousel inside a conversation to switch quickly between contacts."
        )
    ),
    ManualSection(
        id = "voice_messaging",
        category = "Voice Clips",
        title = "Voice Recording & In-Chat Audio Playback",
        icon = Icons.Default.Mic,
        badge = "Audio",
        summary = "Record voice notes directly inside any chat using the device microphone and play them back inline.",
        steps = listOf(
            "When the message box is empty, tap the Microphone icon on the right (or choose 'Record Voice Clip' from the paperclip menu).",
            "Grant the Microphone permission when prompted so SAT Messenger can capture audio.",
            "Watch the live elapsed timer and amplitude bars while recording (up to 90 seconds).",
            "Tap Stop to listen to a preview before sending, Discard to cancel, or Send immediately.",
            "Tap Play/Pause inside any voice message bubble and drag the slider to scrub through the clip."
        ),
        tips = listOf(
            "Typing text in the composer automatically switches the Microphone button into the Send button."
        )
    ),
    ManualSection(
        id = "group_rooms",
        category = "Group Rooms",
        title = "Group Chat Rooms",
        icon = Icons.Default.Group,
        badge = "Collaborative",
        summary = "Create and manage departmental group rooms with shared participant lists and invite links.",
        steps = listOf(
            "Tap the '+' button on the Chats screen and switch to the 'New Group' tab.",
            "Enter a room name, optional charter/description, and select colleagues or friends to invite.",
            "Tap 'Create Group' to launch the room in Cloud Firestore.",
            "Tap the room header or Participants icon inside the chat to view all members and their live availability."
        ),
        tips = listOf(
            "Incoming group messages show the sender's name and handle above each bubble for clarity."
        )
    ),
    ManualSection(
        id = "invite_links",
        category = "Invite Links",
        title = "Channel, Group & Community Invite Links",
        icon = Icons.Default.Link,
        badge = "Shareable",
        summary = "Generate, share, revoke, and redeem invite links for Group Rooms, Channels, and Communities.",
        steps = listOf(
            "Open any Group Room, Channel, or Community and tap its Share Invite Link action.",
            "Copy the invite code or share URL (e.g. satmessenger.app/invite/...) directly to colleagues.",
            "Admins can tap 'Reset Link' at any time to revoke the old code and issue a fresh one.",
            "To join via a link, tap the Link icon in the Chats top bar, paste the code or URL, preview the destination, and tap Join."
        ),
        tips = listOf(
            "Deep links (satmessenger://invite/...) automatically open the Join Invite dialog once signed in."
        )
    ),
    ManualSection(
        id = "broadcast_channels",
        category = "Channels",
        title = "Flexible Broadcast Channels",
        icon = Icons.Default.Campaign,
        badge = "Flexible",
        summary = "Create public or invite-only channels with customizable posting and reaction permissions.",
        steps = listOf(
            "Open the 'Updates' tab and select 'Channels'.",
            "Tap the megaphone FAB to create a channel with a custom Category (e.g. Engineering, Operations, Alerts).",
            "Configure flexibility toggles: Public vs Private (invite-only), Allow Follower Posts, and Allow Reactions.",
            "Channel admins can update channel settings or delete the channel anytime from the channel header."
        ),
        tips = listOf(
            "Follower counts and broadcast posts sync live across the organization in Cloud Firestore."
        )
    ),
    ManualSection(
        id = "whatsapp_communities",
        category = "Communities",
        title = "Communities & Subgroup Hubs",
        icon = Icons.Default.Groups,
        badge = "Enterprise",
        summary = "Organize related group rooms and announcement hubs under one umbrella community.",
        steps = listOf(
            "Tap the 'Communities' tab on the bottom navigation bar.",
            "Tap 'Start a Community' to create a division hub with subgroup rooms.",
            "Share a Community invite link so members can discover and join linked subgroups."
        ),
        tips = listOf(
            "Every community features an '📢 Announcements' room for team-wide notices."
        )
    ),
    ManualSection(
        id = "whatsapp_status",
        category = "Status & Updates",
        title = "24-Hour Status Updates",
        icon = Icons.Default.DonutLarge,
        badge = "Stories",
        summary = "Share text or photo status updates that automatically expire after 24 hours.",
        steps = listOf(
            "Open the 'Updates' tab and stay on 'Status'.",
            "Tap '+' under My Status to pick a background color, write a thought, or attach media.",
            "Contacts with active statuses show a green ring around their avatar in Chats."
        ),
        tips = listOf(
            "Reply directly to any contact's status from the story viewer reply bar."
        )
    ),
    ManualSection(
        id = "media_sharing",
        category = "Media & Files",
        title = "Photos, Media & Document Sharing",
        icon = Icons.Default.AttachFile,
        badge = "Zero-Permission",
        summary = "Share photos and files securely using Android's zero-permission Photo Picker.",
        steps = listOf(
            "Tap the paperclip icon on the left side of any chat composer.",
            "Select 'Photo & Media' or 'Document & Files', review the preview, add an optional caption, and tap Send.",
            "Tap any image bubble in a chat to open the full-screen viewer, or view all media in Contact / Group Info."
        ),
        tips = listOf(
            "Photos are compressed automatically for fast, reliable delivery."
        )
    ),
    ManualSection(
        id = "realtime_typing",
        category = "Real-Time & Receipts",
        title = "Live Presence & Typing Indicators",
        icon = Icons.Default.Sync,
        badge = "Instant",
        summary = "Real-time active indicators and animated typing bubbles powered by Firestore listeners.",
        steps = listOf(
            "Presence automatically syncs when you open, background, or change availability in SAT Messenger.",
            "When someone types in a chat, the header displays who is typing alongside an animated 3-dot bubble."
        ),
        tips = listOf(
            "Group rooms display multiple concurrent typers cleanly in the subtitle."
        )
    ),
    ManualSection(
        id = "read_receipts",
        category = "Real-Time & Receipts",
        title = "Read Receipts & Seen Tracking",
        icon = Icons.Default.DoneAll,
        badge = "Verified",
        summary = "Track sent, delivered, and read status ticks across direct and group conversations.",
        steps = listOf(
            "Single check = sent to Firestore; double check = delivered; teal double check = read.",
            "Long-press your outgoing message to see how many participants have read it."
        ),
        tips = listOf(
            "Disable Read Receipts anytime in Profile > Privacy Controls."
        )
    ),
    ManualSection(
        id = "system_notifications",
        category = "Notifications & Badges",
        title = "Phone Notification Center & Alerts",
        icon = Icons.Default.NotificationsActive,
        badge = "Alerts",
        summary = "Receive incoming message alerts in your phone's Notification Center alongside unread badges.",
        steps = listOf(
            "Allow notification permission when prompted (or enable it from the Chats banner or Profile > Notification Center).",
            "Incoming messages post to your phone Notification Center when the chat isn't open, and tapping a notification opens that conversation directly.",
            "Use 'Send Test Notification' in Profile to verify Notification Center delivery, or toggle system notifications on/off anytime."
        ),
        tips = listOf(
            "Unread counts appear on each chat row and as a green badge on the Chats navigation tab."
        )
    ),
    ManualSection(
        id = "search_and_contact_details",
        category = "Search & Details",
        title = "Global Search & Contact Info",
        icon = Icons.Default.Phone,
        badge = "Directory",
        summary = "Find people by name, @username, email, or phone number and inspect rich Contact Info.",
        steps = listOf(
            "Tap the person-search icon in the Chats top bar for quick global lookup, or use the Contacts tab.",
            "Inside any chat, tap the top bar title to open Contact / Group Info with copyable email/phone, shared media, and chat management."
        ),
        tips = listOf(
            "Keep your phone number updated in Profile > Profile Details so contacts can reach you quickly."
        )
    ),
    ManualSection(
        id = "chat_management_and_badges",
        category = "Chat Management",
        title = "Clearing History & Deleting Chats",
        icon = Icons.Default.DeleteSweep,
        badge = "Controls",
        summary = "Long-press conversations to clear message history or delete chats cleanly.",
        steps = listOf(
            "Long-press any chat card in the Chats tab (or open the ⋮ menu inside a chat) to Clear Chat History or Delete Chat.",
            "Confirmation dialogs protect against accidental deletion."
        ),
        tips = listOf(
            "Clearing history removes messages while keeping the conversation thread active."
        )
    ),
    ManualSection(
        id = "privacy_security",
        category = "Privacy & Safety",
        title = "Privacy, Blocking & Compliance",
        icon = Icons.Default.Security,
        badge = "Compliance",
        summary = "Control Read Receipts, Last Seen presence, blocked contacts, compliance reports, and account deletion.",
        steps = listOf(
            "Go to Profile > Privacy Controls to toggle Read Receipts and Online & Last Seen Presence.",
            "Inside a 1-on-1 chat menu (⋮), tap Block Contact or Report to SAT Compliance.",
            "Manage blocked users or permanently delete your account from the Profile screen."
        ),
        tips = listOf(
            "Compliance reports are logged securely to a restricted Firestore collection."
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppManualScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf(
        "All",
        "Getting Started",
        "Contacts & Availability",
        "Direct Chats",
        "Voice Clips",
        "Group Rooms",
        "Invite Links",
        "Channels",
        "Communities",
        "Status & Updates",
        "Media & Files",
        "Notifications & Badges",
        "Search & Details",
        "Chat Management",
        "Real-Time & Receipts",
        "Privacy & Safety"
    )

    val filteredSections = remember(searchQuery, selectedCategory) {
        MANUAL_SECTIONS.filter { section ->
            val matchesCategory = selectedCategory == "All" || section.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                section.title.contains(searchQuery, ignoreCase = true) ||
                section.summary.contains(searchQuery, ignoreCase = true) ||
                section.steps.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("manual_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(SatElectricBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Help,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SAT Messenger Manual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Official User Guide & Reference",
                                style = MaterialTheme.typography.labelSmall,
                                color = SatTeal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                // Header Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SatNavyDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(SatOnlineGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SAT LIMITED ENTERPRISE PLATFORM",
                                style = MaterialTheme.typography.labelSmall,
                                color = SatTeal,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "How to Use SAT Messenger",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Everything you need to know about Google & Microsoft sign-in, live contact availability, direct & group chats, voice clips, invite links, flexible channels, phone notifications, and privacy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search instructions, features, or tips...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SatElectricBlue
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_search_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SatElectricBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            if (filteredSections.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No manual topics match \"$searchQuery\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Try searching for 'group', 'receipts', 'sign-in', or select 'All'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredSections, key = { it.id }) { section ->
                    ManualSectionCard(section = section)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ManualSectionCard(section: ManualSection) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
            .testTag("manual_card_${section.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SatElectricBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = section.icon,
                        contentDescription = null,
                        tint = SatElectricBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        section.badge?.let { badgeText ->
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SatTeal.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = badgeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SatTeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = section.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Step-by-Step Instructions:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    section.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(SatElectricBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (section.tips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SatTeal.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SatTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Enterprise Tip:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SatTeal
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                section.tips.forEach { tip ->
                                    Text(
                                        text = "• $tip",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
