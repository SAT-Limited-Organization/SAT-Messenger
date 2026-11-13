package com.example.ui.screens.communities

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Chat
import com.example.data.model.Community
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatNavyDark
import com.example.ui.theme.SatTeal

@Composable
fun CommunitiesScreen(
    currentUserId: String,
    communities: List<Community>,
    allChats: List<Chat>,
    onOpenChat: (Chat) -> Unit,
    onCreateCommunity: (name: String, description: String?, subgroups: List<String>) -> Unit,
    onJoinCommunity: (String) -> Unit,
    onShareCommunityInvite: (Community) -> Unit = {},
    onOpenJoinInvite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SatElectricBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("create_community_fab")
            ) {
                Icon(imageVector = Icons.Default.Groups, contentDescription = "New Community", modifier = Modifier.size(28.dp))
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
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SatElectricBlue.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = SatTeal, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SAT Communities",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Organize related group chat rooms together",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SatTeal
                                )
                            }
                            Surface(
                                onClick = onOpenJoinInvite,
                                shape = RoundedCornerShape(20.dp),
                                color = SatTeal.copy(alpha = 0.2f),
                                modifier = Modifier.testTag("communities_join_invite_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Link, contentDescription = "Join via Invite", tint = SatTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Join Invite", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "My Communities (${communities.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (communities.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                            Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = SatTeal, modifier = Modifier.size(38.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Stay connected with a Community",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Communities bring related group chats under one roof, like engineering divisions, flight operations, or satellite payloads.",
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
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("start_community_button")
                                ) {
                                    Text("Start a Community")
                                }
                            }
                        }
                    }
                }
            } else {
                items(communities, key = { it.id }) { community ->
                    val isMember = community.memberIds.contains(currentUserId)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .testTag("community_card_${community.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Community Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SatElectricBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = community.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${community.memberIds.size} members • ${community.description ?: "Departmental Community"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { onShareCommunityInvite(community) },
                                    modifier = Modifier.testTag("share_community_invite_${community.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share Community Invite Link",
                                        tint = SatElectricBlue
                                    )
                                }
                                if (!isMember) {
                                    Button(
                                        onClick = { onJoinCommunity(community.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SatTeal, contentColor = Color.Black),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Join", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Subgroup Chat Rooms:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SatTeal,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Announcement Room (Default in every WhatsApp community)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Open announcement chat
                                        val announcementChat = allChats.firstOrNull { it.name?.contains(community.name, ignoreCase = true) == true }
                                            ?: Chat(id = "comm_${community.id}", type = "group", name = "📢 ${community.name} Announcements", memberIds = community.memberIds)
                                        onOpenChat(announcementChat)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(SatTeal.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Campaign, contentDescription = null, tint = SatTeal, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = "📢 Announcements", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "Official updates broadcast to community members", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // Subgroup Rooms
                            community.subgroupChatIds.forEach { subroomId ->
                                val linkedChat = allChats.firstOrNull { it.id == subroomId }
                                val subroomName = linkedChat?.name ?: "Group Room"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            linkedChat?.let { onOpenChat(it) }
                                                ?: onOpenChat(Chat(id = subroomId, type = "group", name = subroomName, memberIds = community.memberIds))
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(SatElectricBlue.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, tint = SatElectricBlue, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = subroomName, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                                        Text(text = linkedChat?.lastMessageText ?: "Tap to join discussion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCommunityDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, desc, subgroups ->
                onCreateCommunity(name, desc, subgroups)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateCommunityDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String?, subgroups: List<String>) -> Unit
) {
    var communityName by remember { mutableStateOf("") }
    var communityDesc by remember { mutableStateOf("") }
    var newSubgroupName by remember { mutableStateOf("") }
    val subgroups = remember { mutableStateListOf("General Discussion", "Mission Operations") }

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
                    Text(text = "New Community", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = communityName,
                    onValueChange = { communityName = it },
                    label = { Text("Community Name") },
                    placeholder = { Text("e.g. SAT Flight Software Division") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = communityDesc,
                    onValueChange = { communityDesc = it },
                    label = { Text("Description") },
                    placeholder = { Text("Purpose and guidelines for this community") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Subgroup Chat Rooms:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                subgroups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• $group", style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { subgroups.remove(group) }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubgroupName,
                        onValueChange = { newSubgroupName = it },
                        placeholder = { Text("Add subgroup room...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newSubgroupName.isNotBlank()) {
                                subgroups.add(newSubgroupName.trim())
                                newSubgroupName = ""
                            }
                        },
                        enabled = newSubgroupName.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = SatElectricBlue)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        onCreate(communityName, communityDesc.ifBlank { null }, subgroups.toList())
                    },
                    enabled = communityName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create Community (${subgroups.size} groups)")
                }
            }
        }
    }
}
