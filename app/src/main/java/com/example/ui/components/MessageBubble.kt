package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.audio.VoicePlaybackState
import com.example.audio.formatAudioDuration
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatElectricBlueLight
import com.example.ui.theme.SatIncomingBubbleDark
import com.example.ui.theme.SatIncomingBubbleLight
import com.example.ui.theme.SatNavyBorder
import com.example.ui.theme.SatOutgoingBubble
import com.example.ui.theme.SatSlate400
import com.example.ui.theme.SatTeal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable MessageBubble Composable that renders distinct UI styles for
 * incoming and outgoing messages, featuring rich media attachments (images & files),
 * quoted replies, real-time emoji reactions, WhatsApp-style floating reaction bar,
 * and context actions.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isOutgoing: Boolean,
    isGroup: Boolean = false,
    currentUserId: String = "",
    registeredUsers: List<UserProfile> = emptyList(),
    onReply: () -> Unit = {},
    onCopy: () -> Unit = {},
    onDelete: () -> Unit = {},
    onReport: () -> Unit = {},
    onReact: (String) -> Unit = {},
    onImageClick: ((String) -> Unit)? = null,
    onFileClick: ((Message) -> Unit)? = null,
    audioPlaybackState: VoicePlaybackState = VoicePlaybackState(),
    onToggleAudioPlay: ((Message) -> Unit)? = null,
    onSeekAudio: ((Message, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showActionMenu by remember { mutableStateOf(false) }
    var showExtendedEmojiDialog by remember { mutableStateOf(false) }
    var showReactionDetailsDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.value == 0xFF070F1EUL

    val quickEmojis = remember {
        listOf("👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "🎉", "👏", "🚀")
    }
    val extendedEmojis = remember {
        listOf(
            "👍", "❤️", "😂", "😮", "😢", "🙏", "🔥", "🎉",
            "👏", "🚀", "✨", "💯", "👀", "💪", "🤝", "💡",
            "🎯", "🙌", "🤩", "🥳", "😎", "🤔", "🫡", "💥"
        )
    }

    // Outgoing vs Incoming Color Scheme
    val bubbleColor = when {
        isOutgoing -> SatOutgoingBubble
        isDark -> SatIncomingBubbleDark
        else -> SatIncomingBubbleLight
    }

    val textColor = when {
        isOutgoing -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val secondaryTextColor = when {
        isOutgoing -> Color.White.copy(alpha = 0.75f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Asymmetric Corner Geometry
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }

    // Border: Subtle delineation for incoming bubbles in dark theme
    val bubbleBorder = if (!isOutgoing && isDark) {
        BorderStroke(0.5.dp, SatNavyBorder.copy(alpha = 0.6f))
    } else null

    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = alignment
    ) {
        Box {
            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                shape = bubbleShape,
                border = bubbleBorder,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isOutgoing) 1.5.dp else 0.5.dp),
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 320.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showActionMenu = true }
                    )
                    .testTag("chat_bubble_${message.id}")
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 6.dp
                    )
                ) {
                    // Group Chat Sender Attribution for Incoming Messages
                    if (isGroup && !isOutgoing) {
                        val senderUser = registeredUsers.firstOrNull { it.userId == message.senderId }
                        val senderDisplayName = message.senderName.takeIf { it.isNotBlank() }
                            ?: senderUser?.username
                            ?: "Colleague"

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = senderDisplayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) SatTeal else SatElectricBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            senderUser?.usernameHandle?.let { handle ->
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = handle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = secondaryTextColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Quoted Reply Snippet
                    if (!message.replyToText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOutgoing) {
                                Color.White.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(3.5.dp)
                                        .height(32.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isOutgoing) Color.White else SatElectricBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.replyToSender.takeIf { !it.isNullOrBlank() } ?: "Colleague",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOutgoing) Color.White else SatElectricBlueLight,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.replyToText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else textColor.copy(alpha = 0.8f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Rich Media: Image Attachment
                    if (message.type == "image" && !message.attachmentUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isOutgoing) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onImageClick?.invoke(message.attachmentUrl) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(message.attachmentUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Shared photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }

                    // Rich Media: Document Attachment
                    if (message.type == "file") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isOutgoing) {
                                Color.White.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clickable {
                                    if (onFileClick != null) {
                                        onFileClick(message)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Document: ${message.fileName ?: message.text}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isOutgoing) Color.White.copy(alpha = 0.25f) else SatElectricBlue.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.InsertDriveFile,
                                            contentDescription = "Document attachment",
                                            tint = if (isOutgoing) Color.White else SatElectricBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = message.fileName ?: if (message.text.isNotBlank()) message.text else "Document",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sizeLabel = message.fileSize?.let { formatBubbleFileSize(it) } ?: "File Document"
                                    Text(
                                        text = sizeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = secondaryTextColor
                                    )
                                }
                            }
                        }
                    }

                    // Rich Media: Voice / Audio Clip
                    if (message.type == "audio" && !message.deleted) {
                        val isActiveClip = audioPlaybackState.activeMessageId == message.id
                        val isClipPlaying = isActiveClip && audioPlaybackState.isPlaying
                        val totalMs = when {
                            isActiveClip && audioPlaybackState.durationMs > 0 -> audioPlaybackState.durationMs
                            (message.durationSeconds ?: 0) > 0 -> message.durationSeconds!! * 1000
                            else -> 0
                        }
                        val currentMs = if (isActiveClip) audioPlaybackState.currentPositionMs.coerceIn(0, totalMs.coerceAtLeast(1)) else 0
                        val progress = if (totalMs > 0) (currentMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0f
                        val displaySeconds = if (isActiveClip && (isClipPlaying || currentMs > 0)) {
                            currentMs / 1000
                        } else {
                            message.durationSeconds ?: (totalMs / 1000)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isOutgoing) {
                                Color.White.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                                .semantics(mergeDescendants = true) {}
                                .testTag("audio_player_${message.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = !message.attachmentUrl.isNullOrBlank()) {
                                            if (onToggleAudioPlay != null) {
                                                onToggleAudioPlay(message)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Audio playback unavailable in this view.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                        .testTag("audio_play_button_${message.id}")
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isOutgoing) Color.White else SatElectricBlue,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isClipPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = if (isClipPlaying) "Pause voice message" else "Play voice message",
                                                tint = if (isOutgoing) SatElectricBlue else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Slider(
                                        value = progress,
                                        onValueChange = { fraction ->
                                            if (isActiveClip && totalMs > 0 && onSeekAudio != null) {
                                                onSeekAudio(message, (fraction * totalMs).toInt())
                                            }
                                        },
                                        enabled = isActiveClip && totalMs > 0 && onSeekAudio != null,
                                        colors = SliderDefaults.colors(
                                            thumbColor = if (isOutgoing) Color.White else SatElectricBlue,
                                            activeTrackColor = if (isOutgoing) Color.White else SatElectricBlue,
                                            inactiveTrackColor = if (isOutgoing) Color.White.copy(alpha = 0.35f) else SatSlate400.copy(alpha = 0.35f),
                                            disabledThumbColor = if (isOutgoing) Color.White.copy(alpha = 0.9f) else SatElectricBlue,
                                            disabledActiveTrackColor = if (isOutgoing) Color.White else SatElectricBlue,
                                            disabledInactiveTrackColor = if (isOutgoing) Color.White.copy(alpha = 0.35f) else SatSlate400.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isActiveClip && totalMs > 0) {
                                                "${formatAudioDuration(displaySeconds)} / ${formatAudioDuration(totalMs / 1000)}"
                                            } else {
                                                formatAudioDuration(displaySeconds)
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            color = secondaryTextColor
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isClipPlaying) Icons.Default.GraphicEq else Icons.Default.Mic,
                                                contentDescription = null,
                                                tint = secondaryTextColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Voice",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = secondaryTextColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Message Body Text (Or Deleted Tombstone)
                    if (message.deleted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = SatSlate400,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "This message was deleted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SatSlate400,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    } else if (message.text.isNotBlank() &&
                        (message.type != "file" || message.fileName != message.text) &&
                        (message.type != "audio" || message.text != "Voice message")
                    ) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Message Timestamp & Delivery Indicator Row
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatBubbleTime(message.createdAt?.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = secondaryTextColor
                        )
                        if (isOutgoing) {
                            val isSeenByOthers = message.readBy.filterKeys { it != message.senderId }.values.any { it } || message.status == "read"
                            Spacer(modifier = Modifier.width(4.dp))
                            MessageStatusIndicator(
                                status = message.status,
                                isOutgoing = true,
                                isRead = isSeenByOthers
                            )
                        }
                    }

                    // Real-Time Emoji Reaction Chips
                    val groupedReactions = remember(message.reactions) { message.getReactionsGrouped() }
                    if (groupedReactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            groupedReactions.forEach { (emoji, uids) ->
                                val hasReacted = uids.contains(currentUserId)
                                val chipBg = if (hasReacted) {
                                    SatElectricBlue.copy(alpha = 0.35f)
                                } else if (isOutgoing) {
                                    Color.White.copy(alpha = 0.20f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }

                                val chipBorder = if (hasReacted) SatElectricBlue else Color.Transparent

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = chipBg,
                                    border = BorderStroke(1.dp, chipBorder),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = { onReact(emoji) },
                                            onLongClick = {
                                                val names = uids.map { uid ->
                                                    if (uid == currentUserId) "You"
                                                    else registeredUsers.firstOrNull { it.userId == uid }?.username ?: "User_${uid.take(4)}"
                                                }.joinToString(", ")
                                                Toast.makeText(context, "$emoji by $names", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        .testTag("reaction_badge_${message.id}_$emoji")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = emoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "${uids.size}",
                                            fontSize = 11.sp,
                                            fontWeight = if (hasReacted) FontWeight.Bold else FontWeight.Medium,
                                            color = if (hasReacted) SatElectricBlue else textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Context Action Menu with Floating WhatsApp-style Reaction Bar
            DropdownMenu(
                expanded = showActionMenu,
                onDismissRequest = { showActionMenu = false }
            ) {
                // Floating Quick Reaction Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("quick_reactions_bar_${message.id}")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        quickEmojis.forEach { emoji ->
                            val isSelected = message.hasUserReacted(currentUserId, emoji)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) SatElectricBlue.copy(alpha = 0.25f) else Color.Transparent)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) SatElectricBlue else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onReact(emoji)
                                        showActionMenu = false
                                    }
                                    .testTag("quick_react_${message.id}_$emoji")
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }

                        // More Emojis Button (+)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    showActionMenu = false
                                    showExtendedEmojiDialog = true
                                }
                                .testTag("more_reactions_${message.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "More emojis",
                                tint = SatElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                if (message.reactions.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Reactions (${message.reactions.size})") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.EmojiEmotions, contentDescription = null, tint = SatElectricBlue)
                        },
                        onClick = {
                            showActionMenu = false
                            showReactionDetailsDialog = true
                        },
                        modifier = Modifier.testTag("view_reactions_${message.id}")
                    )
                }

                if (isOutgoing) {
                    val readCount = message.readBy.count { it.key != message.senderId && it.value }
                    DropdownMenuItem(
                        text = {
                            Text(if (readCount > 0) "Seen by $readCount participant${if (readCount > 1) "s" else ""}" else "Not seen yet")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = if (readCount > 0) SatTeal else SatSlate400
                            )
                        },
                        onClick = { showActionMenu = false }
                    )
                }

                DropdownMenuItem(
                    text = { Text("Reply") },
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                    },
                    onClick = {
                        showActionMenu = false
                        onReply()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Copy Message") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        showActionMenu = false
                        onCopy()
                    }
                )

                if (isOutgoing && !message.deleted) {
                    DropdownMenuItem(
                        text = { Text("Delete for Everyone") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        },
                        onClick = {
                            showActionMenu = false
                            onDelete()
                        }
                    )
                }

                if (!isOutgoing) {
                    DropdownMenuItem(
                        text = { Text("Report to Compliance") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Report, contentDescription = null)
                        },
                        onClick = {
                            showActionMenu = false
                            onReport()
                        }
                    )
                }
            }
        }
    }

    // Extended Emoji Picker Dialog
    if (showExtendedEmojiDialog) {
        Dialog(onDismissRequest = { showExtendedEmojiDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "React with Emoji",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showExtendedEmojiDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(extendedEmojis) { emoji ->
                            val isSelected = message.hasUserReacted(currentUserId, emoji)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) SatElectricBlue.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) SatElectricBlue else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onReact(emoji)
                                        showExtendedEmojiDialog = false
                                    }
                                    .testTag("extended_react_$emoji")
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Reaction Details Dialog
    if (showReactionDetailsDialog) {
        Dialog(onDismissRequest = { showReactionDetailsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Message Reactions (${message.reactions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showReactionDetailsDialog = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        items(message.reactions.toList(), key = { it.first }) { (uid, emoji) ->
                            val user = registeredUsers.firstOrNull { it.userId == uid }
                            val isSelf = uid == currentUserId
                            val userName = if (isSelf) "You"
                            else user?.displayName?.takeIf { it.isNotBlank() }
                                ?: user?.email?.substringBefore("@")
                                ?: "User_${uid.take(5)}"
                            val userHandle = if (isSelf) null
                            else user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }?.let { "@$it" }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = userName,
                                    avatarUrl = user?.avatarUrl,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (userHandle != null) {
                                        Text(
                                            text = userHandle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SatTeal
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = emoji, fontSize = 16.sp)
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

/**
 * Format timestamp into HH:mm for message bubble display.
 */
fun formatBubbleTime(date: Date?): String {
    if (date == null) return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}

/**
 * Format byte count into human-readable B, KB, MB, GB.
 */
fun formatBubbleFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    val group = digitGroups.coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, group.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[group])
}
