package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

data class RecordedAudioClip(
    val fileName: String,
    val dataUri: String,
    val fileSizeBytes: Long,
    val durationSeconds: Int
)

data class VoicePlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0
)

fun formatAudioDuration(seconds: Int): String {
    val safeSec = max(0, seconds)
    val mins = safeSec / 60
    val secs = safeSec % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

fun encodeAudioBytesToDataUri(bytes: ByteArray): String {
    return "data:audio/mp4;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}

fun decodeAudioDataUriToBytes(dataUri: String): ByteArray? {
    val commaIndex = dataUri.indexOf(',')
    if (commaIndex == -1 || !dataUri.startsWith("data:audio/")) return null
    val base64Payload = dataUri.substring(commaIndex + 1)
    return try {
        Base64.decode(base64Payload, Base64.DEFAULT)
    } catch (e: IllegalArgumentException) {
        null
    }
}

class VoiceRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L
    private var isRecordingActive: Boolean = false

    fun isRecording(): Boolean = isRecordingActive

    fun startRecording(onMaxDurationReached: () -> Unit = {}): Boolean {
        if (isRecordingActive) return true

        val dir = File(context.cacheDir, "voice_recordings").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(24000)
                setMaxDuration(MAX_RECORDING_DURATION_MS)
                setOutputFile(file.absolutePath)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        onMaxDurationReached()
                    }
                }
                prepare()
                start()
            }
            recorder = mediaRecorder
            startTimeMs = System.currentTimeMillis()
            isRecordingActive = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            runCatching { mediaRecorder.release() }
            runCatching { file.delete() }
            recorder = null
            outputFile = null
            isRecordingActive = false
            false
        }
    }

    fun getMaxAmplitudeNormalized(): Float {
        if (!isRecordingActive) return 0f
        val amp = try {
            recorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }
        return (amp / 14000f).coerceIn(0.10f, 1.0f)
    }

    fun stopAndEncode(): Result<RecordedAudioClip> {
        val activeRecorder = recorder
        val file = outputFile
        if (!isRecordingActive || activeRecorder == null || file == null) {
            return Result.failure(IllegalStateException("No active voice recording"))
        }

        val elapsedSec = max(1, ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt())
        isRecordingActive = false
        recorder = null
        outputFile = null

        val stopSucceeded = try {
            activeRecorder.stop()
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "Recording stopped too early or failed", e)
            false
        } finally {
            runCatching { activeRecorder.release() }
        }

        if (!stopSucceeded || !file.exists() || file.length() <= 0L) {
            runCatching { file.delete() }
            return Result.failure(IllegalStateException("Voice clip too short. Hold or record at least 1 second."))
        }

        return try {
            val bytes = file.readBytes()
            runCatching { file.delete() }

            if (bytes.size > MAX_RAW_AUDIO_BYTES) {
                Result.failure(IllegalStateException("Voice clip exceeds size limit (max ~90s)."))
            } else {
                val dataUri = encodeAudioBytesToDataUri(bytes)
                Result.success(
                    RecordedAudioClip(
                        fileName = "Voice_${System.currentTimeMillis()}.m4a",
                        dataUri = dataUri,
                        fileSizeBytes = bytes.size.toLong(),
                        durationSeconds = elapsedSec
                    )
                )
            }
        } catch (e: Exception) {
            runCatching { file.delete() }
            Result.failure(e)
        }
    }

    fun cancel() {
        val activeRecorder = recorder
        val file = outputFile
        isRecordingActive = false
        recorder = null
        outputFile = null

        if (activeRecorder != null) {
            runCatching { activeRecorder.stop() }
            runCatching { activeRecorder.release() }
        }
        if (file != null && file.exists()) {
            runCatching { file.delete() }
        }
    }

    fun release() {
        cancel()
    }

    companion object {
        private const val TAG = "VoiceRecorderManager"
        const val MAX_RECORDING_DURATION_MS = 90_000
        const val MAX_RAW_AUDIO_BYTES = 360_000 // ~480 KB base64, safely under Firestore 500k string rule
    }
}

