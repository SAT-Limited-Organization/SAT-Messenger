package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.formatAudioDuration
import com.example.ui.theme.SatElectricBlue
import com.example.ui.theme.SatErrorRed
import com.example.ui.theme.SatTeal

@Composable
fun VoiceRecordingComposerBar(
    recordingSeconds: Int,
    amplitudeBars: List<Float>,
    onCancel: () -> Unit,
    onStopToPreview: () -> Unit,
    onSendNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("voice_recording_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_recording_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Discard voice recording",
                    tint = SatErrorRed
                )
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(pulseAlpha)
                    .background(SatErrorRed, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${formatAudioDuration(recordingSeconds)} / 1:30",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("recording_timer_text")
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Live audio amplitude waveform bars
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                amplitudeBars.forEach { norm ->
                    val barHeight = (6f + norm * 20f).dp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SatElectricBlue.copy(alpha = 0.85f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onStopToPreview,
                modifier = Modifier
                    .size(42.dp)
                    .minimumInteractiveComponentSize()
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .testTag("stop_recording_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop and preview voice clip",
                    tint = SatErrorRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onSendNow,
                modifier = Modifier
                    .size(44.dp)
                    .minimumInteractiveComponentSize()
                    .background(SatElectricBlue, CircleShape)
                    .testTag("send_voice_now_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send voice clip",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun VoicePreviewComposerBar(
    durationSeconds: Int,
    isPlaying: Boolean,
    onDiscard: () -> Unit,
    onTogglePlay: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("voice_preview_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDiscard,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("discard_voice_preview_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Discard voice clip",
                    tint = SatErrorRed
                )
            }

            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(42.dp)
                    .minimumInteractiveComponentSize()
                    .background(SatTeal, CircleShape)
                    .testTag("play_voice_preview_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Voice Clip Ready",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Duration ${formatAudioDuration(durationSeconds)} • Tap send to share",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(46.dp)
                    .minimumInteractiveComponentSize()
                    .background(SatElectricBlue, CircleShape)
                    .testTag("send_voice_preview_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send voice clip",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
