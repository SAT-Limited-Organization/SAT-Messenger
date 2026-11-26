package com.example.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GitHubReleaseAssetDto(
    val name: String? = null,
    val browser_download_url: String? = null,
    val size: Long = 0L
)

data class GitHubReleaseDto(
    val tag_name: String? = null,
    val name: String? = null,
    val body: String? = null,
    val html_url: String? = null,
    val published_at: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList()
)

interface GitHubReleasesApi {
    @Headers(
        "Accept: application/vnd.github+json",
        "X-GitHub-Api-Version: 2022-11-28",
        "User-Agent: SAT-Messenger-Android"
    )
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseDto

    @Headers(
        "Accept: application/vnd.github+json",
        "X-GitHub-Api-Version: 2022-11-28",
        "User-Agent: SAT-Messenger-Android"
    )
    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 10
    ): List<GitHubReleaseDto>
}

data class AppReleaseInfo(
    val versionName: String,
    val tagName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val releasePageUrl: String,
    val apkDownloadUrl: String?,
    val apkAssetName: String?,
    val apkSizeBytes: Long?,
    val publishedAt: String?,
    val isPreRelease: Boolean = false
)

sealed interface AppUpdateUiState {
    val installedVersion: String

    data class Idle(
        override val installedVersion: String
    ) : AppUpdateUiState

    data class Checking(
        override val installedVersion: String
    ) : AppUpdateUiState

    data class UpToDate(
        override val installedVersion: String,
        val latestRelease: AppReleaseInfo?,
        val checkedAtMillis: Long
    ) : AppUpdateUiState

    data class UpdateAvailable(
        override val installedVersion: String,
        val latestRelease: AppReleaseInfo,
        val checkedAtMillis: Long
    ) : AppUpdateUiState

    data class Error(
        override val installedVersion: String,
        val message: String
    ) : AppUpdateUiState
}

object AppVersionComparator {

    private data class ParsedVersion(
        val numericSegments: List<Long>,
        val preReleaseParts: List<String>
    )

    private val versionTokenRegex = Regex("""\d+(?:\.\d+)*(?:-[0-9A-Za-z.-]+)?""")

    /**
     * Extracts a clean version string from release tags/names such as
     * "v3.2.07.11.2026.14.5.0" or "SAT Messenger v3.2.07.11.2026.14.5.0".
     */
    fun normalizeVersion(raw: String): String {
        val trimmed = raw.trim().substringBefore('+').trim()
        if (trimmed.isEmpty()) return ""
        val withoutPrefix = trimmed
            .replace(Regex("""^(?i)sat\s*messenger\s*"""), "")
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .trim()
        return versionTokenRegex.find(withoutPrefix)?.value ?: withoutPrefix
    }

