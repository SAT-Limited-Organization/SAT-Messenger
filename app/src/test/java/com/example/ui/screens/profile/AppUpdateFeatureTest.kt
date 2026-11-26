package com.example.ui.screens.profile

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PrivacySettings
import com.example.data.model.UserProfile
import com.example.data.repository.AppReleaseInfo
import com.example.data.repository.AppUpdateRepository
import com.example.data.repository.AppUpdateUiState
import com.example.data.repository.AppVersionComparator
import com.example.data.repository.GitHubReleaseAssetDto
import com.example.data.repository.GitHubReleaseDto
import com.example.data.repository.GitHubReleasesApi
import com.example.ui.theme.SATMessengerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h1200dp")
class AppUpdateFeatureTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:815554441605:android:test")
                    .setApiKey("AIzaSyTestKeyForUnitTestsOnly")
                    .setProjectId("sat-limited")
                    .build()
            )
        }
    }

    @Test
    fun versionComparator_handlesMultiSegmentAndPrefixedVersions() {
        assertEquals(
            "3.2.07.11.2026.14.5.0",
            AppVersionComparator.normalizeVersion("SAT Messenger v3.2.07.11.2026.14.5.0")
        )
        assertFalse(
            AppVersionComparator.isUpdateAvailable(
                installed = "3.2.07.11.2026.14.5.0",
                remote = "v3.2.07.11.2026.14.5.0"
            )
        )
        assertTrue(
            AppVersionComparator.isUpdateAvailable(
                installed = "3.2.07.11.2026.14.5.0",
                remote = "v3.3.07.11.2026.14.5.0"
            )
        )
        assertTrue(
            AppVersionComparator.isUpdateAvailable(
                installed = "3.2.07.11.2026.14.5.0",
                remote = "v3.10.0.0.0.0"
            )
        )
        assertTrue(
            AppVersionComparator.isUpdateAvailable(
                installed = "3.2.0-beta.1",
                remote = "v3.2.0"
            )
        )
        assertFalse(
            AppVersionComparator.isUpdateAvailable(
                installed = "4.0.0",
                remote = "v3.2.07.11.2026.14.5.0"
            )
        )
    }

    @Test
    fun appUpdateRepository_returnsUpdateAvailableWithTrackMatchingApk() = runTest {
        val fakeApi = object : GitHubReleasesApi {
            override suspend fun getLatestRelease(owner: String, repo: String): GitHubReleaseDto {
                return GitHubReleaseDto(
                    tag_name = "v3.3.07.11.2026.14.5.0",
                    name = "SAT Messenger v3.3.07.11.2026.14.5.0",
                    body = "Added faster sync and Media improvements.",
                    html_url = "https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/tag/v3.3.07.11.2026.14.5.0",
                    assets = listOf(
                        GitHubReleaseAssetDto(
                            name = "sat-messenger-debug.apk",
                            browser_download_url = "https://example.com/sat-messenger-debug.apk",
                            size = 15_728_640L
                        ),
                        GitHubReleaseAssetDto(
                            name = "sat-messenger-release.apk",
                            browser_download_url = "https://example.com/sat-messenger-release.apk",
                            size = 12_582_912L
                        )
                    )
                )
            }

            override suspend fun listReleases(owner: String, repo: String, perPage: Int): List<GitHubReleaseDto> = emptyList()
        }

        val repo = AppUpdateRepository(api = fakeApi, clockMillis = { 1000L })

        val debugResult = repo.checkForUpdate("3.2.07.11.2026.14.5.0", isDebugBuild = true)
        assertTrue(debugResult is AppUpdateUiState.UpdateAvailable)
        debugResult as AppUpdateUiState.UpdateAvailable
        assertEquals("sat-messenger-debug.apk", debugResult.latestRelease.apkAssetName)
        assertEquals("3.3.07.11.2026.14.5.0", debugResult.latestRelease.versionName)

        val releaseResult = repo.checkForUpdate("3.2.07.11.2026.14.5.0", isDebugBuild = false)
        assertTrue(releaseResult is AppUpdateUiState.UpdateAvailable)
        releaseResult as AppUpdateUiState.UpdateAvailable
        assertEquals("sat-messenger-release.apk", releaseResult.latestRelease.apkAssetName)

        val upToDateResult = repo.checkForUpdate("3.3.07.11.2026.14.5.0", isDebugBuild = true)
        assertTrue(upToDateResult is AppUpdateUiState.UpToDate)
    }

    @Test
    fun appUpdateRepository_fallsBackToReleasesListOn404AndHandlesNetworkError() = runTest {
        val fallbackApi = object : GitHubReleasesApi {
            override suspend fun getLatestRelease(owner: String, repo: String): GitHubReleaseDto {
                throw HttpException(
                    Response.error<GitHubReleaseDto>(
                        404,
                        "{}".toResponseBody("application/json".toMediaType())
                    )
                )
            }

            override suspend fun listReleases(owner: String, repo: String, perPage: Int): List<GitHubReleaseDto> {
                return listOf(
                    GitHubReleaseDto(
                        tag_name = "v4.0.0",
                        name = "SAT Messenger v4.0.0",
                        prerelease = true,
                        html_url = "https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/tag/v4.0.0"
                    )
                )
            }
        }

        val fallbackRepo = AppUpdateRepository(api = fallbackApi, clockMillis = { 2000L })
        val fallbackResult = fallbackRepo.checkForUpdate("3.2.07.11.2026.14.5.0", isDebugBuild = true)
        assertTrue(fallbackResult is AppUpdateUiState.UpdateAvailable)
        assertEquals("4.0.0", (fallbackResult as AppUpdateUiState.UpdateAvailable).latestRelease.versionName)

        val failingApi = object : GitHubReleasesApi {
            override suspend fun getLatestRelease(owner: String, repo: String): GitHubReleaseDto {
                throw IOException("offline")
            }

            override suspend fun listReleases(owner: String, repo: String, perPage: Int): List<GitHubReleaseDto> = emptyList()
        }
        val errorResult = AppUpdateRepository(api = failingApi).checkForUpdate("3.2.0", isDebugBuild = true)
        assertTrue(errorResult is AppUpdateUiState.Error)
        assertTrue((errorResult as AppUpdateUiState.Error).message.contains("internet", ignoreCase = true))
    }

    @Test
    fun profileScreen_displaysUpdateAvailableCardAndTriggersCallbacks() {
        var checkClicked = false
        var promptOpened = false
        var downloadedRelease: AppReleaseInfo? = null

        val release = AppReleaseInfo(
            versionName = "3.4.07.11.2026.14.5.0",
            tagName = "v3.4.07.11.2026.14.5.0",
            releaseTitle = "SAT Messenger v3.4",
            releaseNotes = "Includes version check in Settings.",
            releasePageUrl = "https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/latest",
            apkDownloadUrl = "https://example.com/sat-messenger-debug.apk",
            apkAssetName = "sat-messenger-debug.apk",
            apkSizeBytes = 14_000_000L,
            publishedAt = "2026-11-25T20:00:00Z"
        )

        composeRule.setContent {
            SATMessengerTheme {
                ProfileScreen(
                    userProfile = UserProfile(
                        userId = "u1",
                        displayName = "Simba Tembo",
                        email = "simba@satlimited.com"
                    ),
                    privacySettings = PrivacySettings(userId = "u1"),
                    blockedUsers = emptyList(),
                    onBack = {},
                    onUpdateProfile = { _, _, _ -> },
                    onUpdatePrivacy = { _, _ -> },
                    onUnblockUser = {},
                    onSignOut = {},
                    onDeleteAccount = {},
                    appUpdateState = AppUpdateUiState.UpdateAvailable(
                        installedVersion = "3.2.07.11.2026.14.5.0",
                        latestRelease = release,
                        checkedAtMillis = 123456L
                    ),
                    onCheckForUpdates = { checkClicked = true },
                    onOpenUpdatePrompt = { promptOpened = true },
                    onDownloadUpdate = { downloadedRelease = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Profile & Settings").assertIsDisplayed()
        composeRule.onNodeWithTag("app_update_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("app_update_status_badge").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Update Available").assertIsDisplayed()
        composeRule.onNodeWithTag("download_update_button").performScrollTo().performClick()
        assertEquals(release, downloadedRelease)

        composeRule.onNodeWithTag("view_release_notes_button").performScrollTo().performClick()
        assertTrue(promptOpened)

        composeRule.onNodeWithTag("check_updates_button").performScrollTo().performClick()
        assertTrue(checkClicked)
    }

    @Test
    fun updatePromptDialog_displaysComparisonAndTriggersActions() {
        var downloaded = false
        var openedGitHub = false
        var dismissed = false

        val release = AppReleaseInfo(
            versionName = "4.0.0",
            tagName = "v4.0.0",
            releaseTitle = "SAT Messenger v4.0.0",
            releaseNotes = "- Faster messaging\n- Settings update prompt",
            releasePageUrl = "https://github.com/SAT-Limited-Organization/SAT-Messenger/releases/tag/v4.0.0",
            apkDownloadUrl = "https://example.com/sat-messenger-debug.apk",
            apkAssetName = "sat-messenger-debug.apk",
            apkSizeBytes = 10_485_760L,
            publishedAt = null
        )

        composeRule.setContent {
            SATMessengerTheme {
                UpdatePromptDialog(
                    installedVersion = "3.2.07.11.2026.14.5.0",
                    release = release,
                    onDownloadUpdate = { downloaded = true },
                    onOpenGitHubRelease = { openedGitHub = true },
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("update_prompt_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("update_dialog_version_comparison").assertIsDisplayed()
        composeRule.onNodeWithTag("update_dialog_download_button").performClick()
        assertTrue(downloaded)

        composeRule.onNodeWithTag("update_dialog_github_button").performClick()
        assertTrue(openedGitHub)

        composeRule.onNodeWithTag("update_dialog_later_button").performClick()
        assertTrue(dismissed)
    }
}
