package com.example.ui.screens.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatTeal
import com.example.util.MediaAttachmentHelper
import com.example.util.UserSearchMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NewChatDialog(
    users: List<UserProfile>,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit,
    onCreateGroup: (String, List<String>, String?) -> Unit,
    onCreateGroupWithIcon: ((String, List<String>, String?, String?) -> Unit)? = null,
    onOpenJoinInvite: (() -> Unit)? = null,
    onOpenFindFriends: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var groupIconUrl by remember { mutableStateOf<String?>(null) }
    val selectedGroupMembers = remember { mutableStateListOf<String>() }

    val groupIconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val encoded = withContext(Dispatchers.IO) {
                    MediaAttachmentHelper.encodeIconOrAvatarDataUrl(context, uri)
                }
                if (encoded != null) {
                    groupIconUrl = encoded
                } else {
                    Toast.makeText(context, "Could not process selected icon.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val filteredUsers = remember(users, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) users
        else users.filter { UserSearchMatcher.matches(it, query) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTab == 0) "New Conversation" else "Create Group Chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Direct Message") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("New Group") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = groupName.ifBlank { "Group" },
                            avatarUrl = groupIconUrl,
                            isGroup = true,
                            size = 52.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = {
                                groupIconPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("create_group_pick_icon_button")
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (groupIconUrl != null) "Change Group Icon" else "Pick Group Icon")
                        }
                        if (groupIconUrl != null) {
                            IconButton(
                                onClick = { groupIconUrl = null },
                                modifier = Modifier.testTag("create_group_remove_icon_button")
                            ) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Remove group icon", tint = SatErrorRed)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Chat Room Name") },
                        placeholder = { Text("e.g. SAT Operations, Space Logistics") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it },
                        label = { Text("Room Charter / Topic (Optional)") },
                        placeholder = { Text("e.g. Daily team dispatch and coordination") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_description_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Name, @username, email or phone...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("colleague_search_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    if (filteredUsers.isEmpty()) {
                        item {
                            Text(
                                text = "No colleagues found in directory.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        items(filteredUsers, key = { it.userId }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedTab == 0) {
                                            onSelectUser(user)
                                        } else {
                                            if (selectedGroupMembers.contains(user.userId)) {
                                                selectedGroupMembers.remove(user.userId)
                                            } else {
                                                selectedGroupMembers.add(user.userId)
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val userName = user.username
                                val handleSummary = listOfNotNull(
                                    user.primaryUsernameHandle,
                                    user.secondaryUsernameHandle
                                ).joinToString(" • ")

                                UserAvatar(
                                    name = userName,
                                    avatarUrl = user.avatarUrl,
                                    isOnline = user.isOnline,
                                    size = 40.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = handleSummary,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SatTeal
                                    )
                                    val subtitle = listOfNotNull(
                                        user.phoneNumber,
                                        user.email.takeIf { it.isNotBlank() } ?: user.statusMessage
                                    ).joinToString(" • ")
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selectedTab == 1) {
                                    Checkbox(
                                        checked = selectedGroupMembers.contains(user.userId),
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedGroupMembers.add(user.userId)
                                            } else {
                                                selectedGroupMembers.remove(user.userId)
                                            }
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }
                    }
                }

                if (selectedTab == 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (onCreateGroupWithIcon != null) {
                                onCreateGroupWithIcon(
                                    groupName,
                                    selectedGroupMembers.toList(),
                                    groupDescription.ifBlank { null },
                                    groupIconUrl
                                )
                            } else {
                                onCreateGroup(
                                    groupName,
                                    selectedGroupMembers.toList(),
                                    groupDescription.ifBlank { null }
                                )
                            }
                        },
                        enabled = groupName.isNotBlank() && selectedGroupMembers.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_group_button")
                    ) {
                        Text("Create Group (${selectedGroupMembers.size} selected)")
                    }
                }

                if (onOpenFindFriends != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenFindFriends()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_chat_find_friends_button")
                    ) {
                        Icon(imageVector = Icons.Default.Contacts, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Find Friends from Device Contacts")
                    }
                }

                if (onOpenJoinInvite != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenJoinInvite()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_chat_join_invite_button")
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Join Group / Channel / Community via Invite")
                    }
                }
            }
        }
    }
}
