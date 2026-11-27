package com.example.ui.screens.search

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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import com.example.ui.theme.SatWarningAmber
import com.example.util.UserMatchField
import com.example.util.UserSearchMatcher

@Composable
fun UserSearchDialog(
    users: List<UserProfile>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit,
    friendIds: Set<String> = emptySet(),
    pendingFriendIds: Set<String> = emptySet(),
    onAddFriend: ((UserProfile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery, currentUserId) {
        val query = searchQuery.trim()
        val others = users.filter { it.userId != currentUserId }
        if (query.isBlank()) {
            others
        } else {
            others.filter { user -> UserSearchMatcher.matches(user, query) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Search Registered Users",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search by name, @primary/@secondary username, email or phone",
                            style = MaterialTheme.typography.labelSmall,
                            color = SatTeal
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Name, @username, email, or phone...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = SatElectricBlue)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_search_input_field")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${filteredUsers.size} registered user(s) found",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    if (filteredUsers.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = SatElectricBlue, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No users match \"$searchQuery\"",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Search works across registered name, email, phone number, primary username (before @), and secondary username.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredUsers, key = { it.userId }) { user ->
                            val query = searchQuery.trim()
                            val matchedField = remember(user, query) {
                                UserSearchMatcher.matchField(user, query)
                            }
                            val isFriend = friendIds.contains(user.userId)
                            val isPending = pendingFriendIds.contains(user.userId)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUser(user) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                                    .testTag("user_search_result_${user.userId}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val userName = user.username
                                val primaryHandle = user.primaryUsernameHandle
                                val secondaryHandle = user.secondaryUsernameHandle

                                UserAvatar(
                                    name = userName,
                                    avatarUrl = user.avatarUrl,
                                    isOnline = user.isOnline,
                                    availabilityStatus = user.resolvedAvailability(),
                                    size = 46.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = userName,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (user.isOnline) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SatOnlineGreen.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "Online",
                                                    color = SatOnlineGreen,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Primary & Secondary Usernames
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = primaryHandle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SatTeal,
                                            fontWeight = if (matchedField == UserMatchField.PRIMARY_USERNAME) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (secondaryHandle != null) {
                                            Icon(
                                                imageVector = Icons.Default.AlternateEmail,
                                                contentDescription = null,
                                                tint = SatElectricBlue,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = secondaryHandle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SatElectricBlue,
                                                fontWeight = if (matchedField == UserMatchField.SECONDARY_USERNAME) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (user.email.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Email,
                                                contentDescription = null,
                                                tint = if (matchedField == UserMatchField.EMAIL) SatTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (matchedField == UserMatchField.EMAIL) SatTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (matchedField == UserMatchField.EMAIL) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }

                                    if (!user.phoneNumber.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = if (matchedField == UserMatchField.PHONE) SatElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = user.phoneNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (matchedField == UserMatchField.PHONE) SatElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (matchedField == UserMatchField.PHONE) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }

                                    if (matchedField != null) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = SatTeal.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = "Matched: ${matchedField.label}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = SatTeal,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                if (onAddFriend != null) {
                                    IconButton(
                                        onClick = { onAddFriend(user) },
                                        enabled = !isFriend && !isPending,
                                        modifier = Modifier.testTag("user_search_add_friend_${user.userId}")
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                isFriend -> Icons.Default.Check
                                                isPending -> Icons.Default.HourglassTop
                                                else -> Icons.Default.PersonAdd
                                            },
                                            contentDescription = when {
                                                isFriend -> "Already Friends"
                                                isPending -> "Request Pending"
                                                else -> "Send Friend Request"
                                            },
                                            tint = when {
                                                isFriend -> SatOnlineGreen
                                                isPending -> SatWarningAmber
                                                else -> SatTeal
                                            }
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onSelectUser(user) },
                                    modifier = Modifier.testTag("user_search_message_${user.userId}")
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Message, contentDescription = "Message", tint = SatElectricBlue)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }
    }
}
