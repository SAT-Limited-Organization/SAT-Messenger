package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class EncodedMediaResult(
    val type: String, // "image", "video", "file"
    val dataUrlOrUri: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val errorMessage: String? = null
)

object MediaAttachmentHelper {

    private const val MAX_INLINE_IMAGE_BYTES = 340_000
    private const val MAX_INLINE_RAW_BINARY_BYTES = 340_000

    fun isVideoUrlOrMime(urlOrMime: String?): Boolean {
        if (urlOrMime.isNullOrBlank()) return false
        val lower = urlOrMime.lowercase()
        return lower.startsWith("data:video/") ||
            lower.startsWith("video/") ||
            lower.endsWith(".mp4") ||
            lower.endsWith(".mov") ||
            lower.endsWith(".webm") ||
            lower.endsWith(".mkv")
    }

    fun decodeBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 960): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return null
                scaleBitmap(original, maxDim)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun encodeImageUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDim: Int = 880,
        maxEncodedChars: Int = 460_000
    ): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return null
                val scaled = scaleBitmap(original, maxDim)
                val qualities = listOf(80, 65, 50, 35)
                var bestBytes: ByteArray? = null
                for (q in qualities) {
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, q, baos)
                    val bytes = baos.toByteArray()
                    bestBytes = bytes
                    if (bytes.size <= MAX_INLINE_IMAGE_BYTES) {
                        val candidate = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                        if (candidate.length <= maxEncodedChars) {
                            return candidate
                        }
                    }
                }
                bestBytes?.let { bytes ->
                    val candidate = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                    if (candidate.length <= maxEncodedChars) candidate else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun encodeIconOrAvatarDataUrl(context: Context, uri: Uri): String? {
        return encodeImageUriToDataUrl(
            context = context,
            uri = uri,
            maxDim = 480,
            maxEncodedChars = 350_000
        )
    }

    /**
     * Converts `data:image/...;base64,...` data URLs into decoded [ByteArray] models because
     * Coil 2.x does not ship a built-in `data:` URI fetcher, while preserving regular HTTP(S),
     * content://, and file:// strings unchanged.
     */
    fun resolveImageModel(rawUrl: String?): Any? {
        if (rawUrl.isNullOrBlank()) return null
        val trimmed = rawUrl.trim()
        if (trimmed.startsWith("data:", ignoreCase = true)) {
            val base64MarkerIndex = trimmed.indexOf("base64,", ignoreCase = true)
            if (base64MarkerIndex != -1) {
                val base64Payload = trimmed.substring(base64MarkerIndex + "base64,".length)
                if (base64Payload.isBlank()) return null
                return try {
                    Base64.decode(base64Payload, Base64.DEFAULT)
                } catch (_: Exception) {
                    null
                }
            }
        }
        return trimmed
    }

    fun encodeVisualMedia(context: Context, uri: Uri): EncodedMediaResult {
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
        val (displayName, sizeBytes) = getFileMetadata(context, uri)
        val isVideo = mime.startsWith("video/") || isVideoUrlOrMime(displayName)

        if (!isVideo) {
            val dataUrl = encodeImageUriToDataUrl(context, uri)
            return if (dataUrl != null) {
                EncodedMediaResult(
                    type = "image",
                    dataUrlOrUri = dataUrl,
                    fileName = displayName.ifBlank { "Photo_${System.currentTimeMillis()}.jpg" },
                    fileSizeBytes = dataUrl.length.toLong()
                )
            } else {
                EncodedMediaResult(
                    type = "image",
                    dataUrlOrUri = "",
                    fileName = displayName,
                    fileSizeBytes = sizeBytes,
                    errorMessage = "Could not encode selected photo under cloud size limit."
                )
            }
        }

        // Video inline encoding for Firestore real-time delivery (<= ~340KB raw)
        return try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(chunk)
                    if (read == -1) break
                    total += read
                    if (total > MAX_INLINE_RAW_BINARY_BYTES) {
                        return EncodedMediaResult(
                            type = "video",
                            dataUrlOrUri = "",
                            fileName = displayName,
                            fileSizeBytes = sizeBytes.takeIf { it > 0 } ?: total.toLong(),
                            errorMessage = "Video exceeds 340 KB inline sync limit. Trim clip or share an HTTPS video URL."
                        )
                    }
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            } ?: ByteArray(0)

            val resolvedMime = mime.ifBlank { "video/mp4" }
            val dataUrl = "data:$resolvedMime;base64," + Base64.encodeToString(rawBytes, Base64.NO_WRAP)
            EncodedMediaResult(
                type = "video",
                dataUrlOrUri = dataUrl,
                fileName = displayName.ifBlank { "Video_${System.currentTimeMillis()}.mp4" },
                fileSizeBytes = rawBytes.size.toLong()
            )
        } catch (e: Exception) {
            EncodedMediaResult(
                type = "video",
                dataUrlOrUri = "",
                fileName = displayName,
                fileSizeBytes = sizeBytes,
                errorMessage = e.localizedMessage ?: "Failed to read selected video."
            )
        }
    }

    fun encodeDocumentOrFile(context: Context, uri: Uri): EncodedMediaResult {
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase()
        val (displayName, sizeBytes) = getFileMetadata(context, uri)
        if (mime.startsWith("image/")) {
            return encodeVisualMedia(context, uri)
        }
        return try {
            val rawBytes = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(8192)
                var total = 0
                while (true) {
                    val read = input.read(chunk)
                    if (read == -1) break
                    total += read
                    if (total > MAX_INLINE_RAW_BINARY_BYTES) {
                        // Fall back to metadata + content URI reference if larger than inline limit
                        return EncodedMediaResult(
                            type = "file",
                            dataUrlOrUri = uri.toString(),
                            fileName = displayName,
                            fileSizeBytes = sizeBytes.takeIf { it > 0 } ?: total.toLong()
                        )
                    }
                    buffer.write(chunk, 0, read)
                }
                buffer.toByteArray()
            } ?: ByteArray(0)

            val resolvedMime = mime.ifBlank { "application/octet-stream" }
            val dataUrl = "data:$resolvedMime;base64," + Base64.encodeToString(rawBytes, Base64.NO_WRAP)
            val resolvedType = if (mime.startsWith("video/")) "video" else "file"
            EncodedMediaResult(
                type = resolvedType,
                dataUrlOrUri = dataUrl,
                fileName = displayName,
                fileSizeBytes = rawBytes.size.toLong()
            )
        } catch (_: Exception) {
            EncodedMediaResult(
                type = "file",
                dataUrlOrUri = uri.toString(),
                fileName = displayName,
                fileSizeBytes = sizeBytes
            )
        }
    }

    fun resolvePlayableVideoUri(context: Context, mediaUrl: String): Uri? {
        return try {
            if (mediaUrl.startsWith("data:video/", ignoreCase = true)) {
                val base64Part = mediaUrl.substringAfter("base64,", "")
                if (base64Part.isBlank()) return null
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                val outFile = File(context.cacheDir, "sat_video_${mediaUrl.hashCode()}.mp4")
                if (!outFile.exists() || outFile.length() != bytes.size.toLong()) {
                    FileOutputStream(outFile).use { it.write(bytes) }
                }
                Uri.fromFile(outFile)
            } else {
                Uri.parse(mediaUrl)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
        var name = "Attachment"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
        }
        return name to size
    }

    private fun scaleBitmap(original: Bitmap, maxDim: Int): Bitmap {
        val width = original.width
        val height = original.height
        if (width <= maxDim && height <= maxDim) return original
        val ratio = width.toFloat() / height.toFloat()
        val (targetW, targetH) = if (ratio > 1f) {
            maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
        }
        return Bitmap.createScaledBitmap(original, targetW, targetH, true)
    }
}