    /**
     * Compares [installed] against [remote].
     * Returns < 0 when [remote] is newer than [installed], 0 when equivalent, > 0 when [installed] is newer.
     */
    fun compareVersions(installed: String, remote: String): Int {
        val left = parse(installed)
        val right = parse(remote)

        if (left == null && right == null) {
            return normalizeVersion(installed).compareTo(normalizeVersion(remote), ignoreCase = true)
        }
        if (left == null) return -1
        if (right == null) return 1

        val maxLen = maxOf(left.numericSegments.size, right.numericSegments.size)
        for (i in 0 until maxLen) {
            val lSeg = left.numericSegments.getOrElse(i) { 0L }
            val rSeg = right.numericSegments.getOrElse(i) { 0L }
            if (lSeg != rSeg) {
                return lSeg.compareTo(rSeg)
            }
        }

        // Same numeric core: stable release > pre-release
        if (left.preReleaseParts.isEmpty() && right.preReleaseParts.isEmpty()) return 0
        if (left.preReleaseParts.isEmpty()) return 1
        if (right.preReleaseParts.isEmpty()) return -1

        val maxPre = maxOf(left.preReleaseParts.size, right.preReleaseParts.size)
        for (i in 0 until maxPre) {
            val lPart = left.preReleaseParts.getOrNull(i) ?: return -1
            val rPart = right.preReleaseParts.getOrNull(i) ?: return 1
            val lNum = lPart.toLongOrNull()
            val rNum = rPart.toLongOrNull()
            val cmp = when {
                lNum != null && rNum != null -> lNum.compareTo(rNum)
                lNum != null -> -1 // numeric identifiers have lower precedence than non-numeric per SemVer
                rNum != null -> 1
                else -> lPart.compareTo(rPart, ignoreCase = true)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }

    fun isUpdateAvailable(installed: String, remote: String): Boolean {
        val normalizedRemote = normalizeVersion(remote)
        if (normalizedRemote.isBlank()) return false
        return compareVersions(installed, normalizedRemote) < 0
    }

    private fun parse(raw: String): ParsedVersion? {
        val normalized = normalizeVersion(raw)
        if (normalized.isBlank()) return null

        val dashIndex = normalized.indexOf('-')
        val corePart = if (dashIndex >= 0) normalized.substring(0, dashIndex) else normalized
        val prePart = if (dashIndex >= 0) normalized.substring(dashIndex + 1) else ""

        val segments = corePart.split('.')
            .filter { it.isNotEmpty() }
            .map { it.toLongOrNull() ?: return null }

        if (segments.isEmpty()) return null

        val preParts = if (prePart.isBlank()) {
            emptyList()
        } else {
            prePart.split('.').filter { it.isNotBlank() }
        }
        return ParsedVersion(numericSegments = segments, preReleaseParts = preParts)
    }
}

class AppUpdateRepository(
    private val api: GitHubReleasesApi = createDefaultApi(),
    private val owner: String = DEFAULT_GITHUB_OWNER,
    private val repo: String = DEFAULT_GITHUB_REPO,
    private val clockMillis: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun checkForUpdate(
        installedVersion: String,
        isDebugBuild: Boolean
    ): AppUpdateUiState = withContext(Dispatchers.IO) {
        try {
            val releaseDto = fetchLatestNonDraftRelease()
                ?: return@withContext AppUpdateUiState.UpToDate(
                    installedVersion = installedVersion,
                    latestRelease = null,
                    checkedAtMillis = clockMillis()
                )

            val releaseInfo = mapRelease(releaseDto, isDebugBuild)
            val checkedAt = clockMillis()

            if (AppVersionComparator.isUpdateAvailable(installedVersion, releaseInfo.versionName)) {
                AppUpdateUiState.UpdateAvailable(
                    installedVersion = installedVersion,
                    latestRelease = releaseInfo,
                    checkedAtMillis = checkedAt
                )
            } else {
                AppUpdateUiState.UpToDate(
                    installedVersion = installedVersion,
                    latestRelease = releaseInfo,
                    checkedAtMillis = checkedAt
                )
            }
        } catch (e: HttpException) {
            val message = when (e.code()) {
                403, 429 -> "GitHub API rate limit reached. Please try again in a few minutes."
                404 -> "No published releases found on GitHub yet."
                else -> "GitHub returned HTTP ${e.code()} while checking for updates."
            }
            AppUpdateUiState.Error(installedVersion = installedVersion, message = message)
        } catch (e: IOException) {
            AppUpdateUiState.Error(
                installedVersion = installedVersion,
                message = "Unable to reach GitHub. Check your internet connection and try again."
            )
        } catch (e: Exception) {
            AppUpdateUiState.Error(
                installedVersion = installedVersion,
                message = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Failed to check for updates."
            )
        }
    }

    private suspend fun fetchLatestNonDraftRelease(): GitHubReleaseDto? {
        return try {
            val latest = api.getLatestRelease(owner, repo)
            if (!latest.draft) latest else findFirstPublishedRelease()
        } catch (e: HttpException) {
            if (e.code() == 404) {
                findFirstPublishedRelease()
            } else {
                throw e
            }
        }
    }

    private suspend fun findFirstPublishedRelease(): GitHubReleaseDto? {
        return api.listReleases(owner, repo, perPage = 10)
            .firstOrNull { !it.draft }
    }

    private fun mapRelease(dto: GitHubReleaseDto, isDebugBuild: Boolean): AppReleaseInfo {
        val rawTag = dto.tag_name.orEmpty().ifBlank { dto.name.orEmpty() }
        val normalizedFromTag = AppVersionComparator.normalizeVersion(rawTag)
        val normalizedVersion = normalizedFromTag.ifBlank {
            AppVersionComparator.normalizeVersion(dto.name.orEmpty())
        }.ifBlank { rawTag }

        val apkAssets = dto.assets.filter { asset ->
            val name = asset.name.orEmpty().lowercase()
            name.endsWith(".apk") && !asset.browser_download_url.isNullOrBlank()
        }

        val preferredAsset = if (isDebugBuild) {
            apkAssets.firstOrNull { it.name.orEmpty().contains("debug", ignoreCase = true) }
                ?: apkAssets.firstOrNull()
        } else {
            apkAssets.firstOrNull { it.name.orEmpty().contains("release", ignoreCase = true) }
                ?: apkAssets.firstOrNull { !it.name.orEmpty().contains("debug", ignoreCase = true) }
                ?: apkAssets.firstOrNull()
        }

        val fallbackPageUrl = dto.html_url?.takeIf { it.isNotBlank() }
            ?: "https://github.com/$owner/$repo/releases/latest"

        return AppReleaseInfo(
            versionName = normalizedVersion,
            tagName = rawTag.ifBlank { "v$normalizedVersion" },
            releaseTitle = dto.name?.takeIf { it.isNotBlank() } ?: "SAT Messenger v$normalizedVersion",
            releaseNotes = dto.body?.trim().orEmpty(),
            releasePageUrl = fallbackPageUrl,
            apkDownloadUrl = preferredAsset?.browser_download_url,
            apkAssetName = preferredAsset?.name,
            apkSizeBytes = preferredAsset?.size?.takeIf { it > 0L },
            publishedAt = dto.published_at,
            isPreRelease = dto.prerelease
        )
    }

    companion object {
        const val DEFAULT_GITHUB_OWNER = "SAT-Limited-Organization"
        const val DEFAULT_GITHUB_REPO = "SAT-Messenger"
        private const val GITHUB_API_BASE_URL = "https://api.github.com/"

        fun createDefaultApi(): GitHubReleasesApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(GITHUB_API_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(GitHubReleasesApi::class.java)
        }
    }
}
