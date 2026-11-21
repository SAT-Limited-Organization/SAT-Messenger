package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.formatAudioDuration
import com.example.data.model.AvailabilityStatus
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.ui.components.AvailabilityStatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatNavyDark
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailsScreen(
    chat: Chat,
    currentUserId: String,
    messages: List<Message>,
    directoryUsers: List<UserProfile>,
    onBack: () -> Unit,
    onDirectMessageUser: (UserProfile) -> Unit,
    onBlockUser: (String, String) -> Unit,
    onReportUser: (String, String) -> Unit,
    onClearChat: (() -> Unit)? = null,
    onDeleteChat: (() -> Unit)? = null,
    onAddParticipants: (() -> Unit)? = null,
    onShareGroupInvite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
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
            ?: otherMemberId?.let { "@user_${it.take(6)}" }
    } else null

    val title = if (isGroup) chat.name?.takeIf { it.isNotBlank() } ?: "Group Room" else otherNameOrUsername
    val isOnline = otherUser?.isOnline ?: false
    val otherAvailability = otherUser?.resolvedAvailability() ?: AvailabilityStatus.OFFLINE

    var isMuted by remember { mutableStateOf(false) }
    var participantSearch by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sharedMedia = remember(messages) {
        messages.filter { !it.attachmentUrl.isNullOrBlank() || it.type == "image" }
    }

    val groupMembers = remember(chat.memberIds, directoryUsers, participantSearch) {
        val members = chat.memberIds.mapNotNull { id ->
            directoryUsers.firstOrNull { it.userId == id } ?: if (id == currentUserId) {
                UserProfile(userId = id, displayName = "You", email = "me@satlimited.com")
            } else null
        }
        if (participantSearch.isBlank()) members
        else members.filter {
            it.displayName.contains(participantSearch, ignoreCase = true) ||
                    it.email.contains(participantSearch, ignoreCase = true) ||
                    (it.phoneNumber?.contains(participantSearch, ignoreCase = true) == true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_details_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = if (isGroup) "Group Info" else "Contact Info",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "Contact: $title\nEmail: ${otherUser?.email ?: "N/A"}\nPhone: ${otherUser?.phoneNumber ?: "N/A"}"
                            )
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Contact"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Header Card
            item {
                Card(
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = SatNavyDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                    ) {
                        UserAvatar(
                            name = title,
                            avatarUrl = otherUser?.avatarUrl,
                            isOnline = if (isGroup) false else isOnline,
                            availabilityStatus = if (isGroup) null else otherAvailability,
                            isGroup = isGroup,
                            size = 96.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (otherUsernameHandle != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = otherUsernameHandle,
                                style = MaterialTheme.typography.titleMedium,
                                color = SatTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!isGroup) {
                            AvailabilityStatusPill(
                                status = otherAvailability,
                                lastSeen = otherUser?.lastSeen,
                                showOfflineLastSeen = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        if (!isGroup && otherUser?.phoneNumber != null) {
                            Text(
                                text = otherUser.phoneNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SatElectricBlue,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        if (isGroup) {
                            Text(
                                text = "Group Room • ${chat.memberIds.size} participants",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // WhatsApp Action Buttons (Audio, Video, Search)
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuickActionPill(
                                icon = Icons.Default.Message,
                                label = "Chat",
                                onClick = onBack
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            QuickActionPill(
                                icon = Icons.Default.Call,
                                label = "Audio",
                                onClick = {
                                    Toast.makeText(context, "SAT Voice Call connecting to $title...", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            QuickActionPill(
                                icon = Icons.Default.Videocam,
                                label = "Video",
                                onClick = {
                                    Toast.makeText(context, "SAT Video Call connecting to $title...", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            QuickActionPill(
                                icon = Icons.Default.Search,
                                label = "Search",
                                onClick = onBack
                            )
                        }
                    }
                }
            }

            // About / Status Message Section
            if (!isGroup) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "About and Phone Number",
                                style = MaterialTheme.typography.labelMedium,
                                color = SatTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = otherUser?.statusMessage ?: "Available for SAT Limited operations",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enterprise Status",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            // Phone Number row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        otherUser?.phoneNumber?.let { num ->
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Phone Number", num))
                                            Toast.makeText(context, "Phone number copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(SatElectricBlue.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = SatElectricBlue, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = otherUser?.phoneNumber ?: "Not specified",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Phone (Tap to copy)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (otherUser?.phoneNumber != null) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = SatTeal, modifier = Modifier.size(18.dp))
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )

                            // Email row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(SatTeal.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = SatTeal, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = otherUser?.email?.takeIf { it.isNotBlank() }
                                            ?: otherMemberId?.let { "${it.take(6)}@satlimited.com" }
                                            ?: "user@satlimited.com",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "SAT Corporate Email",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Group Description & Invite Link Section
            if (isGroup) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!chat.description.isNullOrBlank()) {
                                Text(
                                    text = "Group Topic & Charter",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SatTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = chat.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (onShareGroupInvite != null) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                }
                            }

                            if (onShareGroupInvite != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onShareGroupInvite() }
                                        .testTag("details_group_invite_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(SatElectricBlue.copy(alpha = 0.16f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = "Group Invite Link",
                                            tint = SatElectricBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Invite via Link",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (!chat.inviteCode.isNullOrBlank()) {
                                                "Code: ${chat.inviteCode} • Tap to copy or share"
                                            } else {
                                                "Generate a shareable invite link for this group"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SatTeal
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Group Invite",
                                        tint = SatElectricBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Shared Media Section
            if (sharedMedia.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Media, Links and Docs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${sharedMedia.size} items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SatTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(sharedMedia) { msg ->
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SatNavyDark)
                                    ) {
                                        when {
                                            msg.type == "audio" -> {
                                                Column(
                                                    modifier = Modifier.align(Alignment.Center),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Mic,
                                                        contentDescription = "Voice clip",
                                                        tint = SatTeal,
                                                        modifier = Modifier.size(26.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = formatAudioDuration(msg.durationSeconds ?: 0),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                            msg.type == "file" -> {
                                                Icon(
                                                    imageVector = Icons.Default.InsertDriveFile,
                                                    contentDescription = "Document",
                                                    tint = Color.White,
                                                    modifier = Modifier.align(Alignment.Center)
                                                )
                                            }
                                            !msg.attachmentUrl.isNullOrBlank() -> {
                                                AsyncImage(
                                                    model = msg.attachmentUrl,
                                                    contentDescription = "Shared Attachment",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            else -> {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.align(Alignment.Center)
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

            // Group Members List
            if (isGroup) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${chat.memberIds.size} Participants",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                onAddParticipants?.let {
                                    IconButton(onClick = it) {
                                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add", tint = SatElectricBlue)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = participantSearch,
                                onValueChange = { participantSearch = it },
                                placeholder = { Text("Search members by name, email, or phone...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            groupMembers.forEach { member ->
                                val isAdmin = chat.adminIds.contains(member.userId)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (member.userId != currentUserId) {
                                                onDirectMessageUser(member)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        name = member.displayName,
                                        avatarUrl = member.avatarUrl,
                                        isOnline = member.isOnline,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (member.userId == currentUserId) "You" else member.displayName,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = member.phoneNumber ?: member.statusMessage ?: member.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isAdmin) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SatTeal.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Group Admin",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SatTeal,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }

            // Settings & Security Card
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = SatElectricBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Mute Notifications", fontWeight = FontWeight.SemiBold)
                                Text(text = "Silence alerts for incoming messages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isMuted, onCheckedChange = { isMuted = it })
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SatTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Zero-Trust Encryption", fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Messages and calls are secured with SAT Limited enterprise authentication rules.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Danger & Safety Actions
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        if (!isGroup && otherUser != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBlockUser(otherUser.userId, otherUser.displayName) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = SatErrorRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Block ${otherUser.displayName}", color = SatErrorRed, fontWeight = FontWeight.SemiBold)
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onReportUser(otherUser.userId, otherUser.displayName) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Report, contentDescription = null, tint = SatErrorRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Report ${otherUser.displayName} to SAT Compliance", color = SatErrorRed, fontWeight = FontWeight.SemiBold)
                            }
                        } else if (isGroup) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onBack() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, tint = SatErrorRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Exit Group Room", color = SatErrorRed, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (onClearChat != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showClearConfirm = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("details_clear_chat_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = SatErrorRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Clear Chat History", color = SatErrorRed, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (onDeleteChat != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeleteConfirm = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("details_delete_chat_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = SatErrorRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (isGroup) "Delete Group Room" else "Delete Chat",
                                    color = SatErrorRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Clear Chat History?") },
                text = { Text("Are you sure you want to clear all messages in this conversation? Messages will be removed for participants.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearConfirm = false
                            onClearChat?.invoke()
                            onBack()
                        }
                    ) {
                        Text("Clear", color = SatErrorRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(if (isGroup) "Delete Group Room?" else "Delete Chat?") },
                text = { Text("Are you sure you want to delete this ${if (isGroup) "group room" else "chat"}? All messages and chat records will be permanently removed.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            onDeleteChat?.invoke()
                            onBack()
                        }
                    ) {
                        Text("Delete", color = SatErrorRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.size(width = 72.dp, height = 56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = SatTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