class VoicePlayerManager(private val context: Context) {

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    fun togglePlayPause(
        messageId: String,
        attachmentUrl: String,
        fallbackDurationSec: Int? = null,
        scope: CoroutineScope
    ) {
        val current = _playbackState.value
        val player = mediaPlayer

        if (current.activeMessageId == messageId && player != null) {
            try {
                if (player.isPlaying) {
                    player.pause()
                    progressJob?.cancel()
                    _playbackState.value = current.copy(
                        isPlaying = false,
                        currentPositionMs = player.currentPosition
                    )
                } else {
                    player.start()
                    _playbackState.value = current.copy(isPlaying = true)
                    startProgressPolling(scope, messageId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling audio playback", e)
                stop()
            }
            return
        }

        // Start playing a new audio clip
        stop()

        val newPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
        mediaPlayer = newPlayer

        try {
            if (attachmentUrl.startsWith("data:audio/")) {
                val bytes = decodeAudioDataUriToBytes(attachmentUrl)
                    ?: throw IllegalArgumentException("Invalid audio payload")
                val cacheDir = File(context.cacheDir, "voice_cache").apply { mkdirs() }
                val safeId = messageId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val audioFile = File(cacheDir, "clip_$safeId.m4a")
                if (!audioFile.exists() || audioFile.length() != bytes.size.toLong()) {
                    audioFile.writeBytes(bytes)
                }
                newPlayer.setDataSource(audioFile.absolutePath)
            } else {
                newPlayer.setDataSource(context, Uri.parse(attachmentUrl))
            }

            val initialDurationMs = (fallbackDurationSec ?: 0) * 1000
            _playbackState.value = VoicePlaybackState(
                activeMessageId = messageId,
                isPlaying = false,
                currentPositionMs = 0,
                durationMs = initialDurationMs
            )

            newPlayer.setOnPreparedListener { mp ->
                val actualDuration = if (mp.duration > 0) mp.duration else initialDurationMs
                mp.start()
                _playbackState.value = VoicePlaybackState(
                    activeMessageId = messageId,
                    isPlaying = true,
                    currentPositionMs = 0,
                    durationMs = actualDuration
                )
                startProgressPolling(scope, messageId)
            }

            newPlayer.setOnCompletionListener {
                progressJob?.cancel()
                _playbackState.value = VoicePlaybackState(
                    activeMessageId = null,
                    isPlaying = false,
                    currentPositionMs = 0,
                    durationMs = 0
                )
                runCatching { newPlayer.release() }
                if (mediaPlayer === newPlayer) {
                    mediaPlayer = null
                }
            }

            newPlayer.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                stop()
                true
            }

            newPlayer.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare audio clip", e)
            stop()
        }
    }

    fun seekTo(messageId: String, positionMs: Int) {
        val player = mediaPlayer ?: return
        if (_playbackState.value.activeMessageId != messageId) return
        try {
            val clamped = positionMs.coerceIn(0, max(1, _playbackState.value.durationMs))
            player.seekTo(clamped)
            _playbackState.value = _playbackState.value.copy(currentPositionMs = clamped)
        } catch (e: Exception) {
            Log.w(TAG, "Seek failed", e)
        }
    }

    private fun startProgressPolling(scope: CoroutineScope, messageId: String) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val player = mediaPlayer ?: break
                val state = _playbackState.value
                if (state.activeMessageId != messageId || !state.isPlaying) break
                val pos = runCatching { player.currentPosition }.getOrDefault(state.currentPositionMs)
                val dur = runCatching { player.duration }.getOrDefault(state.durationMs)
                _playbackState.value = state.copy(
                    currentPositionMs = pos,
                    durationMs = if (dur > 0) dur else state.durationMs
                )
                delay(100)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        mediaPlayer?.let { player ->
            runCatching {
                if (player.isPlaying) player.stop()
            }
            runCatching { player.release() }
        }
        mediaPlayer = null
        _playbackState.value = VoicePlaybackState()
    }

    fun release() {
        stop()
    }

    companion object {
        private const val TAG = "VoicePlayerManager"
    }
}
