package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AvailabilityStatus
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatSlate400
import com.example.ui.theme.SatTeal
import com.example.ui.theme.SatWarningAmber
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun availabilityColor(status: AvailabilityStatus): Color {
    return when (status) {
        AvailabilityStatus.AVAILABLE -> SatOnlineGreen
        AvailabilityStatus.BUSY -> SatErrorRed
        AvailabilityStatus.AWAY -> SatWarningAmber
        AvailabilityStatus.IN_MEETING -> SatElectricBlue
        AvailabilityStatus.DO_NOT_DISTURB -> SatErrorRed
        AvailabilityStatus.OFFLINE -> SatSlate400
    }
}

fun formatLastSeenRelative(timestamp: Timestamp?): String {
    if (timestamp == null) return "Offline"
    val diffMs = (System.currentTimeMillis() - timestamp.toDate().time).coerceAtLeast(0L)
    val mins = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    return when {
        mins < 1 -> "Last seen just now"
        mins < 60 -> "Last seen ${mins}m ago"
        hours < 24 -> "Last seen ${hours}h ago"
        else -> "Last seen ${SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())}"
    }
}

@Composable
fun AvailabilityStatusPill(
    status: AvailabilityStatus,
    lastSeen: Timestamp? = null,
    showOfflineLastSeen: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dotColor = availabilityColor(status)
    val labelText = if (status == AvailabilityStatus.OFFLINE && showOfflineLastSeen && lastSeen != null) {
        formatLastSeenRelative(lastSeen)
    } else {
        status.label
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = dotColor.copy(alpha = 0.14f),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(dotColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (status == AvailabilityStatus.OFFLINE) MaterialTheme.colorScheme.onSurfaceVariant else dotColor
            )
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    avatarUrl: String? = null,
    isOnline: Boolean = false,
    availabilityStatus: AvailabilityStatus? = null,
    isGroup: Boolean = false,
    size: Dp = 48.dp,
    hasStatus: Boolean = false,
    statusRingColor: Color = SatOnlineGreen,
    modifier: Modifier = Modifier
) {
    val outerModifier = if (hasStatus) {
        modifier
            .size(size)
            .border(2.5.dp, statusRingColor, CircleShape)
            .padding(2.5.dp)
    } else {
        modifier.size(size)
    }

    Box(modifier = outerModifier) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            val initials = if (isGroup) "" else {
                name.trim().split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
                    .ifEmpty { "U" }
            }
            val bgColor = if (isGroup) SatTeal else SatElectricBlue

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                if (isGroup) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Group",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f)
                    )
                } else {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.4f).sp
                    )
                }
            }
        }

        val showDot = !isGroup && (availabilityStatus != null || isOnline)
        if (showDot) {
            val badgeSize = (size * 0.28f).coerceAtLeast(10.dp)
            val badgeColor = availabilityStatus?.let { availabilityColor(it) } ?: SatOnlineGreen
            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .align(Alignment.BottomEnd)
                    .background(badgeColor, CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

@Composable
fun MessageStatusIndicator(
    status: String,
    isOutgoing: Boolean,
    isRead: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!isOutgoing) return

    val effectivelyRead = isRead || status == "read"
    val icon = when {
        effectivelyRead -> Icons.Default.DoneAll
        status == "delivered" -> Icons.Default.DoneAll
        else -> Icons.Default.Done
    }
    val tint = when {
        effectivelyRead -> SatTeal
        status == "delivered" -> Color.White.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.6f)
    }

    Icon(
        imageVector = icon,
        contentDescription = if (effectivelyRead) "Read" else "Status $status",
        tint = tint,
        modifier = modifier.size(14.dp)
    )
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_state_action_button")
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorCard(
    message: String,
    onDismiss: () -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("auth_error_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notice",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(
                        onClick = onAction,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("error_card_action_button")
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_error_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss error",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
