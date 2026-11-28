package com.example.ui.viewmodel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AvailabilityStatus
import com.example.data.model.BlockedUser
import com.example.data.model.Channel
import com.example.data.model.ChannelPost
import com.example.data.model.Chat
import com.example.data.model.Community
import com.example.data.model.ContactEntry
import com.example.data.model.FriendRequest
import com.example.data.model.InviteLink
import com.example.data.model.Message
import com.example.data.model.PrivacySettings
import com.example.data.model.StatusItem
import com.example.data.model.UserProfile
import com.example.BuildConfig
import com.example.data.repository.AppUpdateRepository
import com.example.data.repository.AppUpdateUiState
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthUiState
import com.example.data.repository.ChatRepository
import com.example.data.repository.DeviceContactInfo
import com.example.data.repository.DeviceContactMatch
import com.example.data.repository.DeviceContactsRepository
import com.example.data.repository.InviteRepository
import com.example.data.repository.SatAiMessage
import com.example.data.repository.SatAiMode
import com.example.data.repository.SatAiRepository
import com.example.data.repository.SatAiUiState
import com.example.data.repository.SocialFeaturesRepository
import com.example.data.repository.UserRepository
import com.example.data.service.ContactDiscoveryService
import com.example.data.service.FindFriendsDirectoryState
import com.example.notifications.AppNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ScreenState {
    CHATS,
    CONVERSATION,
    PROFILE,
    MANUAL,
    SAT_AI,
    FIND_FRIENDS
}

