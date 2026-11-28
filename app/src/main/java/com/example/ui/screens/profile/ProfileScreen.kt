package com.example.ui.screens.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.BuildConfig
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AvailabilityStatus
import com.example.data.model.BlockedUser
import com.example.data.model.PrivacySettings
import com.example.data.model.UserProfile
import com.example.data.repository.AppReleaseInfo
import com.example.data.repository.AppUpdateUiState
import com.example.ui.components.AvailabilityStatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.availabilityColor
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import com.example.util.MediaAttachmentHelper
import com.example.util.UsernamePolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    privacySettings: PrivacySettings,
    blockedUsers: List<BlockedUser>,
    onBack: () -> Unit,
    onUpdateProfile: (name: String, status: String, phoneNumber: String?) -> Unit,
    onUpdateAvatar: (String?) -> Unit = {},
    onUpdateSecondaryUsername: (String) -> Unit = {},
    secondaryUsernameFeedback: Pair<Boolean, String>? = null,
    onUpdateAvailability: (AvailabilityStatus) -> Unit = {},
    onUpdatePrivacy: (readReceipts: Boolean, lastSeenVisible: Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onManualClick: () -> Unit = {},
    systemNotificationsEnabled: Boolean = true,
    hasNotificationPermission: Boolean = true,
    onToggleSystemNotifications: (Boolean) -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    appUpdateState: AppUpdateUiState = AppUpdateUiState.Idle(BuildConfig.VERSION_NAME),
    onCheckForUpdates: () -> Unit = {},
    onOpenUpdatePrompt: () -> Unit = {},
    onDownloadUpdate: (AppReleaseInfo) -> Unit = {},
    hasContactsPermission: Boolean = true,
    hasMicrophonePermission: Boolean = true,
    onRequestContactsPermission: () -> Unit = {},
    onRequestMicrophonePermission: () -> Unit = {},
    onRequestAllRequiredPermissions: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessingPhoto by remember { mutableStateOf(false) }
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingPhoto = true
            coroutineScope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    MediaAttachmentHelper.encodeIconOrAvatarDataUrl(context, uri)
                }
                isProcessingPhoto = false
                if (encoded != null) {
                    onUpdateAvatar(encoded)
                } else {
                    Toast.makeText(
                        context,
                        "Selected photo could not be compressed under cloud size limit.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    var displayName by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: "") }
    var statusMessage by remember(userProfile) { mutableStateOf(userProfile?.statusMessage ?: "") }
    var phoneNumber by remember(userProfile) { mutableStateOf(userProfile?.phoneNumber ?: "") }
    var secondaryUsernameInput by remember(userProfile?.secondaryUsername) {
        mutableStateOf(userProfile?.secondaryUsername ?: "")
    }
    val secondaryUsernameStatus = remember(userProfile) {
        UsernamePolicy.describeStatus(userProfile)
    }
    var readReceipts by remember(privacySettings) { mutableStateOf(privacySettings.readReceipts) }
    var lastSeenVisible by remember(privacySettings) { mutableStateOf(privacySettings.lastSeenVisible) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = {
                    Text(
                        text = "Profile & Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = onManualClick,
                        modifier = Modifier.testTag("profile_manual_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "App Manual",
                            tint = SatTeal
                        )
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
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                // Avatar and Identity Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(enabled = !isProcessingPhoto) {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        UserAvatar(
                            name = displayName.ifBlank { "User" },
                            avatarUrl = userProfile?.avatarUrl,
                            isOnline = userProfile?.isOnline ?: true,
                            availabilityStatus = userProfile?.resolvedAvailability(),
                            size = 92.dp
                        )
                        Surface(
                            shape = CircleShape,
                            color = SatElectricBlue,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(30.dp)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Change profile photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !isProcessingPhoto,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("profile_change_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isProcessingPhoto) "Updating..." else "Change Photo")
                        }
                        if (!userProfile?.avatarUrl.isNullOrBlank()) {
                            TextButton(
                                onClick = { onUpdateAvatar(null) },
                                enabled = !isProcessingPhoto,
                                modifier = Modifier.testTag("profile_remove_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = SatErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove Photo", color = SatErrorRed)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val resolvedUsername = userProfile?.resolvedPrimaryUsername?.takeIf { it.isNotBlank() } ?: "User"
                    val resolvedDisplayName = displayName.takeIf { it.isNotBlank() } ?: resolvedUsername
                    Text(
                        text = resolvedDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (userProfile?.resolvedPrimaryUsername?.isNotBlank() == true) {
                        Text(
                            text = "Primary: ${userProfile.primaryUsernameHandle}",
                            style = MaterialTheme.typography.titleSmall,
                            color = SatTeal,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("profile_primary_username_text")
                        )
                    }
                    userProfile?.secondaryUsernameHandle?.let { secondaryHandle ->
                        Text(
                            text = "Secondary: $secondaryHandle",
                            style = MaterialTheme.typography.labelLarge,
                            color = SatElectricBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("profile_secondary_username_text")
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userProfile?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val currentAvailability = userProfile?.resolvedAvailability() ?: AvailabilityStatus.AVAILABLE
                    AvailabilityStatusPill(status = currentAvailability)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(AvailabilityStatus.entries) { statusOption ->
                            val isSelected = statusOption == currentAvailability
                            val dotColor = availabilityColor(statusOption)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateAvailability(statusOption) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                if (isSelected) Color.White else dotColor,
                                                CircleShape
                                            )
                                    )
                                },
                                label = { Text(statusOption.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = dotColor,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("profile_availability_chip_${statusOption.name.lowercase()}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Profile Details Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SatElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Profile Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = statusMessage,
                            onValueChange = { statusMessage = it },
                            label = { Text("Corporate Status / Title") },
                            placeholder = { Text("e.g. Senior Network Specialist") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("status_message_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("e.g. +1 (555) 019-2834") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = SatTeal, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_number_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onUpdateProfile(displayName, statusMessage, phoneNumber.ifBlank { null }) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_profile_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Profile")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Usernames Card (Primary + Secondary)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = null, tint = SatTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SAT Messenger Usernames",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SatTeal.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Primary Username (from email)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SatTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = userProfile?.primaryUsernameHandle ?: "@user",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Secondary Username (Unique Handle)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = secondaryUsernameStatus.statusSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (secondaryUsernameStatus.canChangeNow) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                SatErrorRed
                            },
                            modifier = Modifier.testTag("secondary_username_policy_status")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = secondaryUsernameInput,
                            onValueChange = { secondaryUsernameInput = it.replace(" ", "") },
                            label = { Text("Choose Unpicked Secondary Username") },
                            placeholder = { Text("e.g. sat_commander") },
                            prefix = { Text("@") },
                            enabled = secondaryUsernameStatus.canChangeNow,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("secondary_username_input")
                        )

                        if (secondaryUsernameFeedback != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (secondaryUsernameFeedback.first) {
                                    SatOnlineGreen.copy(alpha = 0.14f)
                                } else {
                                    SatErrorRed.copy(alpha = 0.14f)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = secondaryUsernameFeedback.second,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (secondaryUsernameFeedback.first) SatOnlineGreen else SatErrorRed,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onUpdateSecondaryUsername(secondaryUsernameInput) },
                            enabled = secondaryUsernameStatus.canChangeNow && secondaryUsernameInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_secondary_username_button")
                        ) {
                            Icon(imageVector = Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (secondaryUsernameStatus.currentUsername == null) {
                                    "Claim Secondary Username"
                                } else {
                                    "Change Secondary Username (${secondaryUsernameStatus.remainingChangesToday}/3 today)"
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Privacy Settings Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = SatTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy Controls",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Read Receipts",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Allow others to see when you have read their messages.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = readReceipts,
                                onCheckedChange = {
                                    readReceipts = it
                                    onUpdatePrivacy(it, lastSeenVisible)
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Online & Last Seen Presence",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Display real-time active status to colleagues in chat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = lastSeenVisible,
                                onCheckedChange = {
                                    lastSeenVisible = it
                                    onUpdatePrivacy(readReceipts, it)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Phone Notification Center & Alerts Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = SatElectricBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notification Center",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (hasNotificationPermission) {
                                    SatOnlineGreen.copy(alpha = 0.15f)
                                } else {
                                    SatErrorRed.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = if (hasNotificationPermission) "Allowed" else "Permission Needed",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasNotificationPermission) SatOnlineGreen else SatErrorRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Post System Notifications",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Show new incoming messages in your phone's Notification Center.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = systemNotificationsEnabled && hasNotificationPermission,
                                onCheckedChange = { enabled ->
                                    if (enabled && !hasNotificationPermission) {
                                        onRequestNotificationPermission()
                                    } else {
                                        onToggleSystemNotifications(enabled)
                                    }
                                },
                                modifier = Modifier.testTag("system_notifications_switch")
                            )
                        }

                        if (!hasNotificationPermission) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequestNotificationPermission,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grant_notification_permission_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Notification Permission")
                            }
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = onSendTestNotification,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_test_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = SatElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Test Notification")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Permissions & Storage / Media Access Card
                val grantedRuntimeCount = listOf(
                    hasContactsPermission,
                    hasMicrophonePermission,
                    hasNotificationPermission
                ).count { it }
                val hasMissingRuntimePermissions = grantedRuntimeCount < 3

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_permissions_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = SatElectricBlue
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.permissions_card_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!hasMissingRuntimePermissions) {
                                    SatOnlineGreen.copy(alpha = 0.15f)
                                } else {
                                    SatTeal.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = "$grantedRuntimeCount/3 Runtime + Media",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!hasMissingRuntimePermissions) SatOnlineGreen else SatTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(R.string.permissions_card_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        PermissionStatusRow(
                            icon = Icons.Default.PermMedia,
                            iconTint = SatOnlineGreen,
                            title = stringResource(R.string.permissions_storage_media_title),
                            description = stringResource(R.string.permissions_storage_media_desc),
                            statusLabel = "Zero-Permission Active",
                            statusColor = SatOnlineGreen,
                            statusTag = "permission_storage_media_status"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        PermissionStatusRow(
                            icon = Icons.Default.People,
                            iconTint = SatElectricBlue,
                            title = "Contacts (READ_CONTACTS)",
                            description = "Scans device contacts to highlight registered colleagues and populate Find Friends.",
                            statusLabel = if (hasContactsPermission) "Allowed" else "Permission Needed",
                            statusColor = if (hasContactsPermission) SatOnlineGreen else SatErrorRed,
                            statusTag = "permission_contacts_status",
                            actionLabel = if (!hasContactsPermission) "Allow Contacts" else null,
                            actionTag = "grant_contacts_permission_button",
                            onActionClick = onRequestContactsPermission
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        PermissionStatusRow(
                            icon = Icons.Default.Mic,
                            iconTint = SatTeal,
                            title = "Microphone (RECORD_AUDIO)",
                            description = "Records voice clips directly inside direct and group conversations.",
                            statusLabel = if (hasMicrophonePermission) "Allowed" else "Permission Needed",
                            statusColor = if (hasMicrophonePermission) SatOnlineGreen else SatErrorRed,
                            statusTag = "permission_microphone_status",
                            actionLabel = if (!hasMicrophonePermission) "Allow Mic" else null,
                            actionTag = "grant_microphone_permission_button",
                            onActionClick = onRequestMicrophonePermission
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        PermissionStatusRow(
                            icon = Icons.Default.Notifications,
                            iconTint = SatElectricBlue,
                            title = "Notifications (POST_NOTIFICATIONS)",
                            description = "Shows incoming message alerts and channel broadcasts in Notification Center.",
                            statusLabel = if (hasNotificationPermission) "Allowed" else "Permission Needed",
                            statusColor = if (hasNotificationPermission) SatOnlineGreen else SatErrorRed,
                            statusTag = "permission_notifications_status",
                            actionLabel = if (!hasNotificationPermission) "Allow Alerts" else null,
                            actionTag = "grant_notifications_permission_card_button",
                            onActionClick = onRequestNotificationPermission
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        PermissionStatusRow(
                            icon = Icons.Default.Sync,
                            iconTint = SatTeal,
                            title = "Network & Real-Time Sync",
                            description = "Required for Cloud Firestore messaging, SAT Limited AI, and GitHub update checks.",
                            statusLabel = "Install-Time Active",
                            statusColor = SatTeal,
                            statusTag = "permission_network_status"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (hasMissingRuntimePermissions) {
                            Button(
                                onClick = onRequestAllRequiredPermissions,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("grant_required_permissions_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.permissions_grant_missing))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("open_android_app_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = SatElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.permissions_open_settings))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Blocked Users Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Blocked Contacts (${blockedUsers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (blockedUsers.isEmpty()) {
                            Text(
                                text = "No blocked contacts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            blockedUsers.forEach { blocked ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = blocked.blockedUserName.ifBlank { blocked.blockedUserId },
                                        fontWeight = FontWeight.Medium
                                    )
                                    OutlinedButton(
                                        onClick = { onUnblockUser(blocked.blockedUserId) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Unblock")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Connected Sign-In Method Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = SatTeal
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign-In Method",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SatOnlineGreen.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Google Account: Connected",
                                style = MaterialTheme.typography.labelMedium,
                                color = SatOnlineGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Updates & Version Card
                AppUpdateCard(
                    updateState = appUpdateState,
                    onCheckForUpdates = onCheckForUpdates,
                    onOpenUpdatePrompt = onOpenUpdatePrompt,
                    onDownloadUpdate = onDownloadUpdate
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Help & Documentation
                OutlinedButton(
                    onClick = onManualClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_manual_profile_button")
                ) {
                    Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, tint = SatTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("User Manual & System Guide")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Account Actions
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out from SAT Messenger")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SatErrorRed.copy(alpha = 0.15f), contentColor = SatErrorRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_account_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account & Clear Profile")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SAT Messenger v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("profile_app_version_text")
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete SAT Messenger Account?") },
                text = {
                    Text("This will permanently remove your profile, presence, and personal privacy settings from SAT Limited Cloud Firestore. This operation cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteAccount()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SatErrorRed)
                    ) {
                        Text("Delete Account")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    statusLabel: String,
    statusColor: Color,
    statusTag: String,
    actionLabel: String? = null,
    actionTag: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusColor.copy(alpha = 0.14f),
                modifier = Modifier.testTag(statusTag)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && actionTag != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(actionTag)
            ) {
                Text(actionLabel)
            }
        }
    }
}
