package com.example.ui.screens.status

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.StatusItem
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatNavyDark
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusScreen(
    currentUserId: String,
    currentUserProfile: UserProfile?,
    statuses: List<StatusItem>,
    onPostStatus: (text: String?, mediaUrl: String?, backgroundColor: String?) -> Unit,
    onDeleteStatus: (String) -> Unit,
    onMarkStatusViewed: (String) -> Unit,
    onReplyToStatus: (authorId: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeViewingStatus by remember { mutableStateOf<StatusItem?>(null) }

    val myStatuses = remember(statuses, currentUserId) {
        statuses.filter { it.userId == currentUserId }
    }
    val contactStatuses = remember(statuses, currentUserId) {
        statuses.filter { it.userId != currentUserId }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = SatTeal,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("create_text_status_fab")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Text Status", modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = SatElectricBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("create_status_fab")
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Add Status", modifier = Modifier.size(26.dp))
                }
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
            // My Status Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (myStatuses.isNotEmpty()) {
                                activeViewingStatus = myStatuses.first()
                            } else {
                                showCreateDialog = true
                            }
                        }
                        .padding(vertical = 8.dp)
                        .testTag("my_status_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(56.dp)) {
                        UserAvatar(
                            name = currentUserProfile?.displayName ?: "Me",
                            avatarUrl = currentUserProfile?.avatarUrl,
                            isOnline = false,
                            size = 52.dp
                        )
                        if (myStatuses.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(2.5.dp, SatTeal, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(SatElectricBlue, CircleShape)
                                    .align(Alignment.BottomEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Status", tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "My Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (myStatuses.isNotEmpty()) {
                                val latest = myStatuses.first()
                                val time = formatStatusTime(latest.createdAt?.toDate())
                                "$time • ${latest.viewerIds.size} views"
                            } else {
                                "Tap to add status update (24 hours)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (myStatuses.isNotEmpty()) {
                        IconButton(onClick = { onDeleteStatus(myStatuses.first().id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            }

            // Recent Updates Header
            item {
                Text(
                    text = "Recent Updates (${contactStatuses.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = SatTeal,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (contactStatuses.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "No Recent Contact Updates",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When colleagues at SAT Limited post photos or announcements, their updates will appear here for 24 hours.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(contactStatuses, key = { it.id }) { status ->
                    val isViewed = status.viewerIds.contains(currentUserId)
                    val ringColor = if (isViewed) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else SatOnlineGreen

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeViewingStatus = status
                                onMarkStatusViewed(status.id)
                            }
                            .padding(vertical = 10.dp)
                            .testTag("status_item_${status.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .border(2.5.dp, ringColor, CircleShape)
                                .padding(3.dp)
                        ) {
                            UserAvatar(
                                name = status.userName,
                                avatarUrl = status.userAvatarUrl,
                                isOnline = false,
                                size = 48.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = status.userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatStatusTime(status.createdAt?.toDate()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!status.text.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = status.text,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 70.dp)
                    )
                }
            }
        }
    }

    // Interactive Full-Screen Status Story Viewer
    activeViewingStatus?.let { status ->
        StatusStoryViewerDialog(
            status = status,
            currentUserId = currentUserId,
            onDismiss = { activeViewingStatus = null },
            onReply = { text ->
                onReplyToStatus(status.userId, text)
                activeViewingStatus = null
            }
        )
    }

    // Create Status Sheet
    if (showCreateDialog) {
        CreateStatusDialog(
            onDismiss = { showCreateDialog = false },
            onPost = { text, mediaUrl, bgColor ->
                onPostStatus(text, mediaUrl, bgColor)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun StatusStoryViewerDialog(
    status: StatusItem,
    currentUserId: String,
    onDismiss: () -> Unit,
    onReply: (String) -> Unit
) {
    val progress = remember { Animatable(0f) }
    var replyText by remember { mutableStateOf("") }
    val bgColor = remember(status.backgroundColor) {
        try {
            Color(android.graphics.Color.parseColor(status.backgroundColor ?: "#0A2540"))
        } catch (_: Exception) {
            SatNavyDark
        }
    }

    LaunchedEffect(status.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6000, easing = LinearEasing)
        )
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            // Media Image if present
            if (!status.mediaUrl.isNullOrBlank()) {
                AsyncImage(
                    model = status.mediaUrl,
                    contentDescription = "Status Media",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Progress Bar at Top
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = SatTeal,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Author Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = status.userName,
                        avatarUrl = status.userAvatarUrl,
                        isOnline = false,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = status.userName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = formatStatusTime(status.createdAt?.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Status Text Payload
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!status.text.isNullOrBlank()) {
                        Text(
                            text = status.text,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                // Viewer Count or Reply Bar
                if (status.userId == currentUserId) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = SatTeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${status.viewerIds.size} views",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Reply to ${status.userName}...", color = Color.White.copy(alpha = 0.6f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SatTeal,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("status_reply_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (replyText.isNotBlank()) {
                                    onReply(replyText)
                                }
                            },
                            enabled = replyText.isNotBlank()
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send Reply", tint = SatTeal)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateStatusDialog(
    onDismiss: () -> Unit,
    onPost: (text: String?, mediaUrl: String?, bgColor: String) -> Unit
) {
    var statusText by remember { mutableStateOf("") }
    var mediaUrl by remember { mutableStateOf("") }
    val colorPresets = listOf("#0A2540", "#008080", "#2E7D32", "#6A1B9A", "#C2185B", "#E65100")
    var selectedColor by remember { mutableStateOf(colorPresets[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Status Update",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text caption
                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    label = { Text("What's on your mind?") },
                    placeholder = { Text("e.g. Deploying satellite software payload today 🚀") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("status_input_text")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Media URL
                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = { mediaUrl = it },
                    label = { Text("Photo / Media URL (Optional)") },
                    placeholder = { Text("https://example.com/photo.jpg") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Background Color",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Color picker
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colorPresets) { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 1.dp,
                                    color = if (selectedColor == hex) Color.White else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onPost(
                            statusText.ifBlank { null },
                            mediaUrl.ifBlank { null },
                            selectedColor
                        )
                    },
                    enabled = statusText.isNotBlank() || mediaUrl.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("submit_status_button")
                ) {
                    Text("Share to My Status (24h)")
                }
            }
        }
    }
}

private fun formatStatusTime(date: Date?): String {
    if (date == null) return "Just now"
    val diffMs = System.currentTimeMillis() - date.time
    val hours = diffMs / (60 * 60 * 1000)
    val minutes = diffMs / (60 * 1000)
    return when {
        minutes < 2 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
    }
}
