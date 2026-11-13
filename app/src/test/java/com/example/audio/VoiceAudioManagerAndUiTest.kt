package com.example.audio

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.ui.components.MessageBubble
import com.example.ui.components.VoicePreviewComposerBar
import com.example.ui.components.VoiceRecordingComposerBar
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.theme.SATMessengerTheme
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoiceAudioManagerAndUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun formatAudioDuration_formatsMinutesAndPaddedSeconds() {
        assertEquals("0:00", formatAudioDuration(0))
        assertEquals("0:07", formatAudioDuration(7))
        assertEquals("1:15", formatAudioDuration(75))
        assertEquals("0:00", formatAudioDuration(-5))
    }

    @Test
    fun encodeAndDecodeAudioBytes_roundTripsPayloadCleanly() {
        val sampleBytes = byteArrayOf(0x00, 0x01, 0x2A, 0x7F, -12, -1)
        val encoded = encodeAudioBytesToDataUri(sampleBytes)
        assertTrue(encoded.startsWith("data:audio/mp4;base64,"))

        val decoded = decodeAudioDataUriToBytes(encoded)
        assertArrayEquals(sampleBytes, decoded)
        assertNull(decodeAudioDataUriToBytes("https://example.com/not-data-uri.m4a"))
    }

    @Test
    fun messageBubble_rendersAudioPlayerCard_andInvokesToggleCallback() {
        var toggledMessageId: String? = null
        val audioMsg = Message(
            id = "voice_msg_1",
            chatId = "chat_1",
            senderId = "user_1",
            senderName = "Alice",
            text = "Voice message",
            type = "audio",
            attachmentUrl = "data:audio/mp4;base64,AAAAIGZ0eXBpc29t",
            fileName = "Voice_1.m4a",
            fileSize = 2048L,
            durationSeconds = 14
        )

        composeRule.setContent {
            SATMessengerTheme {
                MessageBubble(
                    message = audioMsg,
                    isOutgoing = true,
                    currentUserId = "user_1",
                    audioPlaybackState = VoicePlaybackState(),
                    onToggleAudioPlay = { msg -> toggledMessageId = msg.id }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("audio_player_voice_msg_1").assertIsDisplayed()
        composeRule.onNodeWithText("0:14").assertIsDisplayed()
        composeRule.onNodeWithText("Voice").assertIsDisplayed()
        composeRule.onNodeWithTag("audio_play_button_voice_msg_1").performClick()
        composeRule.waitForIdle()

        assertEquals("voice_msg_1", toggledMessageId)
    }

    @Test
    fun voiceRecordingAndPreviewComposerBars_renderAndTriggerActions() {
        var cancelled = false
        var stopped = false
        var sentNow = false
        var discardedPreview = false
        var toggledPreviewPlay = false
        var sentPreview = false

        composeRule.setContent {
            SATMessengerTheme {
                androidx.compose.foundation.layout.Column {
                    VoiceRecordingComposerBar(
                        recordingSeconds = 5,
                        amplitudeBars = listOf(0.2f, 0.5f, 0.8f, 0.4f),
                        onCancel = { cancelled = true },
                        onStopToPreview = { stopped = true },
                        onSendNow = { sentNow = true }
                    )
                    VoicePreviewComposerBar(
                        durationSeconds = 12,
                        isPlaying = false,
                        onDiscard = { discardedPreview = true },
                        onTogglePlay = { toggledPreviewPlay = true },
                        onSend = { sentPreview = true }
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("voice_recording_bar").assertIsDisplayed()
        composeRule.onNodeWithText("0:05 / 1:30").assertIsDisplayed()
        composeRule.onNodeWithTag("cancel_recording_button").performClick()
        composeRule.onNodeWithTag("stop_recording_button").performClick()
        composeRule.onNodeWithTag("send_voice_now_button").performClick()

        composeRule.onNodeWithTag("voice_preview_bar").assertIsDisplayed()
        composeRule.onNodeWithText("Duration 0:12 • Tap send to share").assertIsDisplayed()
        composeRule.onNodeWithTag("discard_voice_preview_button").performClick()
        composeRule.onNodeWithTag("play_voice_preview_button").performClick()
        composeRule.onNodeWithTag("send_voice_preview_button").performClick()
        composeRule.waitForIdle()

        assertTrue(cancelled)
        assertTrue(stopped)
        assertTrue(sentNow)
        assertTrue(discardedPreview)
        assertTrue(toggledPreviewPlay)
        assertTrue(sentPreview)
    }

    @Test
    fun chatScreen_showsVoiceMicButtonWhenEmpty_andSendButtonWhenTyping() {
        val chat = Chat(
            id = "chat_test",
            type = "direct",
            memberIds = listOf("u1", "u2"),
            participantNames = mapOf("u1" to "Alice", "u2" to "Bob")
        )

        composeRule.setContent {
            SATMessengerTheme {
                ChatScreen(
                    chat = chat,
                    currentUserId = "u1",
                    messages = emptyList(),
                    registeredUsers = emptyList(),
                    replyingTo = null,
                    onBack = {},
                    onSendMessage = { _, _, _ -> },
                    onSwitchUser = {},
                    onSetReplyingTo = {},
                    onDeleteMessage = {},
                    onBlockUser = { _, _ -> },
                    onReportUser = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("voice_record_button").assertIsDisplayed()
        composeRule.onNodeWithTag("message_input_field").performTextInput("Hello SAT")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("send_message_button").assertIsDisplayed()
    }
}
