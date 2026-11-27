package com.example.ui.screens.auth

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.BuildConfig
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthUiState
import com.example.ui.theme.SATMessengerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SignInScreenAndAuthTest {

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
    fun signInScreen_rendersGoogleSignInButton_andInvokesCallback() {
        var googleClicks = 0

        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Unauthenticated,
                    onGoogleSignInClick = { googleClicks++ }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()
        composeRule.onNodeWithTag("microsoft_sign_in_button").assertDoesNotExist()

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, googleClicks)
    }

    @Test
    fun signInScreen_disablesGoogleButtonDuringLoading() {
        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Loading,
                    onGoogleSignInClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun signInScreen_displaysErrorCard_andInvokesDismissCallback() {
        var dismissed = false

        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Error("Google sign-in failed"),
                    onGoogleSignInClick = {},
                    onDismissError = { dismissed = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("auth_error_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Google sign-in failed").assertIsDisplayed()
        composeRule.onNodeWithTag("dismiss_error_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(dismissed)
    }

    @Test
    fun authRepository_resolvesDefaultWebClientId() {
        val repo = AuthRepository(context)
        assertTrue(!repo.resolveDefaultWebClientId().isNullOrBlank())
    }

    @Test
    fun signInScreen_rendersManualButton_andInvokesCallbackOrEmbeddedManual() {
        var openManualClicks = 0

        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Unauthenticated,
                    onGoogleSignInClick = {},
                    onOpenManual = { openManualClicks++ }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("registration_manual_button").performScrollTo().assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, openManualClicks)
    }

    @Test
    fun signInScreen_opensEmbeddedManualCopyWhenCallbackIsNull() {
        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Unauthenticated,
                    onGoogleSignInClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("registration_manual_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("SAT Messenger Manual").assertIsDisplayed()
        composeRule.onNodeWithText("Registration & Sign-In (Google)").assertIsDisplayed()
    }

    @Test
    fun signInScreen_displaysCurrentBuildConfigVersionFooter() {
        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Unauthenticated,
                    onGoogleSignInClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("signin_app_version_text").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(
            "SAT Messenger v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        ).assertIsDisplayed()
    }
}
