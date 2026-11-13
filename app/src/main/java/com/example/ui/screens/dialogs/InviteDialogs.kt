package com.example.ui.screens.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InviteLink
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatNavyDark
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal

@Composable
fun ShareInviteLinkDialog(
    invite: InviteLink,
    canManage: Boolean,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }

    val (typeLabel, typeIcon) = when (invite.targetType) {
        "channel" -> "Channel Invite Link" to Icons.Default.Campaign
        "group" -> "Group Room Invite Link" to Icons.Default.Group
        "community" -> "Community Invite Link" to Icons.Default.Groups
        else -> "Invite Link" to Icons.Default.Link
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_invite_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(SatElectricBlue.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = typeIcon, contentDescription = null, tint = SatElectricBlue, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = invite.targetName,
                                style = MaterialTheme.typography.bodySmall,
                                color = SatTeal,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SatNavyDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SHAREABLE LINK",
                            style = MaterialTheme.typography.labelSmall,
                            color = SatTeal,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = invite.shareUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White,
                            modifier = Modifier.testTag("invite_share_url_text")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "INVITE CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = invite.code,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = SatTeal,
                                    modifier = Modifier.testTag("invite_code_text")
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${invite.usesCount} joins",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Anyone signed into SAT Messenger with this link or code can join ${invite.targetName}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SAT Invite Link", invite.shareUrl))
                            Toast.makeText(context, "Invite link copied", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_invite_link_button")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link")
                    }

                    Button(
                        onClick = {
                            val shareText = "Join \"${invite.targetName}\" on SAT Messenger!\n" +
                                "Link: ${invite.shareUrl}\n" +
                                "Invite Code: ${invite.code}"
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Invite to ${invite.targetName}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Invite Link"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SatElectricBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_invite_link_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SAT Invite Code", invite.code))
                            Toast.makeText(context, "Code ${invite.code} copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_invite_code_button")
                    ) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code")
                    }

                    if (canManage) {
                        TextButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier.testTag("reset_invite_link_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = SatErrorRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Link", color = SatErrorRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Invite Link?") },
            text = { Text("Resetting generates a new code and immediately revokes the previous invite link so no one else can use it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onRegenerate()
                    }
                ) {
                    Text("Reset Link", color = SatErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun JoinInviteLinkDialog(
    initialInput: String,
    previewInvite: InviteLink?,
    lookupError: String?,
    isResolving: Boolean,
    isJoining: Boolean,
    onDismiss: () -> Unit,
    onLookup: (String) -> Unit,
    onConfirmJoin: (InviteLink) -> Unit
) {
    var inputText by remember(initialInput) { mutableStateOf(initialInput) }

    LaunchedEffect(initialInput) {
        if (initialInput.isNotBlank()) {
            inputText = initialInput
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("join_invite_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = SatTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Join via Invite",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Paste a Channel, Group, or Community invite link or code (e.g. CHN-XXXXXX, GRP-XXXXXX, COM-XXXXXX).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Invite Link or Code") },
                    placeholder = { Text("https://satmessenger.app/invite/CHN-...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_invite_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onLookup(inputText) },
                    enabled = inputText.isNotBlank() && !isResolving && !isJoining,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lookup_invite_button")
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking Invite...")
                    } else {
                        Text("Find Channel / Group / Community")
                    }
                }

                if (!lookupError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SatErrorRed.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = null, tint = SatErrorRed, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = lookupError,
                                style = MaterialTheme.typography.bodySmall,
                                color = SatErrorRed
                            )
                        }
                    }
                }

                if (previewInvite != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val (badgeText, badgeIcon) = when (previewInvite.targetType) {
                        "channel" -> "Broadcast Channel" to Icons.Default.Campaign
                        "group" -> "Group Chat Room" to Icons.Default.Group
                        "community" -> "Community Hub" to Icons.Default.Groups
                        else -> "SAT Space" to Icons.Default.Link
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SatTeal.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_preview_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = badgeIcon, contentDescription = null, tint = SatTeal, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SatTeal.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SatTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Valid Invite", tint = SatOnlineGreen, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = previewInvite.targetName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!previewInvite.targetDescription.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = previewInvite.targetDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onConfirmJoin(previewInvite) },
                                enabled = !isJoining,
                                colors = ButtonDefaults.buttonColors(containerColor = SatOnlineGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_join_invite_button")
                            ) {
                                if (isJoining) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Joining...")
                                } else {
                                    Text("Join Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
