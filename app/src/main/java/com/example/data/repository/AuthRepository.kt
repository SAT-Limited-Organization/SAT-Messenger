package com.example.data.repository

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWebException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class AuthProvider(val providerId: String, val label: String) {
    GOOGLE("google.com", "Google"),
    MICROSOFT("microsoft.com", "Microsoft")
}

sealed interface AuthUiState {
    data object Initial : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: FirebaseUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
    data object Unauthenticated : AuthUiState
}

class AuthRepository(
    private val context: Context,
    private val auth: FirebaseAuth = Firebase.auth,
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) {

    private val _authState = MutableStateFlow<AuthUiState>(
        auth.currentUser?.let { AuthUiState.Authenticated(it) } ?: AuthUiState.Unauthenticated
    )
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _activeProvider = MutableStateFlow<AuthProvider?>(null)
    val activeProvider: StateFlow<AuthProvider?> = _activeProvider.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            _activeProvider.value = null
            _authState.value = AuthUiState.Authenticated(user)
        } else if (_authState.value !is AuthUiState.Loading && _authState.value !is AuthUiState.Error) {
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getLinkedProviderIds(): List<String> {
        val user = auth.currentUser ?: return emptyList()
        return user.providerData
            .map { it.providerId }
            .filter { it.isNotBlank() && it != "firebase" }
            .distinct()
    }

    fun clearError() {
        if (_authState.value is AuthUiState.Error) {
            _activeProvider.value = null
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    internal fun getMicrosoftTenantId(): String {
        return try {
            context.getString(R.string.microsoft_tenant_id).trim().ifBlank { DEFAULT_MICROSOFT_TENANT }
        } catch (_: Exception) {
            DEFAULT_MICROSOFT_TENANT
        }
    }

    internal fun getMicrosoftScopes(): List<String> = MICROSOFT_SCOPES

    internal fun buildMicrosoftProvider(): OAuthProvider {
        return OAuthProvider.newBuilder(AuthProvider.MICROSOFT.providerId)
            .setScopes(getMicrosoftScopes())
            .addCustomParameter("prompt", "select_account")
            .addCustomParameter("tenant", getMicrosoftTenantId())
            .build()
    }

    fun attemptAutoSignIn(scope: CoroutineScope) {
        auth.currentUser?.let { currentUser ->
            _authState.value = AuthUiState.Authenticated(currentUser)
            return
        }

        scope.launch {
            // Recover any pending browser OAuth sign-in result (e.g., Microsoft after process/activity recreation)
            val pendingTask = auth.pendingAuthResult
            if (pendingTask != null) {
                _activeProvider.value = AuthProvider.MICROSOFT
                _authState.value = AuthUiState.Loading
                try {
                    val result = pendingTask.await()
                    val user = result.user
                    if (user != null) {
                        _activeProvider.value = null
                        _authState.value = AuthUiState.Authenticated(user)
                        return@launch
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("Auth", "Pending OAuth result failed or cancelled", e)
                    _activeProvider.value = null
                    val mapped = mapMicrosoftAuthError(e)
                    if (mapped != null) {
                        _authState.value = AuthUiState.Error(mapped)
                        return@launch
                    }
                }
                _activeProvider.value = null
            }

            val clientId = try {
                context.getString(R.string.default_web_client_id)
            } catch (e: Exception) {
                _authState.value = AuthUiState.Unauthenticated
                return@launch
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let {
                        _authState.value = AuthUiState.Authenticated(it)
                    } ?: run {
                        _authState.value = AuthUiState.Unauthenticated
                    }
                } else {
                    _authState.value = AuthUiState.Unauthenticated
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _authState.value = AuthUiState.Unauthenticated
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onError: (String) -> Unit = {}
    ) {
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val err = "Google Sign-In configuration missing: default_web_client_id not found"
            _activeProvider.value = null
            _authState.value = AuthUiState.Error(err)
            onError(err)
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        _activeProvider.value = AuthProvider.GOOGLE
        _authState.value = AuthUiState.Loading

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    _activeProvider.value = null
                    authResult.user?.let {
                        _authState.value = AuthUiState.Authenticated(it)
                    } ?: run {
                        _authState.value = AuthUiState.Error("Google sign-in failed")
                        onError("Google sign-in failed")
                    }
                } else {
                    _activeProvider.value = null
                    val err = "Unexpected credential returned"
                    _authState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
                _activeProvider.value = null
                _authState.value = AuthUiState.Unauthenticated
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                _activeProvider.value = null
                val errorMsg = e.localizedMessage ?: "Google sign-in failed"
                _authState.value = AuthUiState.Error(errorMsg)
                onError(errorMsg)
            }
        }
    }

    fun signInWithMicrosoft(
        activity: Activity,
        scope: CoroutineScope,
        onError: (String) -> Unit = {}
    ) {
        _activeProvider.value = AuthProvider.MICROSOFT
        _authState.value = AuthUiState.Loading

        scope.launch {
            try {
                val pending = auth.pendingAuthResult
                val authResult = if (pending != null) {
                    pending.await()
                } else {
                    auth.startActivityForSignInWithProvider(activity, buildMicrosoftProvider()).await()
                }
                _activeProvider.value = null
                authResult.user?.let {
                    _authState.value = AuthUiState.Authenticated(it)
                } ?: run {
                    val err = "Microsoft sign-in did not return a user account."
                    _authState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("Auth", "Microsoft Sign-In outcome", e)
                _activeProvider.value = null
                val errorMsg = mapMicrosoftAuthError(e)
                if (errorMsg == null) {
                    // User dismissed / cancelled the web sign-in sheet
                    _authState.value = AuthUiState.Unauthenticated
                } else {
                    _authState.value = AuthUiState.Error(errorMsg)
                    onError(errorMsg)
                }
            }
        }
    }

    fun linkMicrosoftAccount(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError("You must be signed in before linking a Microsoft account.")
            return
        }
        scope.launch {
            try {
                val pending = auth.pendingAuthResult
                if (pending != null) {
                    pending.await()
                } else {
                    currentUser.startActivityForLinkWithProvider(activity, buildMicrosoftProvider()).await()
                }
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val mapped = mapMicrosoftAuthError(e)
                if (mapped != null) {
                    onError(mapped)
                }
            }
        }
    }

    internal fun mapMicrosoftAuthError(error: Exception): String? {
        val rawMessage = error.message.orEmpty()
        val isCancelMessage = rawMessage.contains("canceled", ignoreCase = true) ||
            rawMessage.contains("cancelled", ignoreCase = true) ||
            rawMessage.contains("WEB_CONTEXT_CANCELED", ignoreCase = true)

        if (error is FirebaseAuthWebException &&
            (error.errorCode == "ERROR_WEB_CONTEXT_CANCELED" || isCancelMessage)
        ) {
            return null
        }

        if (isCancelMessage && error !is FirebaseAuthUserCollisionException) {
            return null
        }

        if (error is ActivityNotFoundException) {
            return "No compatible browser or Chrome Custom Tabs provider is installed on this device for Microsoft sign-in."
        }

        return when (error) {
            is FirebaseAuthUserCollisionException ->
                "An account already exists with this email using Google Sign-In. Please sign in with Google first, then link Microsoft in Profile settings."
            is FirebaseAuthException -> when (error.errorCode) {
                "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Microsoft Sign-In is not enabled yet in Firebase Console. Enable Microsoft under Authentication -> Sign-in method (redirect URI: $FIREBASE_REDIRECT_URI)."
                "ERROR_CREDENTIAL_ALREADY_IN_USE", "ERROR_EMAIL_ALREADY_IN_USE" ->
                    "This Microsoft account is already linked to another SAT Messenger profile."
                "ERROR_PROVIDER_ALREADY_LINKED" ->
                    "Your Microsoft account is already linked to this profile."
                "ERROR_USER_DISABLED" ->
                    "This Microsoft account has been disabled by your administrator."
                "ERROR_NETWORK_REQUEST_FAILED" ->
                    "Network error while contacting Microsoft or Firebase Authentication. Check your connection and try again."
                "ERROR_WEB_STORAGE_UNSUPPORTED", "ERROR_WEB_INTERNAL_ERROR" ->
                    "Browser sign-in failed to initialize. Update Chrome/WebView on this device and try again."
                "ERROR_INVALID_CREDENTIAL", "ERROR_UNAUTHORIZED_DOMAIN" ->
                    "Microsoft OAuth configuration mismatch. Verify your Azure Application ID, Client Secret value, and redirect URI ($FIREBASE_REDIRECT_URI) in Firebase Console."
                else -> mapRawMicrosoftMessage(rawMessage)
                    ?: error.localizedMessage
                    ?: "Microsoft sign-in failed (${error.errorCode})."
            }
            else -> mapRawMicrosoftMessage(rawMessage)
                ?: error.localizedMessage
                ?: "Microsoft sign-in failed. Please try again."
        }
    }

    private fun mapRawMicrosoftMessage(rawMessage: String): String? {
        if (rawMessage.isBlank()) return null
        return when {
            rawMessage.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                "Microsoft Sign-In is not enabled yet in Firebase Console. Enable Microsoft under Authentication -> Sign-in method (redirect URI: $FIREBASE_REDIRECT_URI)."
            rawMessage.contains("redirect_uri", ignoreCase = true) ||
                rawMessage.contains("AADSTS50011", ignoreCase = true) ->
                "Microsoft redirect URI mismatch. Add $FIREBASE_REDIRECT_URI under Web Redirect URIs in Azure Portal -> App registrations."
            rawMessage.contains("invalid_client", ignoreCase = true) ||
                rawMessage.contains("client_secret", ignoreCase = true) ||
                rawMessage.contains("AADSTS7000215", ignoreCase = true) ||
                rawMessage.contains("AADSTS700016", ignoreCase = true) ->
                "Invalid Microsoft Entra ID Application ID or Client Secret in Firebase Console. Verify the Client Secret Value (not Secret ID) in Firebase Authentication."
            rawMessage.contains("AADSTS50020", ignoreCase = true) ||
                rawMessage.contains("tenant", ignoreCase = true) && rawMessage.contains("AADSTS", ignoreCase = true) ->
                "This Microsoft account does not match the configured Entra ID tenant. Update Supported account types or microsoft_tenant_id in strings.xml."
            else -> null
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        auth.signOut()
        _activeProvider.value = null
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.e("Auth", "Failed to clear credential state", e)
                }
            }
            _authState.value = AuthUiState.Unauthenticated
            onComplete()
        }
    }

    companion object {
        const val DEFAULT_MICROSOFT_TENANT = "common"
        const val FIREBASE_REDIRECT_URI = "https://sat-limited.firebaseapp.com/__/auth/handler"
        val MICROSOFT_SCOPES = listOf("openid", "profile", "email", "User.Read")
    }
}
