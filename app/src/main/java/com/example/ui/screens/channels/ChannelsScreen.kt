package com.example.ui.screens.channels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Channel
import com.example.data.model.ChannelPost
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatNavyDark
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CHANNEL_CATEGORIES = listOf(
    "General",
    "Engineering",
    "Operations",
    "Announcements",
    "Social",
    "Alerts"
)

private fun encodeChannelImageBase64(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val original = BitmapFactory.decodeStream(stream) ?: return null
            val maxDim = 800
            val width = original.width
            val height = original.height
            val scaled = if (width > maxDim || height > maxDim) {
                val ratio = width.toFloat() / height.toFloat()
                val (targetW, targetH) = if (ratio > 1f) {
                    maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
                } else {
                    (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
                }
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else {
                original
            }
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
            "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ChannelsScreen(
    currentUserId: String,
    channels: List<Channel>,
    onToggleFollow: (Channel) -> Unit,
    onCreateChannel: (
        name: String,
        description: String?,
        iconUrl: String?,
        category: String,
        isPublic: Boolean,
        allowFollowerPosts: Boolean,
        allowReactions: Boolean
    ) -> Unit,
    onOpenChannel: (Channel) -> Unit,
    onShareInvite: (Channel) -> Unit = {},
    onOpenJoinInvite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var scopeFilter by remember { mutableStateOf("All") } // "All", "Following", "Managed"
    var categoryFilter by remember { mutableStateOf<String?>(null) }

    val visibleChannels = remember(channels, currentUserId, searchQuery, scopeFilter, categoryFilter) {
        channels.filter { ch ->
            val isMemberOrAdmin = ch.creatorId == currentUserId ||
                ch.adminIds.contains(currentUserId) ||
                ch.subscriberIds.contains(currentUserId)
            // Invite-only channels appear only for members/admins; public channels appear for everyone
            if (!ch.isPublic && !isMemberOrAdmin) return@filter false

            val matchesScope = when (scopeFilter) {
                "Following" -> ch.subscriberIds.contains(currentUserId)
                "Managed" -> ch.creatorId == currentUserId || ch.adminIds.contains(currentUserId)
                else -> true
            }
            if (!matchesScope) return@filter false

            if (categoryFilter != null && !ch.category.equals(categoryFilter, ignoreCase = true)) {
                return@filter false
            }

            val q = searchQuery.trim()
            if (q.isBlank()) true
            else ch.name.contains(q, ignoreCase = true) ||
                ch.category.contains(q, ignoreCase = true) ||
                (ch.description?.contains(q, ignoreCase = true) == true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SatElectricBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("create_channel_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Campaign,
                    contentDescription = "Create Channel",
                    modifier = Modifier.size(26.dp)
                )
            }
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
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SatNavyDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(SatTeal.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = SatTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SAT Channels",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Broadcast updates or open topic discussions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SatTeal
                                )
                            }
                            Surface(
                                onClick = onOpenJoinInvite,
                                shape = RoundedCornerShape(20.dp),
                                color = SatTeal.copy(alpha = 0.2f),
                                modifier = Modifier.testTag("channels_join_invite_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "Join Channel via Invite",
                                        tint = SatTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Join Invite",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search channels by name, topic, or category...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channels_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(listOf("All", "Following", "Managed")) { scope ->
                        FilterChip(
                            selected = scopeFilter == scope && categoryFilter == null,
                            onClick = {
                                scopeFilter = scope
                                categoryFilter = null
                            },
                            label = { Text(scope) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SatElectricBlue,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("channel_scope_chip_${scope.lowercase()}")
                        )
                    }
                    items(CHANNEL_CATEGORIES) { cat ->
                        FilterChip(
                            selected = categoryFilter == cat,
                            onClick = {
                                categoryFilter = if (categoryFilter == cat) null else cat
                            },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SatTeal,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("channel_category_chip_${cat.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Available Channels (${visibleChannels.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (visibleChannels.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = SatTeal,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (channels.isEmpty()) "No Channels Yet" else "No Matching Channels",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create a public or invite-only channel, configure follower posting permissions, or join with an invite link.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onOpenJoinInvite,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Join Invite")
                                }
                                Button(
                                    onClick = { showCreateDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Create Channel")
                                }
                            }
                        }
                    }
                }
            } else {
                items(visibleChannels, key = { it.id }) { channel ->
                    val isFollowing = channel.subscriberIds.contains(currentUserId)
                    val isAdmin = channel.creatorId == currentUserId || channel.adminIds.contains(currentUserId)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenChannel(channel) }
                            .padding(vertical = 12.dp)
                            .testTag("channel_item_${channel.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(SatElectricBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!channel.iconUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = channel.iconUrl,
                                    contentDescription = channel.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = SatElectricBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = channel.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (!channel.isPublic) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Invite-only Channel",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SatTeal.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = channel.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SatTeal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (channel.allowFollowerPosts) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SatOnlineGreen.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "Open Posts",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SatOnlineGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${channel.followerCount} followers • ${channel.description ?: "Official SAT updates"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (channel.isPublic || isAdmin) {
                            IconButton(
                                onClick = { onShareInvite(channel) },
                                modifier = Modifier.testTag("channel_share_invite_${channel.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Channel Invite Link",
                                    tint = SatElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isFollowing) {
                            OutlinedButton(
                                onClick = { onToggleFollow(channel) },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Following", fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = { onToggleFollow(channel) },
                                colors = ButtonDefaults.buttonColors(containerColor = SatTeal, contentColor = Color.Black),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Follow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateChannelDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, icon, category, isPublic, allowFollowerPosts, allowReactions ->
                onCreateChannel(name, desc, icon, category, isPublic, allowFollowerPosts, allowReactions)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun ChannelFeedDialog(
    channel: Channel,
    currentUserId: String,
    posts: List<ChannelPost>,
    onDismiss: () -> Unit,
    onToggleFollow: (Channel) -> Unit = {},
    onPostMessage: (text: String, mediaUrl: String?) -> Unit,
    onDeletePost: (postId: String) -> Unit = {},
    onUpdateChannelSettings: (
        name: String,
        description: String?,
        category: String,
        isPublic: Boolean,
        allowFollowerPosts: Boolean,
        allowReactions: Boolean
    ) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteChannel: () -> Unit = {},
    onShareInvite: () -> Unit = {},
    onReact: (postId: String, reactions: Map<String, Int>, emoji: String) -> Unit
) {
    val context = LocalContext.current
    val isCreator = channel.creatorId == currentUserId
    val isAdmin = isCreator || channel.adminIds.contains(currentUserId)
    val isFollowing = channel.subscriberIds.contains(currentUserId)
    val canPost = isAdmin || (channel.allowFollowerPosts && isFollowing)

    var newPostText by remember { mutableStateOf("") }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageBase64 = encodeChannelImageBase64(context, uri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = channel.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${channel.category} • ${if (channel.isPublic) "Public" else "Invite-only"} • ${channel.followerCount} followers",
                                style = MaterialTheme.typography.labelSmall,
                                color = SatTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onShareInvite,
                            modifier = Modifier.testTag("channel_feed_share_invite_button")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share Invite Link", tint = SatElectricBlue)
                        }
                        if (isAdmin) {
                            IconButton(
                                onClick = { showSettingsDialog = true },
                                modifier = Modifier.testTag("channel_feed_settings_button")
                            ) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Channel Settings", tint = SatTeal)
                            }
                        } else {
                            TextButton(onClick = { onToggleFollow(channel) }) {
                                Text(
                                    text = if (isFollowing) "Following" else "Follow",
                                    color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else SatTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
            ) {
                // Channel Info Strip
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (channel.allowFollowerPosts) Icons.Default.Forum else Icons.Default.Campaign,
                            contentDescription = null,
                            tint = SatTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = buildString {
                                append(if (channel.allowFollowerPosts) "Open posting for followers" else "Admin-only broadcasts")
                                if (!channel.description.isNullOrBlank()) {
                                    append(" • ${channel.description}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    if (posts.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                ) {
                                    Text("No updates posted yet.", fontWeight = FontWeight.SemiBold)
                                    if (canPost) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Share an announcement or photo update using the composer below.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            val canDeletePost = isAdmin || post.authorId == currentUserId
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = post.authorName, fontWeight = FontWeight.Bold, color = SatTeal)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = formatPostTime(post.createdAt?.toDate()),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (canDeletePost) {
                                                IconButton(
                                                    onClick = { onDeletePost(post.id) },
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .padding(start = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete Post",
                                                        tint = SatErrorRed,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (post.text.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(text = post.text, style = MaterialTheme.typography.bodyLarge)
                                    }

                                    if (!post.mediaUrl.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AsyncImage(
                                            model = post.mediaUrl,
                                            contentDescription = "Channel post attachment",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(190.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { fullScreenImage = post.mediaUrl }
                                        )
                                    }

                                    if (channel.allowReactions) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val emojis = listOf("👍", "❤️", "🚀", "👏", "🔥")
                                            emojis.forEach { emoji ->
                                                val count = post.reactions[emoji] ?: 0
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = if (count > 0) SatTeal.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                                    modifier = Modifier.clickable {
                                                        onReact(post.id, post.reactions, emoji)
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = emoji, fontSize = 14.sp)
                                                        if (count > 0) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(text = "$count", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Composer or Follow Prompt
                if (canPost) {
                    Surface(
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!attachedImageBase64.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 8.dp)
                                        .size(84.dp)
                                ) {
                                    AsyncImage(
                                        model = attachedImageBase64,
                                        contentDescription = "Selected photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    IconButton(
                                        onClick = { attachedImageBase64 = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier.testTag("channel_attach_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Attach Photo",
                                        tint = SatTeal
                                    )
                                }

                                OutlinedTextField(
                                    value = newPostText,
                                    onValueChange = { newPostText = it },
                                    placeholder = {
                                        Text(
                                            if (isAdmin) "Broadcast an update..." else "Post to ${channel.name}..."
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    maxLines = 3,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("channel_post_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newPostText.isNotBlank() || !attachedImageBase64.isNullOrBlank()) {
                                            onPostMessage(newPostText, attachedImageBase64)
                                            newPostText = ""
                                            attachedImageBase64 = null
                                        }
                                    },
                                    enabled = newPostText.isNotBlank() || !attachedImageBase64.isNullOrBlank(),
                                    modifier = Modifier.testTag("channel_post_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Post Update",
                                        tint = SatElectricBlue
                                    )
                                }
                            }
                        }
                    }
                } else if (channel.allowFollowerPosts && !isFollowing) {
                    Surface(
                        tonalElevation = 4.dp,
                        color = SatTeal.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Follow this channel to post updates",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onToggleFollow(channel) },
                                colors = ButtonDefaults.buttonColors(containerColor = SatTeal, contentColor = Color.Black),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Follow", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        ChannelSettingsDialog(
            channel = channel,
            isCreator = isCreator,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, desc, cat, isPublic, allowFollowerPosts, allowReactions ->
                onUpdateChannelSettings(name, desc, cat, isPublic, allowFollowerPosts, allowReactions)
                showSettingsDialog = false
            },
            onDeleteChannel = {
                showSettingsDialog = false
                onDeleteChannel()
            }
        )
    }

    if (!fullScreenImage.isNullOrBlank()) {
        Dialog(onDismissRequest = { fullScreenImage = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { fullScreenImage = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fullScreenImage,
                    contentDescription = "Full screen channel media",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { fullScreenImage = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        desc: String?,
        icon: String?,
        category: String,
        isPublic: Boolean,
        allowFollowerPosts: Boolean,
        allowReactions: Boolean
    ) -> Unit
) {
    var channelName by remember { mutableStateOf("") }
    var channelDesc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("General") }
    var isPublic by remember { mutableStateOf(true) }
    var allowFollowerPosts by remember { mutableStateOf(false) }
    var allowReactions by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Create Flexible Channel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    label = { Text("Channel Name") },
                    placeholder = { Text("e.g. SAT Engineering Briefs") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_channel_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = channelDesc,
                    onValueChange = { channelDesc = it },
                    label = { Text("Description") },
                    placeholder = { Text("Describe updates or topic rules") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_channel_desc_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CHANNEL_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SatTeal,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                ChannelToggleRow(
                    icon = if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                    title = if (isPublic) "Public Channel" else "Invite-Only Channel",
                    subtitle = if (isPublic) "Discoverable in directory & via invite link" else "Hidden from directory; joinable only by invite link",
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    testTag = "channel_public_switch"
                )

                ChannelToggleRow(
                    icon = Icons.Default.Forum,
                    title = "Allow Followers to Post",
                    subtitle = if (allowFollowerPosts) "Any follower can share posts & photos" else "Only admins can broadcast updates",
                    checked = allowFollowerPosts,
                    onCheckedChange = { allowFollowerPosts = it },
                    testTag = "channel_follower_posts_switch"
                )

                ChannelToggleRow(
                    icon = Icons.Default.Campaign,
                    title = "Enable Emoji Reactions",
                    subtitle = "Allow members to react to channel updates",
                    checked = allowReactions,
                    onCheckedChange = { allowReactions = it },
                    testTag = "channel_reactions_switch"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onCreate(
                            channelName,
                            channelDesc.ifBlank { null },
                            null,
                            selectedCategory,
                            isPublic,
                            allowFollowerPosts,
                            allowReactions
                        )
                    },
                    enabled = channelName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_create_channel_button")
                ) {
                    Text("Create Channel")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChannelSettingsDialog(
    channel: Channel,
    isCreator: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        description: String?,
        category: String,
        isPublic: Boolean,
        allowFollowerPosts: Boolean,
        allowReactions: Boolean
    ) -> Unit,
    onDeleteChannel: () -> Unit
) {
    var name by remember(channel.id) { mutableStateOf(channel.name) }
    var description by remember(channel.id) { mutableStateOf(channel.description ?: "") }
    var category by remember(channel.id) { mutableStateOf(channel.category.ifBlank { "General" }) }
    var isPublic by remember(channel.id) { mutableStateOf(channel.isPublic) }
    var allowFollowerPosts by remember(channel.id) { mutableStateOf(channel.allowFollowerPosts) }
    var allowReactions by remember(channel.id) { mutableStateOf(channel.allowReactions) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Channel Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CHANNEL_CATEGORIES.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                ChannelToggleRow(
                    icon = if (isPublic) Icons.Default.Public else Icons.Default.Lock,
                    title = if (isPublic) "Public Channel" else "Invite-Only Channel",
                    subtitle = if (isPublic) "Listed publicly for everyone" else "Only accessible via invite link",
                    checked = isPublic,
                    onCheckedChange = { isPublic = it },
                    testTag = "edit_channel_public_switch"
                )

                ChannelToggleRow(
                    icon = Icons.Default.Forum,
                    title = "Allow Followers to Post",
                    subtitle = if (allowFollowerPosts) "Followers can contribute updates" else "Admin-only broadcasts",
                    checked = allowFollowerPosts,
                    onCheckedChange = { allowFollowerPosts = it },
                    testTag = "edit_channel_follower_posts_switch"
                )

                ChannelToggleRow(
                    icon = Icons.Default.Campaign,
                    title = "Enable Emoji Reactions",
                    subtitle = "Allow members to react to posts",
                    checked = allowReactions,
                    onCheckedChange = { allowReactions = it },
                    testTag = "edit_channel_reactions_switch"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSave(
                            name,
                            description.ifBlank { null },
                            category,
                            isPublic,
                            allowFollowerPosts,
                            allowReactions
                        )
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_channel_settings_button")
                ) {
                    Text("Save Changes")
                }

                if (isCreator) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = SatErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Channel", color = SatErrorRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Channel?") },
            text = { Text("Permanently delete \"${channel.name}\" and all of its posts and invite link? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteChannel()
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

@Composable
private fun ChannelToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SatTeal, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

private fun formatPostTime(date: Date?): String {
    if (date == null) return ""
    return SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
}