class MessengerViewModel(
    context: Context,
    private val authRepository: AuthRepository = AuthRepository(context),
    private val userRepository: UserRepository = UserRepository(context),
    private val chatRepository: ChatRepository = ChatRepository(context),
    private val socialRepository: SocialFeaturesRepository = SocialFeaturesRepository(context),
    private val inviteRepository: InviteRepository = InviteRepository(context),
    private val appNotificationManager: AppNotificationManager = AppNotificationManager(context),
    private val satAiRepository: SatAiRepository = SatAiRepository(),
    private val appUpdateRepository: AppUpdateRepository = AppUpdateRepository(),
    private val deviceContactsRepository: DeviceContactsRepository = DeviceContactsRepository(),
    private val contactDiscoveryService: ContactDiscoveryService = ContactDiscoveryService()
) : ViewModel() {

    private val appContext = context.applicationContext
    private val notificationPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val authState: StateFlow<AuthUiState> = authRepository.authState

    private val _currentScreen = MutableStateFlow(ScreenState.CHATS)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    private val _activeChat = MutableStateFlow<Chat?>(null)
    val activeChat: StateFlow<Chat?> = _activeChat.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _directoryUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    val directoryUsers: StateFlow<List<UserProfile>> = _directoryUsers.asStateFlow()

    private val _savedContacts = MutableStateFlow<List<ContactEntry>>(emptyList())
    val savedContacts: StateFlow<List<ContactEntry>> = _savedContacts.asStateFlow()

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests.asStateFlow()

    private val _deviceContactMatches = MutableStateFlow<Map<String, DeviceContactMatch>>(emptyMap())
    val deviceContactMatches: StateFlow<Map<String, DeviceContactMatch>> = _deviceContactMatches.asStateFlow()

    private val _isSyncingDeviceContacts = MutableStateFlow(false)
    val isSyncingDeviceContacts: StateFlow<Boolean> = _isSyncingDeviceContacts.asStateFlow()

    private val _hasDeviceContactsPermission = MutableStateFlow(
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    )
    val hasDeviceContactsPermission: StateFlow<Boolean> = _hasDeviceContactsPermission.asStateFlow()

    private val _deviceContactsScannedCount = MutableStateFlow(0)
    val deviceContactsScannedCount: StateFlow<Int> = _deviceContactsScannedCount.asStateFlow()

    val findFriendsDirectoryState: StateFlow<FindFriendsDirectoryState> =
        contactDiscoveryService.directoryState

    private val _secondaryUsernameFeedback = MutableStateFlow<Pair<Boolean, String>?>(null)
    val secondaryUsernameFeedback: StateFlow<Pair<Boolean, String>?> = _secondaryUsernameFeedback.asStateFlow()

    private val _statuses = MutableStateFlow<List<StatusItem>>(emptyList())
    val statuses: StateFlow<List<StatusItem>> = _statuses.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _communities = MutableStateFlow<List<Community>>(emptyList())
    val communities: StateFlow<List<Community>> = _communities.asStateFlow()

    private val _activeChannel = MutableStateFlow<Channel?>(null)
    val activeChannel: StateFlow<Channel?> = _activeChannel.asStateFlow()

    private val _channelPosts = MutableStateFlow<List<ChannelPost>>(emptyList())
    val channelPosts: StateFlow<List<ChannelPost>> = _channelPosts.asStateFlow()

    private val _showUserSearchDialog = MutableStateFlow(false)
    val showUserSearchDialog: StateFlow<Boolean> = _showUserSearchDialog.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<BlockedUser>>(emptyList())
    val blockedUsers: StateFlow<List<BlockedUser>> = _blockedUsers.asStateFlow()

    private val _privacySettings = MutableStateFlow(PrivacySettings())
    val privacySettings: StateFlow<PrivacySettings> = _privacySettings.asStateFlow()

    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dialog flags
    private val _showNewChatDialog = MutableStateFlow(false)
    val showNewChatDialog: StateFlow<Boolean> = _showNewChatDialog.asStateFlow()

    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog: StateFlow<Boolean> = _showReportDialog.asStateFlow()

    private val _reportedTarget = MutableStateFlow<Pair<String, String>?>(null)
    val reportedTarget: StateFlow<Pair<String, String>?> = _reportedTarget.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _unreadCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val unreadCounts: StateFlow<Map<String, Int>> = _unreadCounts.asStateFlow()

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    private val _inAppNotification = MutableStateFlow<Triple<String, String, String>?>(null) // (chatId, senderName, previewText)
    val inAppNotification: StateFlow<Triple<String, String, String>?> = _inAppNotification.asStateFlow()

    private val _systemNotificationsEnabled = MutableStateFlow(
        notificationPrefs.getBoolean(KEY_SYSTEM_NOTIFICATIONS_ENABLED, true)
    )
    val systemNotificationsEnabled: StateFlow<Boolean> = _systemNotificationsEnabled.asStateFlow()

    private val _isAppInForeground = MutableStateFlow(true)
    private val _pendingNotificationChatId = MutableStateFlow<String?>(null)
    private val _pendingDeepLinkInviteCode = MutableStateFlow<String?>(null)
    private val notifiedMessageIds = mutableSetOf<String>()

    // Invite link states
    private val _activeInviteDialogLink = MutableStateFlow<InviteLink?>(null)
    val activeInviteDialogLink: StateFlow<InviteLink?> = _activeInviteDialogLink.asStateFlow()

    private val _showJoinInviteDialog = MutableStateFlow(false)
    val showJoinInviteDialog: StateFlow<Boolean> = _showJoinInviteDialog.asStateFlow()

    private val _joinInvitePrefill = MutableStateFlow("")
    val joinInvitePrefill: StateFlow<String> = _joinInvitePrefill.asStateFlow()

    private val _pendingInvitePreview = MutableStateFlow<InviteLink?>(null)
    val pendingInvitePreview: StateFlow<InviteLink?> = _pendingInvitePreview.asStateFlow()

    private val _inviteLookupError = MutableStateFlow<String?>(null)
    val inviteLookupError: StateFlow<String?> = _inviteLookupError.asStateFlow()

    private val _isResolvingInvite = MutableStateFlow(false)
    val isResolvingInvite: StateFlow<Boolean> = _isResolvingInvite.asStateFlow()

    private val _isJoiningInvite = MutableStateFlow(false)
    val isJoiningInvite: StateFlow<Boolean> = _isJoiningInvite.asStateFlow()

    // SAT Limited AI states
    private val _satAiMessages = MutableStateFlow<List<SatAiMessage>>(emptyList())
    val satAiMessages: StateFlow<List<SatAiMessage>> = _satAiMessages.asStateFlow()

    private val _satAiUiState = MutableStateFlow<SatAiUiState>(SatAiUiState.Idle)
    val satAiUiState: StateFlow<SatAiUiState> = _satAiUiState.asStateFlow()

    private val _satAiMode = MutableStateFlow(SatAiMode.SMART)
    val satAiMode: StateFlow<SatAiMode> = _satAiMode.asStateFlow()

    private val _satAiWebGroundingEnabled = MutableStateFlow(true)
    val satAiWebGroundingEnabled: StateFlow<Boolean> = _satAiWebGroundingEnabled.asStateFlow()

    // App Version Check & Update states
    private val _appUpdateState = MutableStateFlow<AppUpdateUiState>(
        AppUpdateUiState.Idle(BuildConfig.VERSION_NAME)
    )
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()

    private val _showUpdatePromptDialog = MutableStateFlow(false)
    val showUpdatePromptDialog: StateFlow<Boolean> = _showUpdatePromptDialog.asStateFlow()

    private var messagesJob: Job? = null
    private var chatsJob: Job? = null
    private var profileJob: Job? = null
    private var activeChatJob: Job? = null
    private var satAiJob: Job? = null
    private var updateCheckJob: Job? = null
    private var friendRequestsJob: Job? = null
    private val chatMessageListeners = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            authRepository.attemptAutoSignIn(viewModelScope)
        }

        viewModelScope.launch {
            authState.collectLatest { state ->
                if (state is AuthUiState.Authenticated) {
                    onUserAuthenticated(state.user)
                } else {
                    onUserSignedOut()
                }
            }
        }
    }

    private fun onUserAuthenticated(user: FirebaseUser) {
        val uid = user.uid
        val resolvedEmail = user.email?.takeIf { it.isNotBlank() }
            ?: user.providerData.firstNotNullOfOrNull { it.email?.takeIf { e -> e.isNotBlank() } }
            ?: ""
        val displayName = user.displayName?.takeIf { it.isNotBlank() }
            ?: resolvedEmail.substringBefore("@").takeIf { it.isNotBlank() }
            ?: "SAT Member"

        // Setup or sync profile presence in Firestore
        viewModelScope.launch {
            userRepository.ensureProfileSynced(
                UserProfile(
                    userId = uid,
                    displayName = displayName,
                    email = resolvedEmail,
                    avatarUrl = user.photoUrl?.toString(),
                    availabilityStatus = AvailabilityStatus.AVAILABLE.label,
                    isOnline = true
                )
            )
        }

        if (_appUpdateState.value is AppUpdateUiState.Idle) {
            checkForAppUpdates(userInitiated = false)
        }

        // Observe own profile
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            userRepository.observeUserProfile(uid).collectLatest { profile ->
                _userProfile.value = profile
            }
        }

        // Observe chats
        chatsJob?.cancel()
        chatsJob = viewModelScope.launch {
            chatRepository.observeUserChats(uid).collectLatest { list ->
                _chats.value = list
                val activeId = _activeChat.value?.id
                if (activeId != null) {
                    list.firstOrNull { it.id == activeId }?.let { updated ->
                        _activeChat.value = updated
                    }
                }

                // Open pending chat from notification tap if available
                val pendingId = _pendingNotificationChatId.value
                if (pendingId != null) {
                    val target = list.firstOrNull { it.id == pendingId }
                    if (target != null) {
                        _pendingNotificationChatId.value = null
                        openChat(target)
                    }
                }

                // Manage unread message listeners
                val currentIds = list.map { it.id }.toSet()
                val removed = chatMessageListeners.keys - currentIds
                removed.forEach { id ->
                    chatMessageListeners[id]?.cancel()
                    chatMessageListeners.remove(id)
                    _unreadCounts.update { it - id }
                }

                for (chat in list) {
                    if (!chatMessageListeners.containsKey(chat.id)) {
                        chatMessageListeners[chat.id] = viewModelScope.launch {
                            var isInitialEmission = true
                            chatRepository.observeMessages(chat.id).collectLatest { msgs ->
                                val unread = msgs.count { it.senderId != uid && it.readBy[uid] != true && !it.deleted }
                                _unreadCounts.update { currentMap ->
                                    val updated = currentMap + (chat.id to unread)
                                    _totalUnreadCount.value = updated.values.sum()
                                    updated
                                }

                                val latest = msgs.lastOrNull()
                                val isRecent = latest?.createdAt == null ||
                                    (System.currentTimeMillis() - latest.createdAt.toDate().time) < 60_000L

                                if (isInitialEmission) {
                                    isInitialEmission = false
                                    val toSeed = if (isRecent && latest != null) msgs.dropLast(1) else msgs
                                    notifiedMessageIds.addAll(toSeed.map { it.id })
                                }

                                val shouldNotifyChat = !_isAppInForeground.value || _activeChat.value?.id != chat.id

                                if (latest != null &&
                                    latest.senderId != uid &&
                                    latest.readBy[uid] != true &&
                                    !latest.deleted &&
                                    shouldNotifyChat &&
                                    notifiedMessageIds.add(latest.id)
                                ) {
                                    val preview = when (latest.type) {
                                        "image" -> if (latest.text.isNotBlank()) "📷 ${latest.text}" else "📷 Photo"
                                        "video" -> if (latest.text.isNotBlank()) "🎥 ${latest.text}" else "🎥 Video clip"
                                        "file" -> "📎 ${latest.fileName ?: "Document"}"
                                        "audio" -> "🎤 Voice message"
                                        else -> latest.text
                                    }
                                    val resolvedSender = latest.senderName.takeIf { it.isNotBlank() }
                                        ?: _directoryUsers.value.firstOrNull { it.userId == latest.senderId }?.username
                                        ?: "Colleague"

                                    if (_isAppInForeground.value && _activeChat.value?.id != chat.id) {
                                        _inAppNotification.value = Triple(chat.id, resolvedSender, preview)
                                    }

                                    if (_systemNotificationsEnabled.value) {
                                        val chatTitle = chat.getDisplayNameOrUsername(
                                            currentUserId = uid,
                                            directoryUsers = _directoryUsers.value
                                        )
                                        appNotificationManager.showMessageNotification(
                                            chatId = chat.id,
                                            chatTitle = chatTitle,
                                            senderName = resolvedSender,
                                            messageText = preview,
                                            isGroup = chat.type == "group"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Observe colleague & friend directory
        viewModelScope.launch {
            userRepository.searchUsers("").collectLatest { users ->
                val filtered = users.filter { it.userId != uid }
                _directoryUsers.value = filtered
                val recorrelated = contactDiscoveryService.recorrelateWithRegisteredUsers(
                    registeredUsers = filtered,
                    currentUserId = uid
                )
                if (recorrelated.isNotEmpty() || _deviceContactMatches.value.isNotEmpty()) {
                    _deviceContactMatches.value = recorrelated.associate { it.user.userId to it.toDeviceContactMatch() }
                }
            }
        }

        // Observe saved contacts (friends / pinned colleagues)
        viewModelScope.launch {
            userRepository.observeSavedContacts(uid).collectLatest { contacts ->
                _savedContacts.value = contacts
            }
        }

        // Observe friend requests (incoming, outgoing, accepted)
        friendRequestsJob?.cancel()
        friendRequestsJob = viewModelScope.launch {
            userRepository.observeFriendRequests(uid).collectLatest { requests ->
                _friendRequests.value = requests
            }
        }

        // Auto-sync device contacts quietly if permission is already granted
        val contactsPermGranted = contactDiscoveryService.hasReadContactsPermission(appContext)
        _hasDeviceContactsPermission.value = contactsPermGranted
        if (contactsPermGranted) {
            contactDiscoveryService.startObservingContacts(appContext) {
                syncDeviceContacts(appContext, silent = true)
            }
            syncDeviceContacts(appContext, silent = true)
        }

        // Observe privacy settings
        viewModelScope.launch {
            userRepository.observePrivacySettings(uid).collectLatest { settings ->
                _privacySettings.value = settings
            }
        }

        // Observe blocked users
        viewModelScope.launch {
            userRepository.observeBlockedUsers(uid).collectLatest { list ->
                _blockedUsers.value = list
            }
        }

        // Observe Statuses
        viewModelScope.launch {
            socialRepository.observeStatuses().collectLatest { list ->
                _statuses.value = list
            }
        }

        // Observe Channels
        viewModelScope.launch {
            socialRepository.observeChannels().collectLatest { list ->
                _channels.value = list
                val currentActive = _activeChannel.value
                if (currentActive != null) {
                    list.firstOrNull { it.id == currentActive.id }?.let { updated ->
                        _activeChannel.value = updated
                    }
                }
            }
        }

        // Observe Communities
        viewModelScope.launch {
            socialRepository.observeCommunities().collectLatest { list ->
                _communities.value = list
            }
        }

        // Handle pending deep-link invite if received prior to auth
        _pendingDeepLinkInviteCode.value?.let { code ->
            _pendingDeepLinkInviteCode.value = null
            openJoinInviteDialog(code)
        }
    }

    private fun onUserSignedOut() {
        messagesJob?.cancel()
        chatsJob?.cancel()
        profileJob?.cancel()
        activeChatJob?.cancel()
        satAiJob?.cancel()
        friendRequestsJob?.cancel()
        chatMessageListeners.values.forEach { it.cancel() }
        chatMessageListeners.clear()
        notifiedMessageIds.clear()
        contactDiscoveryService.stopObservingContacts()
        contactDiscoveryService.clear()
        appNotificationManager.cancelAll()
        _unreadCounts.value = emptyMap()
        _totalUnreadCount.value = 0
        _inAppNotification.value = null
        _chats.value = emptyList()
        _messages.value = emptyList()
        _directoryUsers.value = emptyList()
        _savedContacts.value = emptyList()
        _friendRequests.value = emptyList()
        _deviceContactMatches.value = emptyMap()
        _deviceContactsScannedCount.value = 0
        _isSyncingDeviceContacts.value = false
        _secondaryUsernameFeedback.value = null
        _userProfile.value = null
        _activeChat.value = null
        _activeChannel.value = null
        _activeInviteDialogLink.value = null
        _showJoinInviteDialog.value = false
        _pendingInvitePreview.value = null
        _satAiMessages.value = emptyList()
        _satAiUiState.value = SatAiUiState.Idle
        _currentScreen.value = ScreenState.CHATS
    }

    fun signInWithGoogle(activity: Activity) {
        authRepository.signInWithGoogle(activity, viewModelScope) { err ->
            _toastMessage.value = err
        }
    }

    fun clearAuthError() {
        authRepository.clearError()
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.updatePresence(false)
            authRepository.signOut(viewModelScope)
        }
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
        if (screen == ScreenState.PROFILE && _appUpdateState.value is AppUpdateUiState.Idle) {
            checkForAppUpdates(userInitiated = false)
        }
    }

    fun openChat(chat: Chat) {
        _activeChat.value = chat
        _currentScreen.value = ScreenState.CONVERSATION
        _replyingTo.value = null
        if (_inAppNotification.value?.first == chat.id) {
            _inAppNotification.value = null
        }
        appNotificationManager.cancelChatNotification(chat.id)
        _unreadCounts.update { it + (chat.id to 0) }
        _totalUnreadCount.value = _unreadCounts.value.values.sum()

        activeChatJob?.cancel()
        activeChatJob = viewModelScope.launch {
            chatRepository.observeChat(chat.id).collectLatest { updatedChat ->
                if (updatedChat != null) {
                    _activeChat.value = updatedChat
                }
            }
        }

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.observeMessages(chat.id).collectLatest { list ->
                _messages.value = list
            }
        }

        // Mark messages as read
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chat.id)
        }
    }

    fun setTyping(isTyping: Boolean) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            chatRepository.setTyping(chat.id, isTyping)
        }
    }

    fun leaveChat() {
        val chat = _activeChat.value
        if (chat != null) {
            viewModelScope.launch {
                chatRepository.setTyping(chat.id, false)
            }
        }
        activeChatJob?.cancel()
        _activeChat.value = null
        _currentScreen.value = ScreenState.CHATS
    }

    fun openDirectChatWithUser(targetUser: UserProfile) {
        val current = _userProfile.value ?: return
        val targetName = targetUser.displayName.takeIf { it.isNotBlank() }
            ?: targetUser.email.substringBefore("@").takeIf { it.isNotBlank() }
            ?: "User"
        val currentName = current.displayName.takeIf { it.isNotBlank() }
            ?: current.email.substringBefore("@").takeIf { it.isNotBlank() }
            ?: "User"

        viewModelScope.launch {
            val result = chatRepository.getOrCreateDirectChat(
                targetUserId = targetUser.userId,
                targetUserName = targetName,
                currentUserName = currentName
            )
            result.onSuccess { chatId ->
                val chat = Chat(
                    id = chatId,
                    type = "direct",
                    memberIds = listOf(current.userId, targetUser.userId),
                    name = targetName,
                    participantNames = mapOf(current.userId to currentName, targetUser.userId to targetName)
                )
                openChat(chat)
                closeNewChatDialog()
            }.onFailure {
                _toastMessage.value = "Failed to start conversation: ${it.localizedMessage}"
            }
        }
    }

    fun createGroupChat(
        name: String,
        selectedMemberIds: List<String>,
        description: String? = null,
        iconUrl: String? = null
    ) {
        val current = _userProfile.value ?: return
        if (name.isBlank()) {
            _toastMessage.value = "Group room name cannot be empty"
            return
        }
        viewModelScope.launch {
            val result = chatRepository.createGroupRoom(
                name = name,
                description = description,
                selectedMemberIds = selectedMemberIds,
                currentUserName = current.displayName,
                iconUrl = iconUrl
            )
            result.onSuccess { roomId ->
                val chat = Chat(
                    id = roomId,
                    type = "group",
                    name = name,
                    description = description,
                    iconUrl = iconUrl,
                    memberIds = (selectedMemberIds + current.userId).distinct(),
                    adminIds = listOf(current.userId),
                    createdBy = current.userId
                )
                openChat(chat)
                closeNewChatDialog()
            }.onFailure {
                _toastMessage.value = "Failed to create group room: ${it.localizedMessage}"
            }
        }
    }

    fun updateGroupIcon(chatId: String, iconUrl: String?) {
        val cleanIcon = iconUrl?.trim()?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            val result = chatRepository.updateGroupIcon(chatId, cleanIcon)
            result.onSuccess {
                if (_activeChat.value?.id == chatId) {
                    _activeChat.update { it?.copy(iconUrl = cleanIcon) }
                }
                _toastMessage.value = if (cleanIcon != null) "Group icon updated." else "Group icon removed."
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Failed to update group icon."
            }
        }
    }

    fun sendMessage(
        text: String,
        type: String = "text",
        attachmentUrl: String? = null,
        fileName: String? = null,
        fileSize: Long? = null,
        durationSeconds: Int? = null
    ) {
        val chat = _activeChat.value ?: return
        val current = _userProfile.value ?: return
        val reply = _replyingTo.value

        if (text.isBlank() && attachmentUrl == null) return

        viewModelScope.launch {
            chatRepository.setTyping(chat.id, false)
            chatRepository.sendMessage(
                chatId = chat.id,
                senderName = current.displayName,
                text = text,
                type = type,
                attachmentUrl = attachmentUrl,
                fileName = fileName,
                fileSize = fileSize,
                durationSeconds = durationSeconds,
                replyTo = reply
            )
            _replyingTo.value = null
        }
    }

    fun sendMediaMessage(
        caption: String,
        attachmentUrl: String,
        type: String = "image",
        fileName: String? = null,
        fileSize: Long? = null,
        durationSeconds: Int? = null
    ) {
        sendMessage(
            text = caption,
            type = type,
            attachmentUrl = attachmentUrl,
            fileName = fileName,
            fileSize = fileSize,
            durationSeconds = durationSeconds
        )
    }

    fun setReplyingTo(message: Message?) {
        _replyingTo.value = message
    }

    fun deleteMessage(message: Message) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            chatRepository.deleteMessage(chat.id, message.id)
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        val chat = _activeChat.value ?: return
        viewModelScope.launch {
            val result = chatRepository.toggleMessageReaction(chat.id, messageId, emoji)
            result.onFailure {
                _toastMessage.value = "Failed to update reaction: ${it.localizedMessage}"
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            val result = chatRepository.deleteChat(chatId)
            result.onSuccess {
                if (_activeChat.value?.id == chatId) {
                    leaveChat()
                }
                _unreadCounts.update { it - chatId }
                _totalUnreadCount.value = _unreadCounts.value.values.sum()
                _toastMessage.value = "Chat deleted successfully."
            }.onFailure {
                _toastMessage.value = "Failed to delete chat: ${it.localizedMessage}"
            }
        }
    }

    fun clearChat(chatId: String) {
        viewModelScope.launch {
            val result = chatRepository.clearChat(chatId)
            result.onSuccess {
                _unreadCounts.update { it + (chatId to 0) }
                _totalUnreadCount.value = _unreadCounts.value.values.sum()
                _toastMessage.value = "Chat history cleared."
            }.onFailure {
                _toastMessage.value = "Failed to clear chat: ${it.localizedMessage}"
            }
        }
    }

    fun dismissNotification() {
        _inAppNotification.value = null
    }

    fun openNotificationChat(chatId: String) {
        val chat = _chats.value.firstOrNull { it.id == chatId }
        if (chat != null) {
            _pendingNotificationChatId.value = null
            openChat(chat)
        } else {
            _pendingNotificationChatId.value = chatId
        }
        dismissNotification()
    }

    fun setAppInForeground(inForeground: Boolean) {
        _isAppInForeground.value = inForeground
        if (authState.value is AuthUiState.Authenticated) {
            viewModelScope.launch {
                userRepository.updatePresence(inForeground)
            }
        }
    }

    fun setSystemNotificationsEnabled(enabled: Boolean) {
        _systemNotificationsEnabled.value = enabled
        notificationPrefs.edit().putBoolean(KEY_SYSTEM_NOTIFICATIONS_ENABLED, enabled).apply()
        if (!enabled) {
            appNotificationManager.cancelAll()
        }
        _toastMessage.value = if (enabled) {
            "System notifications enabled."
        } else {
            "System notifications muted."
        }
    }

    fun sendTestSystemNotification(): Boolean {
        val posted = appNotificationManager.sendTestNotification()
        if (posted) {
            _toastMessage.value = "Notification sent to your Notification Center."
        } else {
            _toastMessage.value = "Enable notification permission in Android Settings first."
        }
        return posted
    }

    fun hasNotificationPermission(): Boolean = appNotificationManager.hasNotificationPermission()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openNewChatDialog() {
        _showNewChatDialog.value = true
    }

    fun closeNewChatDialog() {
        _showNewChatDialog.value = false
    }

    fun openReportDialog(userId: String, userName: String) {
        _reportedTarget.value = Pair(userId, userName)
        _showReportDialog.value = true
    }

    fun closeReportDialog() {
        _showReportDialog.value = false
        _reportedTarget.value = null
    }

    fun submitReport(reason: String, details: String) {
        val target = _reportedTarget.value ?: return
        viewModelScope.launch {
            val result = userRepository.reportUser(target.first, reason, details)
            if (result.isSuccess) {
                _toastMessage.value = "Report submitted securely to SAT Compliance."
                closeReportDialog()
            } else {
                _toastMessage.value = "Failed to submit report."
            }
        }
    }

    fun blockUser(userId: String, userName: String) {
        viewModelScope.launch {
            val result = userRepository.blockUser(userId, userName)
            if (result.isSuccess) {
                _toastMessage.value = "User blocked."
                if (_currentScreen.value == ScreenState.CONVERSATION) {
                    _currentScreen.value = ScreenState.CHATS
                }
            }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            userRepository.unblockUser(userId)
            _toastMessage.value = "User unblocked."
        }
    }

    fun openUserSearchDialog() {
        _showUserSearchDialog.value = true
    }

    fun closeUserSearchDialog() {
        _showUserSearchDialog.value = false
    }

    fun updateProfile(displayName: String, statusMessage: String, phoneNumber: String? = null) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            userRepository.saveProfile(
                current.copy(
                    displayName = displayName.trim(),
                    statusMessage = statusMessage.trim(),
                    phoneNumber = phoneNumber?.trim()
                )
            )
            _toastMessage.value = "Profile updated."
        }
    }

    fun updateProfileAvatar(avatarUrl: String?) {
        val cleanAvatar = avatarUrl?.trim()?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            val result = userRepository.updateProfileAvatar(cleanAvatar)
            result.onSuccess {
                _userProfile.update { it?.copy(avatarUrl = cleanAvatar) }
                _toastMessage.value = if (cleanAvatar != null) "Profile photo updated." else "Profile photo removed."
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Failed to update profile photo."
            }
        }
    }

    fun updateSecondaryUsername(rawUsername: String) {
        _secondaryUsernameFeedback.value = null
        viewModelScope.launch {
            val result = userRepository.updateSecondaryUsername(rawUsername)
            result.onSuccess { eval ->
                val remainingNote = if (eval.nextLastChangedAtMs == null) {
                    "Secondary username @${eval.normalizedUsername} claimed! You can change it up to 3 times on an active change day (10-day cooldown between change windows)."
                } else {
                    "Secondary username updated to @${eval.normalizedUsername}. ${eval.remainingChangesTodayAfterSave} change(s) remaining today."
                }
                _secondaryUsernameFeedback.value = true to remainingNote
                _toastMessage.value = "Username updated to @${eval.normalizedUsername}."
            }.onFailure { err ->
                val message = err.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Could not update secondary username."
                _secondaryUsernameFeedback.value = false to message
                _toastMessage.value = message
            }
        }
    }

    fun clearSecondaryUsernameFeedback() {
        _secondaryUsernameFeedback.value = null
    }

    fun openFindFriends() {
        _currentScreen.value = ScreenState.FIND_FRIENDS
        if (contactDiscoveryService.hasReadContactsPermission(appContext)) {
            syncDeviceContacts(appContext, silent = true)
        }
    }

    fun refreshDeviceContactsPermission() {
        val granted = contactDiscoveryService.hasReadContactsPermission(appContext)
        val wasGranted = _hasDeviceContactsPermission.value
        _hasDeviceContactsPermission.value = granted
        if (granted && !wasGranted) {
            syncDeviceContacts(appContext, silent = true)
        }
    }

    fun syncDeviceContacts(context: Context = appContext, silent: Boolean = false) {
        val granted = contactDiscoveryService.hasReadContactsPermission(context)
        _hasDeviceContactsPermission.value = granted
        if (!granted) {
            if (!silent) {
                _toastMessage.value = "Contacts permission is needed to find registered device contacts."
            }
            return
        }

        contactDiscoveryService.startObservingContacts(appContext) {
            syncDeviceContacts(appContext, silent = true)
        }

        viewModelScope.launch {
            _isSyncingDeviceContacts.value = true
            try {
                val uid = _userProfile.value?.userId ?: Firebase.auth.currentUser?.uid.orEmpty()
                val directoryState = contactDiscoveryService.scanAndPopulateDirectory(
                    context = context,
                    directoryUsers = _directoryUsers.value,
                    currentUserId = uid,
                    fetchUsersFallback = { userRepository.fetchAllDirectoryUsers() }
                )
                val matches = directoryState.entries.associate { it.user.userId to it.toDeviceContactMatch() }
                _deviceContactsScannedCount.value = directoryState.scannedContactCount
                _deviceContactMatches.value = matches
                if (!silent) {
                    _toastMessage.value = when {
                        !directoryState.errorMessage.isNullOrBlank() -> directoryState.errorMessage
                        matches.isNotEmpty() -> "Find Friends ready: ${matches.size} registered SAT Messenger user(s) matched."
                        else -> "Scanned ${directoryState.scannedContactCount} device contacts; no registered users matched yet."
                    }
                }
            } finally {
                _isSyncingDeviceContacts.value = false
            }
        }
    }

    fun updateAvailabilityStatus(status: AvailabilityStatus) {
        viewModelScope.launch {
            val result = userRepository.updateAvailabilityStatus(status)
            result.onSuccess {
                _toastMessage.value = "Availability set to ${status.label}."
            }.onFailure { err ->
                _toastMessage.value = "Failed to update availability: ${err.localizedMessage}"
            }
        }
    }

    fun sendFriendRequest(targetUser: UserProfile) {
        val authUser = Firebase.auth.currentUser
        val current = _userProfile.value ?: authUser?.let {
            UserProfile(
                userId = it.uid,
                displayName = it.displayName?.takeIf { n -> n.isNotBlank() }
                    ?: it.email?.substringBefore("@") ?: "SAT Member",
                email = it.email.orEmpty(),
                avatarUrl = it.photoUrl?.toString()
            )
        } ?: return

        viewModelScope.launch {
            val result = userRepository.sendFriendRequest(targetUser, current)
            result.onSuccess { outcome ->
                _toastMessage.value = when (outcome) {
                    "already_friends" -> "You are already friends with ${targetUser.username}."
                    "accepted" -> "Accepted ${targetUser.username}'s friend request!"
                    else -> "Friend request sent to ${targetUser.username}."
                }
            }.onFailure { err ->
                _toastMessage.value = "Could not send friend request: ${err.localizedMessage}"
            }
        }
    }

    fun respondToFriendRequest(request: FriendRequest, accept: Boolean) {
        viewModelScope.launch {
            val result = userRepository.respondToFriendRequest(request, accept)
            result.onSuccess {
                _toastMessage.value = if (accept) {
                    "You and ${request.fromUserName.ifBlank { "user" }} are now friends!"
                } else {
                    "Friend request declined."
                }
            }.onFailure { err ->
                _toastMessage.value = "Failed to respond to request: ${err.localizedMessage}"
            }
        }
    }

    fun cancelOrRemoveFriend(targetUser: UserProfile) {
        viewModelScope.launch {
            val result = userRepository.cancelOrRemoveFriend(targetUser.userId)
            result.onSuccess {
                _toastMessage.value = "Removed ${targetUser.username} from Friends."
            }.onFailure { err ->
                _toastMessage.value = "Failed to update friend status: ${err.localizedMessage}"
            }
        }
    }

    fun toggleFriendContact(targetUser: UserProfile) {
        val uid = _userProfile.value?.userId ?: Firebase.auth.currentUser?.uid.orEmpty()
        val existingRequest = _friendRequests.value.firstOrNull { req ->
            (req.fromUserId == targetUser.userId && req.toUserId == uid) ||
                (req.fromUserId == uid && req.toUserId == targetUser.userId)
        }
        val isSavedFriend = _savedContacts.value.any {
            it.contactUserId == targetUser.userId && it.relationship == "friend"
        }

        when {
            existingRequest?.status == "pending" && existingRequest.toUserId == uid -> {
                respondToFriendRequest(existingRequest, accept = true)
            }
            existingRequest?.status == "accepted" || isSavedFriend || existingRequest?.status == "pending" -> {
                cancelOrRemoveFriend(targetUser)
            }
            else -> {
                sendFriendRequest(targetUser)
            }
        }
    }

    // Status Actions
    fun postStatus(text: String?, mediaUrl: String?, backgroundColor: String?) {
        val trimmedText = text?.trim()?.takeIf { it.isNotBlank() }
        val trimmedMedia = mediaUrl?.trim()?.takeIf { it.isNotBlank() }
        if (trimmedText == null && trimmedMedia == null) {
            _toastMessage.value = "Add text or pick a photo/video before sharing a status."
            return
        }

        val authUser = Firebase.auth.currentUser
        val current = _userProfile.value
        if (authUser == null && current == null) {
            _toastMessage.value = "Please sign in before posting a status."
            return
        }

        val resolvedName = current?.displayName?.takeIf { it.isNotBlank() }
            ?: current?.resolvedPrimaryUsername?.takeIf { it.isNotBlank() }
            ?: authUser?.displayName?.takeIf { it.isNotBlank() }
            ?: authUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "SAT Member"
        val resolvedAvatar = current?.avatarUrl ?: authUser?.photoUrl?.toString()

        viewModelScope.launch {
            val result = socialRepository.postStatus(
                userName = resolvedName,
                userAvatarUrl = resolvedAvatar,
                text = trimmedText,
                mediaUrl = trimmedMedia,
                backgroundColor = backgroundColor
            )
            if (result.isSuccess) {
                _toastMessage.value = "Status updated (expires in 24 hours)."
            } else {
                _toastMessage.value = "Failed to post status: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            val result = socialRepository.deleteStatus(statusId)
            if (result.isSuccess) {
                _toastMessage.value = "Status removed."
            } else {
                _toastMessage.value = "Failed to delete status: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun markStatusViewed(statusId: String) {
        viewModelScope.launch {
            socialRepository.markStatusViewed(statusId)
        }
    }

    fun replyToStatus(authorId: String, text: String, authorName: String = "") {
        val replyBody = text.trim()
        if (replyBody.isBlank()) return

        val authUser = Firebase.auth.currentUser
        val current = _userProfile.value
        val currentUid = current?.userId?.takeIf { it.isNotBlank() } ?: authUser?.uid ?: return
        val currentName = current?.displayName?.takeIf { it.isNotBlank() }
            ?: current?.resolvedPrimaryUsername?.takeIf { it.isNotBlank() }
            ?: authUser?.displayName?.takeIf { it.isNotBlank() }
            ?: authUser?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "SAT Member"

        viewModelScope.launch {
            val author = _directoryUsers.value.firstOrNull { it.userId == authorId }
                ?: userRepository.getUserProfile(authorId)
                ?: UserProfile(userId = authorId, displayName = authorName.ifBlank { "SAT Member" })
            val targetName = author.displayName.takeIf { it.isNotBlank() }
                ?: author.email.substringBefore("@").takeIf { it.isNotBlank() }
                ?: authorName.ifBlank { "User" }

            val chatResult = chatRepository.getOrCreateDirectChat(
                targetUserId = author.userId,
                targetUserName = targetName,
                currentUserName = currentName
            )
            chatResult.onSuccess { chatId ->
                val chat = Chat(
                    id = chatId,
                    type = "direct",
                    memberIds = listOf(currentUid, author.userId),
                    name = targetName,
                    participantNames = mapOf(currentUid to currentName, author.userId to targetName)
                )
                openChat(chat)
                val sendResult = chatRepository.sendMessage(
                    chatId = chatId,
                    senderName = currentName,
                    text = "Replying to status: $replyBody"
                )
                sendResult.onSuccess {
                    _toastMessage.value = "Status reply sent to $targetName."
                }.onFailure { err ->
                    _toastMessage.value = "Opened chat, but reply failed: ${err.localizedMessage}"
                }
            }.onFailure { err ->
                _toastMessage.value = "Failed to send status reply: ${err.localizedMessage}"
            }
        }
    }

    // Channel Actions
    fun createChannel(
        name: String,
        description: String?,
        iconUrl: String?,
        category: String = "General",
        isPublic: Boolean = true,
        allowFollowerPosts: Boolean = false,
        allowReactions: Boolean = true
    ) {
        viewModelScope.launch {
            val result = socialRepository.createChannel(
                name = name,
                description = description,
                iconUrl = iconUrl,
                category = category,
                isPublic = isPublic,
                allowFollowerPosts = allowFollowerPosts,
                allowReactions = allowReactions
            )
            if (result.isSuccess) {
                _toastMessage.value = "Channel '$name' created."
            } else {
                _toastMessage.value = "Failed to create channel: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateChannelSettings(
        channelId: String,
        name: String,
        description: String?,
        category: String,
        isPublic: Boolean,
        allowFollowerPosts: Boolean,
        allowReactions: Boolean,
        inviteCode: String? = null
    ) {
        viewModelScope.launch {
            val result = socialRepository.updateChannelSettings(
                channelId = channelId,
                name = name,
                description = description,
                category = category,
                isPublic = isPublic,
                allowFollowerPosts = allowFollowerPosts,
                allowReactions = allowReactions,
                inviteCode = inviteCode
            )
            result.onSuccess {
                _toastMessage.value = "Channel settings updated."
            }.onFailure {
                _toastMessage.value = "Failed to update channel: ${it.localizedMessage}"
            }
        }
    }

    fun updateChannelIcon(channelId: String, iconUrl: String?) {
        val cleanIcon = iconUrl?.trim()?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            val result = socialRepository.updateChannelIcon(channelId, cleanIcon)
            result.onSuccess {
                if (_activeChannel.value?.id == channelId) {
                    _activeChannel.update { it?.copy(iconUrl = cleanIcon) }
                }
                _toastMessage.value = if (cleanIcon != null) "Channel icon updated." else "Channel icon removed."
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Failed to update channel icon."
            }
        }
    }

    fun deleteChannel(channel: Channel) {
        viewModelScope.launch {
            val result = socialRepository.deleteChannel(channel.id, channel.inviteCode)
            result.onSuccess {
                if (_activeChannel.value?.id == channel.id) {
                    closeChannel()
                }
                _toastMessage.value = "Channel '${channel.name}' deleted."
            }.onFailure {
                _toastMessage.value = "Failed to delete channel: ${it.localizedMessage}"
            }
        }
    }

    fun toggleFollowChannel(channel: Channel) {
        viewModelScope.launch {
            val result = socialRepository.toggleFollowChannel(channel)
            result.onSuccess { followed ->
                _toastMessage.value = if (followed) "Following ${channel.name}" else "Unfollowed ${channel.name}"
            }
        }
    }

    private var channelPostsJob: Job? = null

    fun openChannel(channel: Channel) {
        _activeChannel.value = channel
        channelPostsJob?.cancel()
        channelPostsJob = viewModelScope.launch {
            socialRepository.observeChannelPosts(channel.id).collectLatest { posts ->
                _channelPosts.value = posts
            }
        }
    }

    fun closeChannel() {
        _activeChannel.value = null
        channelPostsJob?.cancel()
        _channelPosts.value = emptyList()
    }

    fun postToChannel(text: String, mediaUrl: String? = null) {
        val channel = _activeChannel.value ?: return
        val current = _userProfile.value ?: return
        if (text.isBlank() && mediaUrl.isNullOrBlank()) return
        viewModelScope.launch {
            val result = socialRepository.postToChannel(channel.id, current.displayName, text, mediaUrl)
            result.onFailure {
                _toastMessage.value = "Failed to post update: ${it.localizedMessage}"
            }
        }
    }

    fun deleteChannelPost(channelId: String, postId: String) {
        viewModelScope.launch {
            val result = socialRepository.deleteChannelPost(channelId, postId)
            result.onSuccess {
                _toastMessage.value = "Post deleted."
            }.onFailure {
                _toastMessage.value = "Failed to delete post: ${it.localizedMessage}"
            }
        }
    }

    fun reactToChannelPost(postId: String, currentReactions: Map<String, Int>, emoji: String) {
        val channel = _activeChannel.value ?: return
        if (!channel.allowReactions) return
        viewModelScope.launch {
            socialRepository.reactToPost(channel.id, postId, currentReactions, emoji)
        }
    }

    // Invite Link Actions
    fun openShareInviteDialog(
        targetType: String,
        targetId: String,
        targetName: String,
        targetDescription: String? = null,
        existingCode: String? = null
    ) {
        viewModelScope.launch {
            val result = inviteRepository.getOrCreateInviteLink(
                targetType = targetType,
                targetId = targetId,
                targetName = targetName,
                targetDescription = targetDescription,
                existingCode = existingCode
            )
            result.onSuccess { link ->
                _activeInviteDialogLink.value = link
                // Refresh active chat / channel reference if needed
                if (targetType == "group" && _activeChat.value?.id == targetId) {
                    _activeChat.update { it?.copy(inviteCode = link.code) }
                }
            }.onFailure {
                _toastMessage.value = "Could not load invite link: ${it.localizedMessage}"
            }
        }
    }

    fun closeShareInviteDialog() {
        _activeInviteDialogLink.value = null
    }

    fun regenerateInviteLink(invite: InviteLink) {
        viewModelScope.launch {
            val result = inviteRepository.regenerateInviteLink(
                targetType = invite.targetType,
                targetId = invite.targetId,
                targetName = invite.targetName,
                targetDescription = invite.targetDescription,
                oldCode = invite.code
            )
            result.onSuccess { newInvite ->
                _activeInviteDialogLink.value = newInvite
                if (invite.targetType == "group" && _activeChat.value?.id == invite.targetId) {
                    _activeChat.update { it?.copy(inviteCode = newInvite.code) }
                }
                _toastMessage.value = "Invite link reset. Previous link is now revoked."
            }.onFailure {
                _toastMessage.value = "Failed to reset invite link: ${it.localizedMessage}"
            }
        }
    }

    fun openJoinInviteDialog(prefillCodeOrUrl: String = "") {
        _joinInvitePrefill.value = prefillCodeOrUrl
        _pendingInvitePreview.value = null
        _inviteLookupError.value = null
        _showJoinInviteDialog.value = true
        if (prefillCodeOrUrl.isNotBlank()) {
            lookupInviteCode(prefillCodeOrUrl)
        }
    }

    fun closeJoinInviteDialog() {
        _showJoinInviteDialog.value = false
        _pendingInvitePreview.value = null
        _inviteLookupError.value = null
        _joinInvitePrefill.value = ""
    }

    fun lookupInviteCode(rawInput: String) {
        viewModelScope.launch {
            _isResolvingInvite.value = true
            _inviteLookupError.value = null
            _pendingInvitePreview.value = null
            val result = inviteRepository.resolveInviteLink(rawInput)
            _isResolvingInvite.value = false
            result.onSuccess { invite ->
                _pendingInvitePreview.value = invite
            }.onFailure { err ->
                _inviteLookupError.value = err.localizedMessage ?: "Invite link not found or revoked."
            }
        }
    }

    fun confirmJoinInvite(invite: InviteLink) {
        viewModelScope.launch {
            _isJoiningInvite.value = true
            val result = inviteRepository.redeemInviteLink(invite)
            _isJoiningInvite.value = false
            result.onSuccess { joined ->
                closeJoinInviteDialog()
                _toastMessage.value = "Joined ${joined.targetName}!"
                when (joined.targetType) {
                    "group" -> {
                        val existingChat = _chats.value.firstOrNull { it.id == joined.targetId }
                        val uid = _userProfile.value?.userId
                        val targetChat = existingChat ?: Chat(
                            id = joined.targetId,
                            type = "group",
                            name = joined.targetName,
                            description = joined.targetDescription,
                            inviteCode = joined.code,
                            memberIds = if (uid != null) listOf(uid) else emptyList()
                        )
                        openChat(targetChat)
                    }
                    "channel" -> {
                        val existingChannel = _channels.value.firstOrNull { it.id == joined.targetId }
                        val uid = _userProfile.value?.userId
                        val targetChannel = existingChannel ?: Channel(
                            id = joined.targetId,
                            name = joined.targetName,
                            description = joined.targetDescription,
                            inviteCode = joined.code,
                            subscriberIds = if (uid != null) listOf(uid) else emptyList(),
                            followerCount = 1
                        )
                        openChannel(targetChannel)
                    }
                }
            }.onFailure { err ->
                _inviteLookupError.value = "Could not join: ${err.localizedMessage}"
            }
        }
    }

    fun handleDeepLinkUri(uriString: String?) {
        val code = InviteRepository.extractInviteCode(uriString ?: "")
        if (code.isBlank()) return
        if (authState.value is AuthUiState.Authenticated) {
            openJoinInviteDialog(code)
        } else {
            _pendingDeepLinkInviteCode.value = code
        }
    }

    // Community Actions
    fun createCommunity(
        name: String,
        description: String?,
        subgroups: List<String>,
        iconUrl: String? = null
    ) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            // First create the subrooms
            val createdSubroomIds = mutableListOf<String>()
            for (subgroup in subgroups) {
                val roomResult = chatRepository.createGroupRoom(
                    name = "$name - $subgroup",
                    description = "Subgroup room for $name community",
                    selectedMemberIds = emptyList(),
                    currentUserName = current.displayName
                )
                roomResult.onSuccess { id -> createdSubroomIds.add(id) }
            }

            val result = socialRepository.createCommunity(name, description, iconUrl, createdSubroomIds)
            if (result.isSuccess) {
                _toastMessage.value = "Community '$name' created with ${createdSubroomIds.size} subgroups."
            } else {
                _toastMessage.value = "Failed to create community: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateCommunityIcon(communityId: String, iconUrl: String?) {
        val cleanIcon = iconUrl?.trim()?.takeIf { it.isNotBlank() }
        viewModelScope.launch {
            val result = socialRepository.updateCommunityIcon(communityId, cleanIcon)
            result.onSuccess {
                _toastMessage.value = if (cleanIcon != null) "Community icon updated." else "Community icon removed."
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Failed to update community icon."
            }
        }
    }

    fun joinCommunity(communityId: String) {
        viewModelScope.launch {
            val result = socialRepository.joinCommunity(communityId)
            if (result.isSuccess) {
                _toastMessage.value = "Joined community."
            }
        }
    }

    fun updatePrivacy(readReceipts: Boolean, lastSeenVisible: Boolean) {
        val current = _userProfile.value ?: return
        viewModelScope.launch {
            userRepository.updatePrivacySettings(
                PrivacySettings(
                    userId = current.userId,
                    readReceipts = readReceipts,
                    lastSeenVisible = lastSeenVisible
                )
            )
            _toastMessage.value = "Privacy settings updated."
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = userRepository.deleteAccount()
            if (result.isSuccess) {
                authRepository.signOut(viewModelScope)
            } else {
                _toastMessage.value = "Account deletion failed."
            }
        }
    }

    fun setGroupAdminRole(chatId: String, targetUserId: String, makeAdmin: Boolean) {
        viewModelScope.launch {
            val result = chatRepository.setGroupAdminRole(chatId, targetUserId, makeAdmin)
            result.onSuccess {
                _toastMessage.value = if (makeAdmin) {
                    "Member promoted to Group Admin."
                } else {
                    "Group Admin role removed."
                }
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Failed to update group admin role."
            }
        }
    }

    fun setChannelAdminRole(channelId: String, targetUserId: String, makeAdmin: Boolean) {
        viewModelScope.launch {
            val result = socialRepository.setChannelAdminRole(channelId, targetUserId, makeAdmin)
            result.onSuccess {
                _toastMessage.value = if (makeAdmin) {
                    "Member promoted to Channel Admin."
                } else {
                    "Channel Admin role removed."
                }
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Failed to update channel admin role."
            }
        }
    }

    fun setCommunityAdminRole(communityId: String, targetUserId: String, makeAdmin: Boolean) {
        viewModelScope.launch {
            val result = socialRepository.setCommunityAdminRole(communityId, targetUserId, makeAdmin)
            result.onSuccess {
                _toastMessage.value = if (makeAdmin) {
                    "Member promoted to Community Admin."
                } else {
                    "Community Admin role removed."
                }
            }.onFailure { err ->
                _toastMessage.value = err.localizedMessage ?: "Failed to update community admin role."
            }
        }
    }

    // SAT Limited AI Actions
    fun openSatAi() {
        _currentScreen.value = ScreenState.SAT_AI
    }

    fun setSatAiMode(mode: SatAiMode) {
        _satAiMode.value = mode
    }

    fun setSatAiWebGrounding(enabled: Boolean) {
        _satAiWebGroundingEnabled.value = enabled
    }

    fun dismissSatAiError() {
        if (_satAiUiState.value is SatAiUiState.Error) {
            _satAiUiState.value = SatAiUiState.Idle
        }
    }

    fun clearSatAiConversation() {
        satAiJob?.cancel()
        _satAiMessages.value = emptyList()
        _satAiUiState.value = SatAiUiState.Idle
    }

    fun sendSatAiPrompt(
        prompt: String,
        attachedImage: Bitmap? = null,
        attachmentPreviewUri: String? = null
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() && attachedImage == null) return

        val priorHistory = _satAiMessages.value
        val displayPrompt = trimmed.ifBlank { "Analyze attached image" }
        val userMessage = SatAiMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = displayPrompt,
            attachmentPreviewUri = attachmentPreviewUri
        )

        val selectedMode = _satAiMode.value
        val groundingEnabled = _satAiWebGroundingEnabled.value
        _satAiMessages.value = priorHistory + userMessage
        _satAiUiState.value = SatAiUiState.Loading

        satAiJob?.cancel()
        satAiJob = viewModelScope.launch {
            try {
                val result = satAiRepository.sendMessageStreaming(
                    historyMessages = priorHistory,
                    userPrompt = trimmed,
                    attachedImage = attachedImage,
                    mode = selectedMode,
                    webGroundingEnabled = groundingEnabled,
                    onPartialText = { partial ->
                        _satAiUiState.value = SatAiUiState.Streaming(partial)
                    }
                )
                result.onSuccess { response ->
                    val aiMessage = SatAiMessage(
                        id = UUID.randomUUID().toString(),
                        role = "model",
                        text = response.fullText,
                        modelLabel = selectedMode.label,
                        webSearchQueries = response.webSearchQueries,
                        groundingSources = response.groundingSources
                    )
                    _satAiMessages.value = _satAiMessages.value + aiMessage
                    _satAiUiState.value = SatAiUiState.Success
                }.onFailure { err ->
                    val message = err.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "SAT Limited AI could not complete the request. Please verify connectivity and try again."
                    _satAiUiState.value = SatAiUiState.Error(message)
                }
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    fun checkForAppUpdates(userInitiated: Boolean = true) {
        if (_appUpdateState.value is AppUpdateUiState.Checking) return
        val installed = BuildConfig.VERSION_NAME
        _appUpdateState.value = AppUpdateUiState.Checking(installedVersion = installed)

        updateCheckJob?.cancel()
        updateCheckJob = viewModelScope.launch {
            val resultState = appUpdateRepository.checkForUpdate(
                installedVersion = installed,
                isDebugBuild = BuildConfig.DEBUG
            )
            _appUpdateState.value = resultState

            when (resultState) {
                is AppUpdateUiState.UpdateAvailable -> {
                    val dismissedTag = notificationPrefs.getString(KEY_DISMISSED_UPDATE_TAG, null)
                    if (userInitiated || dismissedTag != resultState.latestRelease.tagName) {
                        _showUpdatePromptDialog.value = true
                    }
                }
                is AppUpdateUiState.UpToDate -> {
                    if (userInitiated) {
                        _toastMessage.value = "You are on the latest version (v$installed)."
                    }
                }
                is AppUpdateUiState.Error -> {
                    if (userInitiated) {
                        _toastMessage.value = resultState.message
                    }
                }
                else -> Unit
            }
        }
    }

    fun openUpdatePromptDialog() {
        if (_appUpdateState.value is AppUpdateUiState.UpdateAvailable) {
            _showUpdatePromptDialog.value = true
        }
    }

    fun dismissUpdatePromptDialog(rememberForThisVersion: Boolean = true) {
        _showUpdatePromptDialog.value = false
        val currentState = _appUpdateState.value
        if (rememberForThisVersion && currentState is AppUpdateUiState.UpdateAvailable) {
            notificationPrefs.edit()
                .putString(KEY_DISMISSED_UPDATE_TAG, currentState.latestRelease.tagName)
                .apply()
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        contactDiscoveryService.stopObservingContacts()
        super.onCleared()
    }

    private companion object {
        const val PREFS_NAME = "sat_notification_prefs"
        const val KEY_SYSTEM_NOTIFICATIONS_ENABLED = "system_notifications_enabled"
        const val KEY_DISMISSED_UPDATE_TAG = "dismissed_update_tag"
    }
}
