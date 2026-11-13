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
import androidx.compose.material.icons.filled.Block
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
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
        title = "Enterprise Authentication & Sign-In",
        icon = Icons.Default.Lock,
        badge = "Zero-Trust",
        summary = "Access SAT Messenger with secure Google Sign-In tied to your enterprise email.",
        steps = listOf(
            "Tap 'Sign in with Google' on the welcome screen.",
            "Choose your SAT Limited corporate account in the bottom sheet prompt.",
            "Upon first sign-in, your employee profile is automatically initialized with your name and avatar.",
            "Your authenticated session persists securely across app relaunches with Google Credential Manager."
        ),
        tips = listOf(
            "Anonymous sign-in is disabled to maintain zero-trust corporate security standards.",
            "You can update your corporate display name and status message anytime from the Profile tab."
        )
    ),
    ManualSection(
        id = "direct_chats",
        category = "Direct Chats",
        title = "1-on-1 Colleague Messaging",
        icon = Icons.Default.Person,
        badge = "Direct",
        summary = "Communicate privately in real time with colleagues across SAT departments.",
        steps = listOf(
            "Tap the '+' (New Message) floating button on the bottom right.",
            "Browse or search the registered employee directory under the 'Direct Message' tab.",
            "Tap on a colleague's name to open your private direct channel.",
            "Type your message into the input field and tap the Send button.",
            "Long-press any message bubble to react with emojis (👍, ❤️, 😂, 😮, 😢, 🙏, 🔥, 🎉, 👏, 🚀) or tap '+' for extended emojis.",
            "Long-press any message bubble to Reply with a quote, Copy the text, or Delete for everyone."
        ),
        tips = listOf(
            "Tap any reaction chip on a message to quickly toggle your reaction or view who reacted.",
            "Use the top colleague carousel inside the chat to switch conversations with one tap.",
            "Tap the paperclip icon to send project photos or official SAT documents."
        )
    ),
    ManualSection(
        id = "group_rooms",
        category = "Group Rooms",
        title = "Group Chat Rooms (`chatRooms`)",
        icon = Icons.Default.Group,
        badge = "Collaborative",
        summary = "Create and manage departmental chat rooms with multiple participants using 'memberIds'.",
        steps = listOf(
            "Tap the '+' button and select the 'New Group' tab.",
            "Enter a descriptive room name (e.g., 'SAT Operations', 'Payload Engineering').",
            "Optionally add a room charter / topic to clarify the room's objective.",
            "Select colleagues by checking the box next to their names in the directory.",
            "Tap 'Create Group' — your group room is saved to the Firestore 'chatRooms' collection with all selected memberIds.",
            "Inside the room, tap the 'Participants' button in the top bar to inspect all active members and their online status."
        ),
        tips = listOf(
            "Every incoming group message displays the sender's name in Cyber Teal for effortless identification.",
            "Room administrators can manage discussions, and all members receive real-time notifications."
        )
    ),
    ManualSection(
        id = "realtime_typing",
        category = "Real-Time & Receipts",
        title = "Live Presence & Typing Indicators",
        icon = Icons.Default.Sync,
        badge = "Instant",
        summary = "Real-time presence tracking and typing status powered by Firestore snapshot streams.",
        steps = listOf(
            "Look for the green dot next to colleague avatars indicating they are 'Active Now'.",
            "When a colleague begins composing in the input field, the 'isTyping' field updates in the chat document.",
            "A typing subtitle (e.g. 'Alice is typing...') appears in the top bar.",
            "A typing card featuring 3 animated bouncing dots displays at the bottom of the conversation.",
            "Typing status automatically clears after 2.5 seconds of inactivity or when the message is sent."
        ),
        tips = listOf(
            "In group chat rooms, multiple typing colleagues are reported (e.g. 'Alice and Bob are typing...')."
        )
    ),
    ManualSection(
        id = "read_receipts",
        category = "Real-Time & Receipts",
        title = "Read Receipts & 'readBy' Map",
        icon = Icons.Default.DoneAll,
        badge = "Verified",
        summary = "Track exactly who has received and viewed your messages in both 1-on-1 and group rooms.",
        steps = listOf(
            "Single Check: The message has been sent to Cloud Firestore.",
            "Double Check (White): The message has been delivered to members.",
            "Double Check (Cyber Teal): The message has been read by participants.",
            "When viewing a chat, opening the conversation automatically marks incoming messages as read by updating your entry in the 'readBy' map.",
            "Long-press your message to view the exact read receipt summary (e.g. 'Seen by 3 colleagues')."
        ),
        tips = listOf(
            "You can disable sending read receipts in your Profile > Privacy Settings if you prefer silent viewing."
        )
    ),
    ManualSection(
        id = "privacy_security",
        category = "Privacy & Safety",
        title = "Privacy, Blocking & Compliance",
        icon = Icons.Default.Security,
        badge = "Compliance",
        summary = "Manage corporate compliance, contact blocking, and visibility controls.",
        steps = listOf(
            "Privacy Toggles: Go to Profile to toggle 'Read Receipts' and 'Last Seen' presence.",
            "Blocking Contacts: Inside any 1-on-1 chat, tap the top menu (⋮) and select 'Block Contact'. Blocked users cannot message you.",
            "Unblocking Contacts: Navigate to Profile > Blocked Contacts to review and unblock colleagues.",
            "Reporting Incidents: Tap 'Report to SAT Compliance' in the chat menu to submit an official violation report.",
            "Account Deletion: Permanent account deletion is available at the bottom of the Profile page."
        ),
        tips = listOf(
            "All compliance reports are stored in a restricted, auditable Firestore collection for the SAT Security Operations team."
        )
    ),
    ManualSection(
        id = "whatsapp_status",
        category = "Status & Updates",
        title = "Status Updates (24-Hour Stories)",
        icon = Icons.Default.DonutLarge,
        badge = "New",
        summary = "Share text thoughts, announcements, and photos that automatically expire after 24 hours.",
        steps = listOf(
            "Navigate to the 'Updates' bottom navigation tab.",
            "Under 'My Status', tap the '+' or the camera/pencil floating action button.",
            "Compose your update, choose a vibrant background color gradient, and optionally add a media URL.",
            "Tap 'Share to My Status' — your status is broadcast to SAT colleagues for 24 hours.",
            "Tap your status to view who has seen it, or tap a colleague's status ring to watch their full-screen interactive story."
        ),
        tips = listOf(
            "Contacts with active statuses display a green status ring around their avatar in the conversation list.",
            "You can reply directly to any colleague's status from the bottom reply bar in the story viewer."
        )
    ),
    ManualSection(
        id = "broadcast_channels",
        category = "Channels",
        title = "Broadcast Channels",
        icon = Icons.Default.Campaign,
        badge = "Broadcast",
        summary = "Follow one-way company announcements, mission telemetry alerts, and departmental news.",
        steps = listOf(
            "Go to 'Updates' tab and tap 'Channels'.",
            "Browse public SAT channels or tap the megaphone FAB to create your own.",
            "Tap 'Follow' on any channel to subscribe to corporate announcements.",
            "Tap on a channel card to view its broadcast feed and react with emoji pills (👍, ❤️, 🚀, 👏).",
            "Channel administrators can post announcements with text and images directly from the bottom composer."
        ),
        tips = listOf(
            "Follower counts are updated live across the organization in Firestore."
        )
    ),
    ManualSection(
        id = "whatsapp_communities",
        category = "Communities",
        title = "Community Chats & Subgroups",
        icon = Icons.Default.Groups,
        badge = "Enterprise",
        summary = "Group related chat rooms and teams under an umbrella community hub like WhatsApp.",
        steps = listOf(
            "Tap the 'Communities' tab on the bottom navigation bar.",
            "Tap 'Start a Community' or the community FAB.",
            "Name the community (e.g. 'Flight Software Division'), add a description, and specify subgroup chat rooms.",
            "Every community automatically features an '📢 Announcements' room for broadcast notices.",
            "Tap on any subgroup room card inside a community to jump right into discussion."
        ),
        tips = listOf(
            "Communities help large teams avoid chat clutter by categorizing related topics together."
        )
    ),
    ManualSection(
        id = "search_and_contact_details",
        category = "Search & Details",
        title = "User Details & Phone/Email Search",
        icon = Icons.Default.Phone,
        badge = "Directory",
        summary = "Search colleagues by name, email, or phone number, and view rich WhatsApp-style contact details.",
        steps = listOf(
            "Tap the Search tab on the bottom bar or the search icon in the top bar.",
            "Type a user's display name, company email, or phone number (e.g. '+1 555').",
            "Results display matching indicators highlighting whether name, email, or phone matched.",
            "Inside any chat, tap the top bar user name or group title to open full Contact / Group Details.",
            "Contact Info reveals phone number (tap to copy), email, status, quick Audio/Video call affordances, shared media, and security encryption status."
        ),
        tips = listOf(
            "Update your phone number anytime under Profile > Profile Details so colleagues can easily find and call you."
        )
    ),
    ManualSection(
        id = "media_sharing",
        category = "Media & Files",
        title = "Sending Photos & Media Files",
        icon = Icons.Default.AttachFile,
        badge = "Zero-Permission",
        summary = "Share photos, images, and documents with instant preview, caption editing, and full-screen viewing.",
        steps = listOf(
            "Tap the paperclip attachment icon on the left of the message composer in any chat.",
            "Select 'Photo & Media' to open Android's secure zero-permission Photo Picker, or 'Document & Files' for other files.",
            "In the preview sheet, review the thumbnail or document card, file name, and size.",
            "Optionally add a message caption in the text input field.",
            "Tap 'Send' to compress and deliver the media securely to chat participants.",
            "Tap any shared image in the conversation thread to launch the full-screen photo viewer."
        ),
        tips = listOf(
            "Images are automatically optimized and compressed to ensure instant delivery even on low bandwidth connections."
        )
    ),
    ManualSection(
        id = "chat_management_and_badges",
        category = "Chat Management",
        title = "Deleting Chats, Clearing History & Unread Notifications",
        icon = Icons.Default.DeleteSweep,
        badge = "Badges & Alerts",
        summary = "Delete conversations, clear message logs, and stay updated with real-time unread badges and banners.",
        steps = listOf(
            "Long press on any chat item in the main Chats list to open the quick action dialog.",
            "Tap 'Clear Chat History' to remove all messages while keeping the conversation room open.",
            "Tap 'Delete Chat' to permanently remove the conversation and its message records.",
            "Alternatively, tap the 3-dots menu inside any chat or tap Contact Details to find 'Clear Chat' and 'Delete Conversation'.",
            "Unread message counts appear as green badge pills on conversation cards and on the bottom 'Chats' tab icon.",
            "When new messages arrive from other chats while using the app, an in-app alert banner appears at the top for instant switching."
        ),
        tips = listOf(
            "All clearing and deletion actions require explicit confirmation to protect important communications from accidental deletion."
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
    val categories = listOf("All", "Getting Started", "Direct Chats", "Group Rooms", "Media & Files", "Chat Management", "Status & Updates", "Channels", "Communities", "Search & Details", "Real-Time & Receipts", "Privacy & Safety")

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
                            text = "Everything you need to know about 1-on-1 direct messaging, group chat rooms, real-time typing indicators, read receipts, and corporate compliance.",
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
