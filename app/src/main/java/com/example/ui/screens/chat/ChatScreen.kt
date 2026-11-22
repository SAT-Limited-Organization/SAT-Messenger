package com.example.ui.screens.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import java.io.ByteArrayOutputStream
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.RecordedAudioClip
import com.example.audio.VoicePlayerManager
import com.example.audio.VoiceRecorderManager
import com.example.audio.formatAudioDuration
import com.example.data.model.AvailabilityStatus
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.data.model.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MessageBubble
import com.example.ui.components.MessageStatusIndicator
import com.example.ui.components.UserAvatar
import com.example.ui.components.VoicePreviewComposerBar
import com.example.ui.components.VoiceRecordingComposerBar
import com.example.ui.components.availabilityColor
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatIncomingBubbleDark
import com.example.ui.theme.SatIncomingBubbleLight
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatOutgoingBubble
import com.example.ui.theme.SatSlate400
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.SatTeal
import com.example.ui.theme.SatErrorRed
import com.example.util.MediaAttachmentHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun processImageUriToBase64(context: Context, uri: Uri): String {
    return MediaAttachmentHelper.encodeImageUriToDataUrl(context, uri) ?: uri.toString()
}

private fun getFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
    var name = "Document"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (e: Exception) {}
    return Pair(name, size)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes.toFloat() / (1024 * 1024))
        bytes >= 1024 -> "${bytes / 1024} KB"
        else -> "$bytes B"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chat: Chat,
    currentUserId: String,
    messages: List<Message>,
    registeredUsers: List<UserProfile>,
    replyingTo: Message?,
    onBack: () -> Unit,
    onSendMessage: (text: String, type: String, attachmentUrl: String?) -> Unit,
    onSendMediaMessage: ((caption: String, type: String, attachmentUrl: String, fileName: String?, fileSize: Long?, durationSeconds: Int?) -> Unit)? = null,
    onClearChat: (() -> Unit)? = null,
    onDeleteChat: (() -> Unit)? = null,
    onSwitchUser: (UserProfile) -> Unit,
    onSetReplyingTo: (Message?) -> Unit,
    onDeleteMessage: (Message) -> Unit,
    onBlockUser: (String, String) -> Unit,
    onReportUser: (String, String) -> Unit,
    onTyping: (Boolean) -> Unit = {},
    onReactToMessage: (messageId: String, emoji: String) -> Unit = { _, _ -> },
    onShareGroupInvite: (() -> Unit)? = null,
    onToggleGroupAdmin: ((targetUserId: String, makeAdmin: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showRegisteredBar by remember { mutableStateOf(true) }
    var showParticipantsDialog by remember { mutableStateOf(false) }
    var showChatDetails by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }
    var fullScreenImageUri by remember { mutableStateOf<String?>(null) }
    var fullScreenVideoUri by remember { mutableStateOf<String?>(null) }

    // Voice recorder & playback controllers
    val voiceRecorder = remember(context) { VoiceRecorderManager(context) }
    val voicePlayer = remember(context) { VoicePlayerManager(context) }
    val audioPlaybackState by voicePlayer.playbackState.collectAsState()
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var amplitudeBars by remember { mutableStateOf(List(18) { 0.15f }) }
    var recordedPreviewClip by remember { mutableStateOf<RecordedAudioClip?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.release()
            voicePlayer.release()
        }
    }

    val sendRecordedClip: (RecordedAudioClip) -> Unit = { clip ->
        voicePlayer.stop()
        recordedPreviewClip = null
        if (onSendMediaMessage != null) {
            onSendMediaMessage(
                "Voice message",
                "audio",
                clip.dataUri,
                clip.fileName,
                clip.fileSizeBytes,
                clip.durationSeconds
            )
        } else {
            onSendMessage("Voice message", "audio", clip.dataUri)
        }
    }

    val stopRecordingToPreview: () -> Unit = {
        val result = voiceRecorder.stopAndEncode()
        isRecordingAudio = false
        result.onSuccess { clip ->
            recordedPreviewClip = clip
        }.onFailure { err ->
            Toast.makeText(
                context,
                err.localizedMessage ?: "Recording discarded.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val stopAndSendVoiceNow: () -> Unit = {
        val result = voiceRecorder.stopAndEncode()
        isRecordingAudio = false
        result.onSuccess { clip ->
            sendRecordedClip(clip)
        }.onFailure { err ->
            Toast.makeText(
                context,
                err.localizedMessage ?: "Voice clip too short.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val beginVoiceRecording: () -> Unit = {
        voicePlayer.stop()
        recordedPreviewClip = null
        val started = voiceRecorder.startRecording(
            onMaxDurationReached = {
                stopRecordingToPreview()
                Toast.makeText(
                    context,
                    "Maximum voice note length (90s) reached.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        if (started) {
            isRecordingAudio = true
            recordingSeconds = 0
            amplitudeBars = List(18) { 0.15f }
        } else {
            Toast.makeText(
                context,
                "Unable to access device microphone.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            val startedAt = System.currentTimeMillis()
            while (isRecordingAudio) {
                recordingSeconds = ((System.currentTimeMillis() - startedAt) / 1000L).toInt()
                val nextAmp = voiceRecorder.getMaxAmplitudeNormalized()
                amplitudeBars = (amplitudeBars.drop(1) + nextAmp)
                delay(90)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginVoiceRecording()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required to record voice clips.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val requestOrStartVoiceRecording: () -> Unit = {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMicPermission) {
            beginVoiceRecording()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Media attachment state
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaType by remember { mutableStateOf("image") }
    var pendingMediaName by remember { mutableStateOf<String?>(null) }
    var pendingMediaSize by remember { mutableStateOf<Long?>(null) }
    var pendingEncodedPayload by remember { mutableStateOf<String?>(null) }
    var showMediaPreviewDialog by remember { mutableStateOf(false) }
    var mediaCaption by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encodedResult = MediaAttachmentHelper.encodeVisualMedia(context, uri)
            if (encodedResult.errorMessage != null) {
                Toast.makeText(context, encodedResult.errorMessage, Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            pendingMediaUri = uri
            pendingMediaType = encodedResult.type
            pendingMediaName = encodedResult.fileName
            pendingMediaSize = encodedResult.fileSizeBytes
            pendingEncodedPayload = encodedResult.dataUrlOrUri
            mediaCaption = ""
            showMediaPreviewDialog = true
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val encodedResult = MediaAttachmentHelper.encodeDocumentOrFile(context, uri)
            if (encodedResult.errorMessage != null) {
                Toast.makeText(context, encodedResult.errorMessage, Toast.LENGTH_LONG).show()
                return@rememberLauncherForActivityResult
            }
            pendingMediaUri = uri
            pendingMediaType = encodedResult.type
            pendingMediaName = encodedResult.fileName
            pendingMediaSize = encodedResult.fileSizeBytes
            pendingEncodedPayload = encodedResult.dataUrlOrUri
            mediaCaption = ""
            showMediaPreviewDialog = true
        }
    }

    val listState = rememberLazyListState()

    if (showChatDetails) {
        ChatDetailsScreen(
            chat = chat,
            currentUserId = currentUserId,
            messages = messages,
            directoryUsers = registeredUsers,
            onBack = { showChatDetails = false },
            onDirectMessageUser = { user ->
                showChatDetails = false
                onSwitchUser(user)
            },
            onBlockUser = { targetId, targetName ->
                showChatDetails = false
                onBlockUser(targetId, targetName)
            },
            onReportUser = { targetId, targetName ->
                showChatDetails = false
                onReportUser(targetId, targetName)
            },
            onClearChat = onClearChat,
            onDeleteChat = onDeleteChat,
            onShareGroupInvite = onShareGroupInvite,
            onToggleGroupAdmin = onToggleGroupAdmin
        )
        return
    }

    val isGroup = chat.type == "group"
    val otherUserId = chat.memberIds.firstOrNull { it != currentUserId }
    val otherUser = registeredUsers.firstOrNull { it.userId == otherUserId }

    val otherNameOrUsername = when {
        otherUser != null && otherUser.displayName.isNotBlank() -> otherUser.displayName
        otherUser != null && otherUser.email.isNotBlank() -> otherUser.email.substringBefore("@")
        !otherUserId.isNullOrBlank() && !chat.participantNames[otherUserId].isNullOrBlank() -> chat.participantNames[otherUserId]!!
        chat.lastSenderId == otherUserId && !chat.lastSenderName.isNullOrBlank() -> chat.lastSenderName
        !otherUserId.isNullOrBlank() -> "User_${otherUserId.take(6)}"
        else -> "User"
    }

    val title = if (isGroup) chat.name?.takeIf { it.isNotBlank() } ?: "Group Room" else otherNameOrUsername
    val isOnline = otherUser?.isOnline == true

    // Real-time detection of other people typing in this chat
    val typingUsers = remember(chat.isTyping, registeredUsers, currentUserId) {
        chat.isTyping.filter { it.key != currentUserId && it.value }.keys.mapNotNull { uid ->
            val u = registeredUsers.firstOrNull { it.userId == uid }
            when {
                u != null && u.displayName.isNotBlank() -> u.displayName
                u != null && u.email.isNotBlank() -> u.email.substringBefore("@")
                else -> "User_${uid.take(6)}"
            }
        }
    }
    val isSomeoneTyping = typingUsers.isNotEmpty()
    val typingStatusText = when {
        typingUsers.size == 1 -> "${typingUsers.first()} is typing..."
        typingUsers.size == 2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing..."
        typingUsers.size > 2 -> "Multiple people are typing..."
        else -> ""
    }

    // Reset typing status when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            onTyping(false)
        }
    }

    // Debounced typing indicator updater for current user
    LaunchedEffect(inputText) {
        if (inputText.isNotBlank()) {
            onTyping(true)
            delay(2500)
            onTyping(false)
        } else {
            onTyping(false)
        }
    }

    val otherUsernameHandle = otherUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }?.let { "@$it" }
        ?: otherUserId?.let { "@user_${it.take(6)}" }

    val otherAvailability = otherUser?.resolvedAvailability() ?: AvailabilityStatus.OFFLINE

    val statusSubtitle = if (isGroup) {
        "${chat.memberIds.size} registered members"
    } else {
        val handlePrefix = if (otherUsernameHandle != null) "$otherUsernameHandle • " else ""
        if (otherAvailability != AvailabilityStatus.OFFLINE) {
            "${handlePrefix}${otherAvailability.label}"
        } else {
            val lastSeenDate = otherUser?.lastSeen?.toDate()
            if (lastSeenDate != null) "${handlePrefix}Last seen ${formatMessageTime(lastSeenDate)}" else "${handlePrefix}Offline"
        }
    }

    // Scroll to latest message smoothly on arrival
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showChatDetails = true }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .testTag("chat_header_details_button")
                        ) {
                            UserAvatar(
                                name = title,
                                avatarUrl = otherUser?.avatarUrl,
                                isOnline = isOnline,
                                availabilityStatus = if (isGroup) null else otherAvailability,
                                isGroup = isGroup,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSomeoneTyping) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(SatElectricBlue, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    } else if (!isGroup && otherAvailability != AvailabilityStatus.OFFLINE) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(availabilityColor(otherAvailability), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = if (isSomeoneTyping) typingStatusText else statusSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSomeoneTyping) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSomeoneTyping) SatElectricBlue else if (!isGroup && otherAvailability != AvailabilityStatus.OFFLINE) availabilityColor(otherAvailability) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (isGroup) {
                            IconButton(
                                onClick = { showParticipantsDialog = true },
                                modifier = Modifier.testTag("view_group_participants_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = "View Room Participants",
                                    tint = SatElectricBlue
                                )
                            }
                        }

                        IconButton(
                            onClick = { showRegisteredBar = !showRegisteredBar },
                            modifier = Modifier.testTag("toggle_registered_users_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Show registered colleagues",
                                tint = if (showRegisteredBar) SatElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showMenu = !showMenu },
                            modifier = Modifier.testTag("chat_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isGroup) "Group Details" else "Contact Info") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = SatElectricBlue)
                                },
                                onClick = {
                                    showMenu = false
                                    showChatDetails = true
                                },
                                modifier = Modifier.testTag("menu_chat_details")
                            )

                            if (isGroup && onShareGroupInvite != null) {
                                DropdownMenuItem(
                                    text = { Text("Group Invite Link") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = SatTeal)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onShareGroupInvite()
                                    },
                                    modifier = Modifier.testTag("menu_group_invite_link")
                                )
                            }

                            if (onClearChat != null) {
                                DropdownMenuItem(
                                    text = { Text("Clear Chat") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = SatErrorRed)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showClearChatDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_clear_chat")
                                )
                            }

                            if (onDeleteChat != null) {
                                DropdownMenuItem(
                                    text = { Text(if (isGroup) "Delete Group Room" else "Delete Chat") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = SatErrorRed)
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteChatDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_delete_chat")
                                )
                            }

                            if (!isGroup && otherUserId != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                DropdownMenuItem(
                                    text = { Text("Block Contact") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Block, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onBlockUser(otherUserId, title)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Report to SAT Compliance") },
                                    leadingIcon = {
                                        Icon(imageVector = Icons.Default.Report, contentDescription = null)
                                    },
                                    onClick = {
                                        showMenu = false
                                        onReportUser(otherUserId, title)
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Real-time Registered Colleagues Carousel for Instant Switching
                AnimatedVisibility(visible = showRegisteredBar) {
                    RegisteredColleaguesBar(
                        registeredUsers = registeredUsers,
                        selectedUserId = otherUserId,
                        onSelectUser = onSwitchUser
                    )
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
            ) {
                // Reply Preview Bar
                if (replyingTo != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                tint = SatElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Replying to ${replyingTo.senderName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SatElectricBlue
                                )
                                Text(
                                    text = if (replyingTo.deleted) "Deleted message" else replyingTo.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onSetReplyingTo(null) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Chat Input / Voice Recording / Voice Preview Row
                when {
                    isRecordingAudio -> {
                        VoiceRecordingComposerBar(
                            recordingSeconds = recordingSeconds,
                            amplitudeBars = amplitudeBars,
                            onCancel = {
                                voiceRecorder.cancel()
                                isRecordingAudio = false
                            },
                            onStopToPreview = stopRecordingToPreview,
                            onSendNow = stopAndSendVoiceNow
                        )
                    }
                    recordedPreviewClip != null -> {
                        val previewClip = recordedPreviewClip!!
                        val isPreviewPlaying =
                            audioPlaybackState.activeMessageId == "__preview__" && audioPlaybackState.isPlaying
                        VoicePreviewComposerBar(
                            durationSeconds = previewClip.durationSeconds,
                            isPlaying = isPreviewPlaying,
                            onDiscard = {
                                voicePlayer.stop()
                                recordedPreviewClip = null
                            },
                            onTogglePlay = {
                                voicePlayer.togglePlayPause(
                                    messageId = "__preview__",
                                    attachmentUrl = previewClip.dataUri,
                                    fallbackDurationSec = previewClip.durationSeconds,
                                    scope = coroutineScope
                                )
                            },
                            onSend = { sendRecordedClip(previewClip) }
                        )
                    }
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                IconButton(
                                    onClick = { showAttachmentMenu = !showAttachmentMenu },
                                    modifier = Modifier.testTag("chat_attachment_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Attach file or photo",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                DropdownMenu(
                                    expanded = showAttachmentMenu,
                                    onDismissRequest = { showAttachmentMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Photo & Video") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = SatElectricBlue)
                                        },
                                        onClick = {
                                            showAttachmentMenu = false
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                            )
                                        },
                                        modifier = Modifier.testTag("pick_photo_menu_item")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Document & Files") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = SatTeal)
                                        },
                                        onClick = {
                                            showAttachmentMenu = false
                                            documentPickerLauncher.launch("*/*")
                                        },
                                        modifier = Modifier.testTag("pick_doc_menu_item")
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Record Voice Clip") },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = SatErrorRed)
                                        },
                                        onClick = {
                                            showAttachmentMenu = false
                                            requestOrStartVoiceRecording()
                                        },
                                        modifier = Modifier.testTag("pick_voice_menu_item")
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                modifier = Modifier.testTag("quick_media_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Send Photo or Video",
                                    tint = SatTeal
                                )
                            }

                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = { Text("Type a message to $title...") },
                                maxLines = 4,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SatElectricBlue,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("message_input_field")
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            if (inputText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onSendMessage(inputText, "text", null)
                                        inputText = ""
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .minimumInteractiveComponentSize()
                                        .background(SatElectricBlue, CircleShape)
                                        .testTag("send_message_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send Message",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = requestOrStartVoiceRecording,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .minimumInteractiveComponentSize()
                                        .background(SatTeal, CircleShape)
                                        .testTag("voice_record_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Record voice clip",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = "Start the Conversation",
                    message = "Connected to $title in real time via Cloud Firestore. Send a message to begin.",
                    actionLabel = "Say Hello 👋",
                    onAction = {
                        onSendMessage("Hello! Connecting from SAT Limited.", "text", null)
                    }
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    RealtimeSecurityBadge()
                    Spacer(modifier = Modifier.height(14.dp))
                }

                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        currentUserId = currentUserId,
                        registeredUsers = registeredUsers,
                        isOutgoing = message.senderId == currentUserId,
                        isGroup = isGroup,
                        onReply = { onSetReplyingTo(message) },
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.text))
                            Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { onDeleteMessage(message) },
                        onReport = {
                            onReportUser(message.senderId, message.senderName)
                        },
                        onReact = { emoji -> onReactToMessage(message.id, emoji) },
                        onImageClick = { fullScreenImageUri = it },
                        onVideoClick = { fullScreenVideoUri = it },
                        audioPlaybackState = audioPlaybackState,
                        onToggleAudioPlay = { msg ->
                            msg.attachmentUrl?.let { url ->
                                voicePlayer.togglePlayPause(
                                    messageId = msg.id,
                                    attachmentUrl = url,
                                    fallbackDurationSec = msg.durationSeconds,
                                    scope = coroutineScope
                                )
                            }
                        },
                        onSeekAudio = { msg, posMs ->
                            voicePlayer.seekTo(msg.id, posMs)
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (isSomeoneTyping) {
                    item(key = "typing_bubble") {
                        TypingIndicatorBubble(
                            typingUserNames = typingUsers,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showParticipantsDialog) {
        GroupParticipantsDialog(
            chat = chat,
            registeredUsers = registeredUsers,
            currentUserId = currentUserId,
            onToggleAdmin = onToggleGroupAdmin,
            onDismiss = { showParticipantsDialog = false }
        )
    }

    // Media Preview Dialog Before Sending
    if (showMediaPreviewDialog && pendingMediaUri != null) {
        Dialog(onDismissRequest = { showMediaPreviewDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (pendingMediaType) {
                            "image" -> "Send Photo"
                            "video" -> "Send Video Clip"
                            else -> "Send Document"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    when (pendingMediaType) {
                        "image" -> {
                            AsyncImage(
                                model = pendingMediaUri,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        "video" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0A2540),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Selected Video Clip",
                                        tint = SatTeal,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = pendingMediaName ?: "Video Clip",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    pendingMediaSize?.let {
                                        Text(
                                            text = formatFileSize(it),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SatTeal
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = SatElectricBlue,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = pendingMediaName ?: "Document",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        pendingMediaSize?.let {
                                            Text(
                                                text = formatFileSize(it),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mediaCaption,
                        onValueChange = { mediaCaption = it },
                        placeholder = { Text("Add a caption...") },
                        modifier = Modifier.fillMaxWidth().testTag("media_caption_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showMediaPreviewDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val uri = pendingMediaUri
                                val payload = pendingEncodedPayload
                                if (uri != null || !payload.isNullOrBlank()) {
                                    showMediaPreviewDialog = false
                                    val encoded = payload?.takeIf { it.isNotBlank() } ?: when (pendingMediaType) {
                                        "image" -> processImageUriToBase64(context, uri!!)
                                        else -> uri?.toString().orEmpty()
                                    }
                                    if (onSendMediaMessage != null) {
                                        onSendMediaMessage(
                                            mediaCaption,
                                            pendingMediaType,
                                            encoded,
                                            pendingMediaName,
                                            pendingMediaSize,
                                            null
                                        )
                                    } else {
                                        onSendMessage(mediaCaption, pendingMediaType, encoded)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("confirm_send_media_button")
                        ) {
                            Text("Send")
                        }
                    }
                }
            }
        }
    }

    // Full Screen Image Viewer Modal
    if (fullScreenImageUri != null) {
        Dialog(onDismissRequest = { fullScreenImageUri = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { fullScreenImageUri = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = fullScreenImageUri,
                    contentDescription = "Full Screen Media",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                IconButton(
                    onClick = { fullScreenImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    // Full Screen Video Player Modal
    if (fullScreenVideoUri != null) {
        val playableVideoUri = remember(fullScreenVideoUri) {
            MediaAttachmentHelper.resolvePlayableVideoUri(context, fullScreenVideoUri!!)
        }
        Dialog(onDismissRequest = { fullScreenVideoUri = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f)),
                contentAlignment = Alignment.Center
            ) {
                if (playableVideoUri != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                val controller = MediaController(ctx)
                                controller.setAnchorView(this)
                                setMediaController(controller)
                                setVideoURI(playableVideoUri)
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    )
                } else {
                    Text(
                        text = "Unable to decode video clip for playback.",
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = { fullScreenVideoUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close video", tint = Color.White)
                }
            }
        }
    }

    // Clear Chat Confirmation Dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Chat History?") },
            text = { Text("Are you sure you want to clear all messages in this conversation? Messages will be deleted for all participants.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = false
                        onClearChat?.invoke()
                    }
                ) {
                    Text("Clear", color = SatErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Chat Confirmation Dialog
    if (showDeleteChatDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteChatDialog = false },
            title = { Text(if (isGroup) "Delete Group Room?" else "Delete Chat?") },
            text = { Text("Are you sure you want to delete this ${if (isGroup) "group room" else "chat"}? All messages and records will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteChatDialog = false
                        onDeleteChat?.invoke()
                        onBack()
                    }
                ) {
                    Text("Delete", color = SatErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RegisteredColleaguesBar(
    registeredUsers: List<UserProfile>,
    selectedUserId: String?,
    onSelectUser: (UserProfile) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                text = "REGISTERED ON SAT MESSENGER (${registeredUsers.size})",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(registeredUsers, key = { it.userId }) { user ->
                    val isSelected = user.userId == selectedUserId
                    val nameOrUser = user.displayName.takeIf { it.isNotBlank() }
                        ?: user.email.substringBefore("@").takeIf { it.isNotBlank() }
                        ?: "User"

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) SatElectricBlue.copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .clickable { onSelectUser(user) }
                            .padding(4.dp)
                            .testTag("registered_user_${user.userId}")
                    ) {
                        Box {
                            UserAvatar(
                                name = nameOrUser,
                                avatarUrl = user.avatarUrl,
                                isOnline = user.isOnline,
                                size = 42.dp
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .border(2.dp, SatElectricBlue, CircleShape)
                                        .padding(2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = nameOrUser.split(" ").firstOrNull() ?: nameOrUser,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 60.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}



@Composable
private fun RealtimeSecurityBadge() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚡ Real-Time Cloud Firestore Sync",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Live updates with registered SAT Limited colleagues.",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatMessageTime(date: Date?): String {
    if (date == null) return ""
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}

@Composable
fun TypingIndicatorBubble(
    typingUserNames: List<String>,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF070F1EUL
    val bubbleColor = if (isDark) SatIncomingBubbleDark else SatIncomingBubbleLight

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.testTag("typing_indicator_bubble")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = typingUserNames.firstOrNull()?.let { "$it is typing" } ?: "Typing",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                TypingDotsAnimation()
            }
        }
    }
}

@Composable
fun TypingDotsAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val dots = (0..2).map { index ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -6f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    0f at 0
                    -6f at (index * 150 + 200)
                    0f at (index * 150 + 400)
                    0f at 1000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_$index"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { anim ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer { translationY = anim.value }
                    .background(SatElectricBlue, CircleShape)
            )
        }
    }
}

@Composable
fun GroupParticipantsDialog(
    chat: Chat,
    registeredUsers: List<UserProfile>,
    currentUserId: String,
    onToggleAdmin: ((targetUserId: String, makeAdmin: Boolean) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val participants = remember(chat.memberIds, registeredUsers) {
        chat.memberIds.map { memberId ->
            registeredUsers.firstOrNull { it.userId == memberId }
                ?: UserProfile(
                    userId = memberId,
                    displayName = if (memberId == currentUserId) "You" else chat.participantNames[memberId] ?: "User_${memberId.take(6)}"
                )
        }
    }
    val callerCanManageAdmins = chat.createdBy == currentUserId || chat.adminIds.contains(currentUserId)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = chat.name ?: "Group Chat Room",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${participants.size} Active Members • SAT Limited",
                            style = MaterialTheme.typography.labelSmall,
                            color = SatTeal
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_participants_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    items(participants, key = { it.userId }) { member ->
                        val isSelf = member.userId == currentUserId
                        val isCreator = chat.createdBy.isNotBlank() && chat.createdBy == member.userId
                        val isAdmin = isCreator || chat.adminIds.contains(member.userId)
                        val canToggle = callerCanManageAdmins &&
                            onToggleAdmin != null &&
                            !isCreator &&
                            !(isSelf && chat.adminIds.size <= 1)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            UserAvatar(
                                name = member.displayName,
                                avatarUrl = member.avatarUrl,
                                isOnline = member.isOnline,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = member.displayName + if (isSelf) " (You)" else "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isCreator) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SatTeal.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "CREATOR",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SatTeal,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else if (isAdmin) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SatElectricBlue.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SatElectricBlue,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (member.isOnline) "Active Now" else member.statusMessage ?: member.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (member.isOnline) SatOnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canToggle) {
                                TextButton(
                                    onClick = { onToggleAdmin?.invoke(member.userId, !isAdmin) },
                                    modifier = Modifier.testTag("dialog_toggle_group_admin_${member.userId}")
                                ) {
                                    Text(
                                        text = if (isAdmin) "Dismiss" else "Make Admin",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdmin) SatErrorRed else SatElectricBlue
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("done_participants_button")
                ) {
                    Text("Done")
                }
            }
        }
    }
}
