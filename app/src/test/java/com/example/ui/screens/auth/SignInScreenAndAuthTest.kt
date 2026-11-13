package com.example.ui.screens.auth

import android.app.Activity
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
import com.example.data.repository.AuthProvider
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthUiState
import com.example.ui.theme.SATMessengerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWebException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun signInScreen_rendersBothGoogleAndMicrosoftButtons_andInvokesCallbacks() {
        var googleClicks = 0
        var microsoftClicks = 0

        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Unauthenticated,
                    onGoogleSignInClick = { googleClicks++ },
                    onMicrosoftSignInClick = { microsoftClicks++ }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("microsoft_sign_in_button").performScrollTo().assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()
        composeRule.onNodeWithText("Continue with Microsoft").assertIsDisplayed()

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().performClick()
        composeRule.onNodeWithTag("microsoft_sign_in_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(1, googleClicks)
        assertEquals(1, microsoftClicks)
    }

    @Test
    fun signInScreen_disablesBothButtonsDuringMicrosoftLoading() {
        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Loading,
                    activeProvider = AuthProvider.MICROSOFT,
                    onGoogleSignInClick = {},
                    onMicrosoftSignInClick = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("google_sign_in_button").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("microsoft_sign_in_button").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun signInScreen_displaysErrorCard_andInvokesDismissCallback() {
        var dismissed = false

        composeRule.setContent {
            SATMessengerTheme {
                SignInScreen(
                    authState = AuthUiState.Error("Microsoft Sign-In not enabled yet"),
                    onGoogleSignInClick = {},
                    onMicrosoftSignInClick = {},
                    onDismissError = { dismissed = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("auth_error_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Microsoft Sign-In not enabled yet").assertIsDisplayed()
        composeRule.onNodeWithTag("dismiss_error_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(dismissed)
    }

    @Test
    fun authRepository_buildMicrosoftProvider_andErrorMapping() {
        val repo = AuthRepository(context)
        val provider = repo.buildMicrosoftProvider()
        assertEquals("microsoft.com", provider.providerId)

        val collisionMsg = repo.mapMicrosoftAuthError(
            FirebaseAuthUserCollisionException("ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "exists")
        )
        assertTrue(collisionMsg?.contains("Google", ignoreCase = true) == true)

        val notAllowedMsg = repo.mapMicrosoftAuthError(
            FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", "disabled")
        )
        assertTrue(notAllowedMsg?.contains("Firebase Console", ignoreCase = true) == true)

        val cancelledResult = repo.mapMicrosoftAuthError(
            FirebaseAuthWebException("ERROR_WEB_CONTEXT_CANCELED", "The web operation was canceled by the user.")
        )
        assertNull(cancelledResult)
    }
}
