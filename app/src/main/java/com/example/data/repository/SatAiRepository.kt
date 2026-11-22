package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

enum class SatAiMode(
    val id: String,
    val label: String,
    val modelName: String,
    val description: String
) {
    SMART(
        id = "smart",
        label = "Smart",
        modelName = "gemini-3.8-flash",
        description = "Balanced intelligence & live Google Search grounding"
    ),
    FAST(
        id = "fast",
        label = "Fast",
        modelName = "gemini-3.5-flash-lite",
        description = "Ultra-low-latency answers"
    ),
    REASONING(
        id = "reasoning",
        label = "Reasoning",
        modelName = "gemini-4-argon",
        description = "Deep multi-step technical & analytical reasoning"
    )
}

data class GroundingSource(
    val title: String,
    val uri: String,
    val domain: String? = null
)

data class SatAiMessage(
    val id: String,
    val role: String, // "user" or "model"
    val text: String,
    val modelLabel: String = "",
    val webSearchQueries: List<String> = emptyList(),
    val groundingSources: List<GroundingSource> = emptyList(),
    val attachmentPreviewUri: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

sealed interface SatAiUiState {
    data object Idle : SatAiUiState
    data object Loading : SatAiUiState
    data class Streaming(val partialText: String) : SatAiUiState
    data object Success : SatAiUiState
    data class Error(val message: String) : SatAiUiState
}

data class SatAiStreamResult(
    val fullText: String,
    val webSearchQueries: List<String>,
    val groundingSources: List<GroundingSource>
)

open class SatAiRepository {

    private val systemPromptContent: Content by lazy {
        content {
            text(
                "You are SAT Limited AI, the helpful enterprise and productivity assistant built into SAT Messenger for SAT Limited personnel. " +
                    "Provide clear, accurate, concise, and well-structured answers tailored to engineering, operations, team communication, planning, and general knowledge. " +
                    "When Google Search results are used, synthesize facts accurately and stay truthful."
            )
        }
    }

    private val balancedModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SatAiMode.SMART.modelName,
            systemInstruction = systemPromptContent
        )
    }

    private val balancedGroundedModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SatAiMode.SMART.modelName,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = systemPromptContent
        )
    }

    private val fastModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SatAiMode.FAST.modelName,
            systemInstruction = systemPromptContent
        )
    }

    private val fastGroundedModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SatAiMode.FAST.modelName,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = systemPromptContent
        )
    }

    private val reasoningModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SatAiMode.REASONING.modelName,
            systemInstruction = systemPromptContent
        )
    }

    private val reasoningGroundedModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = SatAiMode.REASONING.modelName,
            tools = listOf(Tool.googleSearch()),
            systemInstruction = systemPromptContent
        )
    }

    private fun resolveModel(mode: SatAiMode, webGroundingEnabled: Boolean): GenerativeModel {
        return when (mode) {
            SatAiMode.SMART -> if (webGroundingEnabled) balancedGroundedModel else balancedModel
            SatAiMode.FAST -> if (webGroundingEnabled) fastGroundedModel else fastModel
            SatAiMode.REASONING -> if (webGroundingEnabled) reasoningGroundedModel else reasoningModel
        }
    }

    open suspend fun sendMessageStreaming(
        historyMessages: List<SatAiMessage>,
        userPrompt: String,
        attachedImage: Bitmap? = null,
        mode: SatAiMode = SatAiMode.SMART,
        webGroundingEnabled: Boolean = true,
        onPartialText: (String) -> Unit
    ): Result<SatAiStreamResult> {
        return try {
            val model = resolveModel(mode, webGroundingEnabled)
            val sdkHistory = historyMessages
                .filter { it.text.isNotBlank() }
                .map { msg ->
                    content(role = if (msg.role == "user") "user" else "model") {
                        text(msg.text)
                    }
                }

            val chat = model.startChat(history = sdkHistory)
            val promptContent = content("user") {
                attachedImage?.let { image(it) }
                text(userPrompt.ifBlank { "Please analyze the attached image." })
            }

            val accumulated = StringBuilder()
            val collectedQueries = linkedSetOf<String>()
            val collectedSources = linkedMapOf<String, GroundingSource>()

            withTimeout(AI_TIMEOUT_MS) {
                chat.sendMessageStream(promptContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        onPartialText(accumulated.toString())
                    }

                    val grounding = chunk.candidates.firstOrNull()?.groundingMetadata
                    if (grounding != null) {
                        grounding.webSearchQueries.forEach { query ->
                            if (query.isNotBlank()) collectedQueries.add(query)
                        }
                        grounding.groundingChunks.forEach { gChunk ->
                            val web = gChunk.web
                            val uri = web?.uri.orEmpty()
                            if (uri.isNotBlank()) {
                                val title = web?.title?.takeIf { it.isNotBlank() }
                                    ?: web?.domain?.takeIf { it.isNotBlank() }
                                    ?: uri
                                collectedSources[uri] = GroundingSource(
                                    title = title,
                                    uri = uri,
                                    domain = web?.domain
                                )
                            }
                        }
                    }
                }
            }

            val finalText = accumulated.toString().trim()
            if (finalText.isBlank()) {
                Result.failure(IllegalStateException("SAT Limited AI returned an empty response."))
            } else {
                Result.success(
                    SatAiStreamResult(
                        fullText = finalText,
                        webSearchQueries = collectedQueries.toList(),
                        groundingSources = collectedSources.values.toList()
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "SAT Limited AI request timed out after ${AI_TIMEOUT_MS}ms", e)
            Result.failure(
                IllegalStateException("SAT Limited AI timed out after 60 seconds. Please try again.")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SAT Limited AI inference failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "SatAiRepository"
        const val AI_TIMEOUT_MS = 60_000L
    }
}
