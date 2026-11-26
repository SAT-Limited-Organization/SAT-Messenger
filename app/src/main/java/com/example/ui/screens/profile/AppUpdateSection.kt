package com.example.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.repository.AppReleaseInfo
import com.example.data.repository.AppUpdateUiState
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatOnlineGreen
import com.example.ui.theme.SatTeal
import com.example.ui.theme.SatWarningAmber
import java.text.DateFormat
import java.util.Date

@Composable
fun AppUpdateCard(
    updateState: AppUpdateUiState,
    onCheckForUpdates: () -> Unit,
    onOpenUpdatePrompt: () -> Unit,
    onDownloadUpdate: (AppReleaseInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val isChecking = updateState is AppUpdateUiState.Checking
    val statusBadge = when (updateState) {
        is AppUpdateUiState.Idle -> "GitHub Releases" to SatElectricBlue
        is AppUpdateUiState.Checking -> "Checking…" to SatElectricBlue
        is AppUpdateUiState.UpToDate -> "Up to Date" to SatOnlineGreen
        is AppUpdateUiState.UpdateAvailable -> "Update Available" to SatWarningAmber
        is AppUpdateUiState.Error -> "Check Failed" to SatErrorRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (updateState is AppUpdateUiState.UpdateAvailable) {
            BorderStroke(1.5.dp, SatElectricBlue.copy(alpha = 0.45f))
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_update_card")
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
                        imageVector = if (updateState is AppUpdateUiState.UpdateAvailable) {
                            Icons.Default.NewReleases
                        } else {
                            Icons.Default.SystemUpdate
                        },
                        contentDescription = null,
                        tint = if (updateState is AppUpdateUiState.UpdateAvailable) {
                            SatWarningAmber
                        } else {
                            SatElectricBlue
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_updates_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusBadge.second.copy(alpha = 0.14f),
                    modifier = Modifier.testTag("app_update_status_badge")
                ) {
                    Text(
                        text = statusBadge.first,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusBadge.second,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            VersionRow(
                label = stringResource(R.string.settings_installed_version_label),
                value = "v${updateState.installedVersion} (Build ${BuildConfig.VERSION_CODE})",
                valueColor = MaterialTheme.colorScheme.onSurface,
                tag = "installed_version_value"
            )

            when (updateState) {
                is AppUpdateUiState.UpToDate -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    VersionRow(
                        label = stringResource(R.string.settings_latest_version_label),
                        value = updateState.latestRelease?.let { "v${it.versionName}" }
                            ?: "No published release yet",
                        valueColor = SatOnlineGreen,
                        tag = "latest_version_value"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = SatOnlineGreen.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SatOnlineGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val checkedTime = DateFormat.getTimeInstance(DateFormat.SHORT)
                            .format(Date(updateState.checkedAtMillis))
                        Text(
                            text = if (updateState.latestRelease != null) {
                                "You are running the latest release (checked $checkedTime)."
                            } else {
                                "No GitHub releases published yet (checked $checkedTime)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is AppUpdateUiState.UpdateAvailable -> {
                    val release = updateState.latestRelease
                    Spacer(modifier = Modifier.height(8.dp))
                    VersionRow(
                        label = stringResource(R.string.settings_latest_version_label),
                        value = "v${release.versionName}",
                        valueColor = SatElectricBlue,
                        tag = "latest_version_value"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        SatElectricBlue.copy(alpha = 0.12f),
                                        SatTeal.copy(alpha = 0.12f)
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                            .testTag("update_available_banner")
                    ) {
                        Column {
                            Text(
                                text = release.releaseTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val metaParts = buildList {
                                release.apkAssetName?.let { add(it) }
                                release.apkSizeBytes?.let { add(formatFileSize(it)) }
                            }
                            if (metaParts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = metaParts.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SatElectricBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (release.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = release.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onDownloadUpdate(release) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SatElectricBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("download_update_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.settings_download_update_action))
                                }

                                OutlinedButton(
                                    onClick = onOpenUpdatePrompt,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("view_release_notes_button")
                                ) {
                                    Text(stringResource(R.string.settings_view_release_notes_action))
                                }
                            }
                        }
                    }
                }

                is AppUpdateUiState.Error -> {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = SatErrorRed.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("app_update_error_banner")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = SatErrorRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = updateState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = SatErrorRed
                        )
                    }
                }

                is AppUpdateUiState.Idle,
                is AppUpdateUiState.Checking -> Unit
            }

            AnimatedVisibility(
                visible = isChecking,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = SatElectricBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Checking SAT-Limited-Organization/SAT-Messenger on GitHub…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onCheckForUpdates,
                enabled = !isChecking,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("check_updates_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = SatElectricBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isChecking) {
                        stringResource(R.string.settings_checking_updates_action)
                    } else {
                        stringResource(R.string.settings_check_updates_action)
                    }
                )
            }
        }
    }
}

@Composable
private fun VersionRow(
    label: String,
    value: String,
    valueColor: Color,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier
                .weight(1f, fill = false)
                .testTag(tag)
        )
    }
}

@Composable
fun UpdatePromptDialog(
    installedVersion: String,
    release: AppReleaseInfo,
    onDownloadUpdate: (AppReleaseInfo) -> Unit,
    onOpenGitHubRelease: (AppReleaseInfo) -> Unit,
    onDismiss: (rememberForThisVersion: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss(true) },
        icon = {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = SatElectricBlue,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.update_dialog_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SatElectricBlue.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = release.releaseTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SatElectricBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Installed: v$installedVersion  →  Latest: v${release.versionName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("update_dialog_version_comparison")
                        )
                        release.apkAssetName?.let { asset ->
                            val sizeLabel = release.apkSizeBytes?.let { " (${formatFileSize(it)})" }.orEmpty()
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Package: $asset$sizeLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                Text(
                    text = "What's New",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = release.releaseNotes.ifBlank {
                        "A new SAT Messenger release is ready on GitHub. Download the latest APK to stay up to date."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDownloadUpdate(release) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SatElectricBlue,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("update_dialog_download_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.update_dialog_download))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onOpenGitHubRelease(release) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("update_dialog_github_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.update_dialog_github))
                }
                TextButton(
                    onClick = { onDismiss(true) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("update_dialog_later_button")
                ) {
                    Text(stringResource(R.string.update_dialog_later))
                }
            }
        },
        modifier = Modifier.testTag("update_prompt_dialog")
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    } else {
        String.format(java.util.Locale.US, "%.0f KB", kb)
    }
}
