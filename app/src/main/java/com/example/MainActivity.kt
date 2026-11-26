package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notifications.AppNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.data.repository.AuthUiState
import com.example.ui.screens.ai.SatLimitedAiScreen
import com.example.ui.screens.auth.SignInScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.chats.ChatListScreen
import com.example.ui.screens.channels.ChannelFeedDialog
import com.example.ui.screens.conversation.ConversationScreen
import com.example.ui.screens.dialogs.JoinInviteLinkDialog
import com.example.ui.screens.dialogs.NewChatDialog
import com.example.ui.screens.dialogs.ReportDialog
import com.example.ui.screens.dialogs.ShareInviteLinkDialog
import com.example.data.repository.AppUpdateUiState
import com.example.ui.screens.manual.AppManualScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.UpdatePromptDialog
import com.example.ui.screens.search.UserSearchDialog
import com.example.ui.theme.SATMessengerTheme
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import com.example.ui.viewmodel.MessengerViewModel
import com.example.ui.viewmodel.ScreenState
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val pendingNotificationChatId = MutableStateFlow<String?>(null)
    private val pendingDeepLinkUri = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        pendingNotificationChatId.value = intent?.getStringExtra(AppNotificationManager.EXTRA_CHAT_ID)
        pendingDeepLinkUri.value = intent?.data?.toString()

        setContent {
            SATMessengerTheme {
                val context = LocalContext.current
                val viewModel: MessengerViewModel = viewModel {
                    MessengerViewModel(context.applicationContext)
                }

                val authState by viewModel.authState.collectAsState()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val activeChat by viewModel.activeChat.collectAsState()
                val userProfile by viewModel.userProfile.collectAsState()
                val chats by viewModel.chats.collectAsState()
                val messages by viewModel.messages.collectAsState()
                val directoryUsers by viewModel.directoryUsers.collectAsState()
                val savedContacts by viewModel.savedContacts.collectAsState()
                val blockedUsers by viewModel.blockedUsers.collectAsState()
                val privacySettings by viewModel.privacySettings.collectAsState()
                val replyingTo by viewModel.replyingTo.collectAsState()
                val statuses by viewModel.statuses.collectAsState()
                val channels by viewModel.channels.collectAsState()
                val communities by viewModel.communities.collectAsState()
                val activeChannel by viewModel.activeChannel.collectAsState()
                val channelPosts by viewModel.channelPosts.collectAsState()
                val unreadCounts by viewModel.unreadCounts.collectAsState()
                val totalUnreadCount by viewModel.totalUnreadCount.collectAsState()
                val inAppNotification by viewModel.inAppNotification.collectAsState()
                val showUserSearchDialog by viewModel.showUserSearchDialog.collectAsState()
                val showNewChatDialog by viewModel.showNewChatDialog.collectAsState()
                val showReportDialog by viewModel.showReportDialog.collectAsState()
                val reportedTarget by viewModel.reportedTarget.collectAsState()
                val toastMessage by viewModel.toastMessage.collectAsState()
                val systemNotificationsEnabled by viewModel.systemNotificationsEnabled.collectAsState()
                val intentChatId by pendingNotificationChatId.collectAsState()
                val intentDeepLink by pendingDeepLinkUri.collectAsState()
                val activeInviteDialogLink by viewModel.activeInviteDialogLink.collectAsState()
                val showJoinInviteDialog by viewModel.showJoinInviteDialog.collectAsState()
                val joinInvitePrefill by viewModel.joinInvitePrefill.collectAsState()
                val pendingInvitePreview by viewModel.pendingInvitePreview.collectAsState()
                val inviteLookupError by viewModel.inviteLookupError.collectAsState()
                val isResolvingInvite by viewModel.isResolvingInvite.collectAsState()
                val isJoiningInvite by viewModel.isJoiningInvite.collectAsState()
                val satAiMessages by viewModel.satAiMessages.collectAsState()
                val satAiUiState by viewModel.satAiUiState.collectAsState()
                val satAiMode by viewModel.satAiMode.collectAsState()
                val satAiWebGroundingEnabled by viewModel.satAiWebGroundingEnabled.collectAsState()
                val appUpdateState by viewModel.appUpdateState.collectAsState()
                val showUpdatePromptDialog by viewModel.showUpdatePromptDialog.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }
                val lifecycleOwner = LocalLifecycleOwner.current
                val notificationPrefs = remember {
                    context.getSharedPreferences("sat_notification_prefs", Context.MODE_PRIVATE)
                }

                var hasNotificationPermission by remember {
                    mutableStateOf(
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                var notificationBannerDismissed by remember { mutableStateOf(false) }
                var showPreAuthManual by remember { mutableStateOf(false) }

                val openAppNotificationSettings: () -> Unit = {
                    val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(settingsIntent)
                }

                val openExternalUrl: (String) -> Unit = { url ->
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }.onFailure {
                        Toast.makeText(
                            context,
                            "Unable to open link in browser.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    if (granted) {
                        notificationBannerDismissed = true
                        viewModel.setSystemNotificationsEnabled(true)
                    } else {
                        Toast.makeText(
                            context,
                            "Notification permission denied. You can enable it anytime in Profile or Settings.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                val requestNotificationPermission: () -> Unit = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val alreadyAsked = notificationPrefs.getBoolean("asked_post_notifications", false)
                        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                        if (!alreadyAsked || shouldShowRationale) {
                            notificationPrefs.edit().putBoolean("asked_post_notifications", true).apply()
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openAppNotificationSettings()
                        }
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_START -> viewModel.setAppInForeground(true)
                            Lifecycle.Event.ON_STOP -> viewModel.setAppInForeground(false)
                            Lifecycle.Event.ON_RESUME -> {
                                hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                            }
                            else -> Unit
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(authState) {
                    if (authState is AuthUiState.Authenticated) {
                        showPreAuthManual = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !hasNotificationPermission &&
                            !notificationPrefs.getBoolean("asked_post_notifications", false)
                        ) {
                            notificationPrefs.edit().putBoolean("asked_post_notifications", true).apply()
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                LaunchedEffect(intentChatId, authState) {
                    val targetId = intentChatId
                    if (targetId != null && authState is AuthUiState.Authenticated) {
                        viewModel.openNotificationChat(targetId)
                        pendingNotificationChatId.value = null
                    }
                }

                LaunchedEffect(intentDeepLink) {
                    val deepUri = intentDeepLink
                    if (!deepUri.isNullOrBlank()) {
                        viewModel.handleDeepLinkUri(deepUri)
                        pendingDeepLinkUri.value = null
                    }
                }

                LaunchedEffect(toastMessage) {
                    toastMessage?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearToast()
                    }
                }

                if (authState is AuthUiState.Authenticated) {
                    val uid = (authState as AuthUiState.Authenticated).user.uid

                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                ScreenState.CHATS -> {
                                    ChatListScreen(
                                        currentUserId = uid,
                                        userProfile = userProfile,
                                        chats = chats,
                                        directoryUsers = directoryUsers,
                                        savedContacts = savedContacts,
                                        statuses = statuses,
                                        channels = channels,
                                        communities = communities,
                                        unreadCounts = unreadCounts,
                                        totalUnreadCount = totalUnreadCount,
                                        onChatClick = { chat -> viewModel.openChat(chat) },
                                        onDeleteChat = { chatId -> viewModel.deleteChat(chatId) },
                                        onClearChat = { chatId -> viewModel.clearChat(chatId) },
                                        onProfileClick = { viewModel.navigateTo(ScreenState.PROFILE) },
                                        onNewChatClick = { viewModel.openNewChatDialog() },
                                        onManualClick = { viewModel.navigateTo(ScreenState.MANUAL) },
                                        onPostStatus = { text, mediaUrl, bgColor ->
                                            viewModel.postStatus(text, mediaUrl, bgColor)
                                        },
                                        onDeleteStatus = { statusId ->
                                            viewModel.deleteStatus(statusId)
                                        },
                                        onMarkStatusViewed = { statusId ->
                                            viewModel.markStatusViewed(statusId)
                                        },
                                        onReplyToStatus = { authorId, text ->
                                            viewModel.replyToStatus(authorId, text)
                                        },
                                        onToggleFollowChannel = { channel ->
                                            viewModel.toggleFollowChannel(channel)
                                        },
                                        onCreateChannel = { name, desc, iconUrl, category, isPublic, allowFollowerPosts, allowReactions ->
                                            viewModel.createChannel(
                                                name = name,
                                                description = desc,
                                                iconUrl = iconUrl,
                                                category = category,
                                                isPublic = isPublic,
                                                allowFollowerPosts = allowFollowerPosts,
                                                allowReactions = allowReactions
                                            )
                                        },
                                        onOpenChannel = { channel ->
                                            viewModel.openChannel(channel)
                                        },
                                        onShareChannelInvite = { channel ->
                                            viewModel.openShareInviteDialog(
                                                targetType = "channel",
                                                targetId = channel.id,
                                                targetName = channel.name,
                                                targetDescription = channel.description,
                                                existingCode = channel.inviteCode
                                            )
                                        },
                                        onCreateCommunity = { name, desc, subgroups ->
                                            viewModel.createCommunity(name, desc, subgroups)
                                        },
                                        onJoinCommunity = { commId ->
                                            viewModel.joinCommunity(commId)
                                        },
                                        onShareCommunityInvite = { community ->
                                            viewModel.openShareInviteDialog(
                                                targetType = "community",
                                                targetId = community.id,
                                                targetName = community.name,
                                                targetDescription = community.description,
                                                existingCode = community.inviteCode
                                            )
                                        },
                                        onOpenJoinInvite = {
                                            viewModel.openJoinInviteDialog()
                                        },
                                        onOpenUserSearch = {
                                            viewModel.openUserSearchDialog()
                                        },
                                        onDirectMessageUser = { targetUser ->
                                            viewModel.openDirectChatWithUser(targetUser)
                                        },
                                        onUpdateAvailability = { status ->
                                            viewModel.updateAvailabilityStatus(status)
                                        },
                                        onToggleFriendContact = { targetUser ->
                                            viewModel.toggleFriendContact(targetUser)
                                        },
                                        onOpenSatAi = {
                                            viewModel.openSatAi()
                                        },
                                        onToggleCommunityAdmin = { communityId, targetUserId, makeAdmin ->
                                            viewModel.setCommunityAdminRole(communityId, targetUserId, makeAdmin)
                                        },
                                        showNotificationPermissionBanner = !hasNotificationPermission && !notificationBannerDismissed,
                                        onEnableNotificationsClick = requestNotificationPermission,
                                        onDismissNotificationBanner = { notificationBannerDismissed = true }
                                    )
                                }

                                ScreenState.CONVERSATION -> {
                                    activeChat?.let { chat ->
                                        ChatScreen(
                                            chat = chat,
                                            currentUserId = uid,
                                            messages = messages,
                                            registeredUsers = directoryUsers,
                                            replyingTo = replyingTo,
                                            onBack = { viewModel.leaveChat() },
                                            onSendMessage = { text, type, attachmentUrl ->
                                                viewModel.sendMessage(text, type, attachmentUrl)
                                            },
                                            onSendMediaMessage = { caption, type, attachmentUrl, fileName, fileSize, durationSeconds ->
                                                viewModel.sendMediaMessage(caption, attachmentUrl, type, fileName, fileSize, durationSeconds)
                                            },
                                            onClearChat = { viewModel.clearChat(chat.id) },
                                            onDeleteChat = { viewModel.deleteChat(chat.id) },
                                            onSwitchUser = { targetUser ->
                                                viewModel.openDirectChatWithUser(targetUser)
                                            },
                                            onSetReplyingTo = { viewModel.setReplyingTo(it) },
                                            onDeleteMessage = { viewModel.deleteMessage(it) },
                                            onBlockUser = { targetId, targetName ->
                                                viewModel.blockUser(targetId, targetName)
                                            },
                                            onReportUser = { targetId, targetName ->
                                                viewModel.openReportDialog(targetId, targetName)
                                            },
                                            onTyping = { isTyping ->
                                                viewModel.setTyping(isTyping)
                                            },
                                            onReactToMessage = { messageId, emoji ->
                                                viewModel.reactToMessage(messageId, emoji)
                                            },
                                            onShareGroupInvite = if (chat.type == "group") {
                                                {
                                                    viewModel.openShareInviteDialog(
                                                        targetType = "group",
                                                        targetId = chat.id,
                                                        targetName = chat.name ?: "Group Room",
                                                        targetDescription = chat.description,
                                                        existingCode = chat.inviteCode
                                                    )
                                                }
                                            } else null,
                                            onToggleGroupAdmin = if (chat.type == "group") {
                                                { targetUserId, makeAdmin ->
                                                    viewModel.setGroupAdminRole(chat.id, targetUserId, makeAdmin)
                                                }
                                            } else null
                                        )
                                    } ?: run {
                                        viewModel.navigateTo(ScreenState.CHATS)
                                    }
                                }

                                ScreenState.PROFILE -> {
                                    ProfileScreen(
                                        userProfile = userProfile,
                                        privacySettings = privacySettings,
                                        blockedUsers = blockedUsers,
                                        onBack = { viewModel.navigateTo(ScreenState.CHATS) },
                                        onUpdateProfile = { name, status, phone ->
                                            viewModel.updateProfile(name, status, phone)
                                        },
                                        onUpdateAvailability = { status ->
                                            viewModel.updateAvailabilityStatus(status)
                                        },
                                        onUpdatePrivacy = { readReceipts, lastSeenVisible ->
                                            viewModel.updatePrivacy(readReceipts, lastSeenVisible)
                                        },
                                        onUnblockUser = { blockedId ->
                                            viewModel.unblockUser(blockedId)
                                        },
                                        onSignOut = { viewModel.signOut() },
                                        onDeleteAccount = { viewModel.deleteAccount() },
                                        onManualClick = { viewModel.navigateTo(ScreenState.MANUAL) },
                                        systemNotificationsEnabled = systemNotificationsEnabled,
                                        hasNotificationPermission = hasNotificationPermission,
                                        onToggleSystemNotifications = { enabled ->
                                            viewModel.setSystemNotificationsEnabled(enabled)
                                        },
                                        onRequestNotificationPermission = requestNotificationPermission,
                                        onSendTestNotification = {
                                            if (hasNotificationPermission) {
                                                viewModel.sendTestSystemNotification()
                                            } else {
                                                requestNotificationPermission()
                                            }
                                        },
                                        appUpdateState = appUpdateState,
                                        onCheckForUpdates = { viewModel.checkForAppUpdates(userInitiated = true) },
                                        onOpenUpdatePrompt = { viewModel.openUpdatePromptDialog() },
                                        onDownloadUpdate = { release ->
                                            openExternalUrl(release.apkDownloadUrl ?: release.releasePageUrl)
                                        }
                                    )
                                }

                                ScreenState.MANUAL -> {
                                    AppManualScreen(
                                        onBack = { viewModel.navigateTo(ScreenState.CHATS) }
                                    )
                                }

                                ScreenState.SAT_AI -> {
                                    SatLimitedAiScreen(
                                        messages = satAiMessages,
                                        uiState = satAiUiState,
                                        selectedMode = satAiMode,
                                        webGroundingEnabled = satAiWebGroundingEnabled,
                                        onBack = { viewModel.navigateTo(ScreenState.CHATS) },
                                        onSendPrompt = { prompt, bitmap, previewUri ->
                                            viewModel.sendSatAiPrompt(prompt, bitmap, previewUri)
                                        },
                                        onSelectMode = { mode -> viewModel.setSatAiMode(mode) },
                                        onToggleWebGrounding = { enabled -> viewModel.setSatAiWebGrounding(enabled) },
                                        onClearConversation = { viewModel.clearSatAiConversation() },
                                        onDismissError = { viewModel.dismissSatAiError() }
                                    )
                                }
                            }

                            // Modals
                            if (showUserSearchDialog) {
                                UserSearchDialog(
                                    users = directoryUsers,
                                    currentUserId = uid,
                                    onDismiss = { viewModel.closeUserSearchDialog() },
                                    onSelectUser = { targetUser ->
                                        viewModel.closeUserSearchDialog()
                                        viewModel.openDirectChatWithUser(targetUser)
                                    }
                                )
                            }

                            activeChannel?.let { channel ->
                                ChannelFeedDialog(
                                    channel = channel,
                                    currentUserId = uid,
                                    posts = channelPosts,
                                    directoryUsers = directoryUsers,
                                    onDismiss = { viewModel.closeChannel() },
                                    onToggleFollow = { viewModel.toggleFollowChannel(it) },
                                    onPostMessage = { text, mediaUrl ->
                                        viewModel.postToChannel(text, mediaUrl)
                                    },
                                    onDeletePost = { postId ->
                                        viewModel.deleteChannelPost(channel.id, postId)
                                    },
                                    onUpdateChannelSettings = { name, desc, cat, isPublic, allowFollowerPosts, allowReactions ->
                                        viewModel.updateChannelSettings(
                                            channelId = channel.id,
                                            name = name,
                                            description = desc,
                                            category = cat,
                                            isPublic = isPublic,
                                            allowFollowerPosts = allowFollowerPosts,
                                            allowReactions = allowReactions,
                                            inviteCode = channel.inviteCode
                                        )
                                    },
                                    onToggleChannelAdmin = { targetUserId, makeAdmin ->
                                        viewModel.setChannelAdminRole(channel.id, targetUserId, makeAdmin)
                                    },
                                    onDeleteChannel = {
                                        viewModel.deleteChannel(channel)
                                    },
                                    onShareInvite = {
                                        viewModel.openShareInviteDialog(
                                            targetType = "channel",
                                            targetId = channel.id,
                                            targetName = channel.name,
                                            targetDescription = channel.description,
                                            existingCode = channel.inviteCode
                                        )
                                    },
                                    onReact = { postId, reactions, emoji ->
                                        viewModel.reactToChannelPost(postId, reactions, emoji)
                                    }
                                )
                            }

                            if (showNewChatDialog) {
                                NewChatDialog(
                                    users = directoryUsers,
                                    onDismiss = { viewModel.closeNewChatDialog() },
                                    onSelectUser = { targetUser ->
                                        viewModel.openDirectChatWithUser(targetUser)
                                    },
                                    onCreateGroup = { groupName, memberIds, description ->
                                        viewModel.createGroupChat(groupName, memberIds, description)
                                    },
                                    onOpenJoinInvite = {
                                        viewModel.openJoinInviteDialog()
                                    }
                                )
                            }

                            if (showReportDialog && reportedTarget != null) {
                                ReportDialog(
                                    reportedUserName = reportedTarget!!.second,
                                    onDismiss = { viewModel.closeReportDialog() },
                                    onSubmit = { reason, details ->
                                        viewModel.submitReport(reason, details)
                                    }
                                )
                            }

                            activeInviteDialogLink?.let { invite ->
                                val canManage = invite.creatorId == uid
                                ShareInviteLinkDialog(
                                    invite = invite,
                                    canManage = canManage,
                                    onDismiss = { viewModel.closeShareInviteDialog() },
                                    onRegenerate = { viewModel.regenerateInviteLink(invite) }
                                )
                            }

                            if (showJoinInviteDialog) {
                                JoinInviteLinkDialog(
                                    initialInput = joinInvitePrefill,
                                    previewInvite = pendingInvitePreview,
                                    lookupError = inviteLookupError,
                                    isResolving = isResolvingInvite,
                                    isJoining = isJoiningInvite,
                                    onDismiss = { viewModel.closeJoinInviteDialog() },
                                    onLookup = { viewModel.lookupInviteCode(it) },
                                    onConfirmJoin = { viewModel.confirmJoinInvite(it) }
                                )
                            }

                            val availableUpdate = (appUpdateState as? AppUpdateUiState.UpdateAvailable)?.latestRelease
                            if (showUpdatePromptDialog && availableUpdate != null) {
                                UpdatePromptDialog(
                                    installedVersion = appUpdateState.installedVersion,
                                    release = availableUpdate,
                                    onDownloadUpdate = { release ->
                                        viewModel.dismissUpdatePromptDialog(rememberForThisVersion = false)
                                        openExternalUrl(release.apkDownloadUrl ?: release.releasePageUrl)
                                    },
                                    onOpenGitHubRelease = { release ->
                                        viewModel.dismissUpdatePromptDialog(rememberForThisVersion = false)
                                        openExternalUrl(release.releasePageUrl)
                                    },
                                    onDismiss = { rememberForVersion ->
                                        viewModel.dismissUpdatePromptDialog(rememberForThisVersion = rememberForVersion)
                                    }
                                )
                            }

                            // WhatsApp-style In-App Incoming Message Notification Banner
                            AnimatedVisibility(
                                visible = inAppNotification != null,
                                enter = slideInVertically(initialOffsetY = { -it }),
                                exit = slideOutVertically(targetOffsetY = { -it }),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                inAppNotification?.let { notif ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.openNotificationChat(notif.first) }
                                            .testTag("in_app_notification_banner")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Message,
                                                contentDescription = null,
                                                tint = SatTeal,
                                                modifier = Modifier.padding(end = 10.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = notif.second,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SatTeal
                                                )
                                                Text(
                                                    text = notif.third,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(onClick = { viewModel.dismissNotification() }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Dismiss notification",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (showPreAuthManual) {
                        AppManualScreen(
                            onBack = { showPreAuthManual = false }
                        )
                    } else {
                        SignInScreen(
                            authState = authState,
                            onGoogleSignInClick = { activity ->
                                viewModel.signInWithGoogle(activity)
                            },
                            onDismissError = {
                                viewModel.clearAuthError()
                            },
                            onOpenManual = {
                                showPreAuthManual = true
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val chatId = intent.getStringExtra(AppNotificationManager.EXTRA_CHAT_ID)
        if (!chatId.isNullOrBlank()) {
            pendingNotificationChatId.value = chatId
        }
        val deepUri = intent.data?.toString()
        if (!deepUri.isNullOrBlank()) {
            pendingDeepLinkUri.value = deepUri
        }
    }
}
