package com.example.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal

@Composable
fun UserSearchDialog(
    users: List<UserProfile>,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSelectUser: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery, currentUserId) {
        val query = searchQuery.trim()
        val others = users.filter { it.userId != currentUserId }
        if (query.isBlank()) others
        else others.filter { user ->
            user.displayName.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true) ||
                    (user.phoneNumber != null && user.phoneNumber.contains(query, ignoreCase = true)) ||
                    (user.statusMessage?.contains(query, ignoreCase = true) == true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
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
                    Column {
                        Text(
                            text = "Search Registered Users",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Find colleagues by name, email, or phone",
                            style = MaterialTheme.typography.labelSmall,
                            color = SatTeal
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Name, email or phone (e.g. +1 555)...") },
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
                    text = "${filteredUsers.size} colleagues found",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
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
                                        text = "Try searching by display name, corporate email address, or registered phone number.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredUsers, key = { it.userId }) { user ->
                            val query = searchQuery.trim()
                            val matchedPhone = query.isNotBlank() && user.phoneNumber?.contains(query, ignoreCase = true) == true
                            val matchedEmail = query.isNotBlank() && user.email.contains(query, ignoreCase = true)
                            val matchedName = query.isNotBlank() && user.displayName.contains(query, ignoreCase = true)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectUser(user) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp)
                                    .testTag("user_search_result_${user.userId}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val userName = user.displayName.takeIf { it.isNotBlank() }
                                    ?: user.email.substringBefore("@").takeIf { it.isNotBlank() }
                                    ?: "User"
                                val usernameHandle = user.email.substringBefore("@").takeIf { it.isNotBlank() }?.let { "@$it" }

                                UserAvatar(
                                    name = userName,
                                    avatarUrl = user.avatarUrl,
                                    isOnline = user.isOnline,
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
                                        if (usernameHandle != null && usernameHandle != "@$userName") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = usernameHandle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SatTeal
                                            )
                                        }
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

                                    // Display Email and Phone
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = if (matchedEmail) SatTeal else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = user.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (matchedEmail) SatTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (matchedEmail) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    if (!user.phoneNumber.isNullOrBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = if (matchedPhone) SatElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = user.phoneNumber,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (matchedPhone) SatElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (matchedPhone) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { onSelectUser(user) }) {
                                    Icon(imageVector = Icons.Default.Message, contentDescription = "Message", tint = SatElectricBlue)
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
