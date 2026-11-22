package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            _authState.value = AuthUiState.Authenticated(user)
        } else if (_authState.value !is AuthUiState.Loading && _authState.value !is AuthUiState.Error) {
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun clearError() {
        if (_authState.value is AuthUiState.Error) {
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    fun attemptAutoSignIn(scope: CoroutineScope) {
        auth.currentUser?.let { currentUser ->
            _authState.value = AuthUiState.Authenticated(currentUser)
            return
        }

        scope.launch {
            val clientId = resolveDefaultWebClientId(context) ?: run {
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

    @Suppress("DiscouragedApi")
    internal fun resolveDefaultWebClientId(ctx: Context = context): String? {
        return try {
            val resId = ctx.resources.getIdentifier("default_web_client_id", "string", ctx.packageName)
            if (resId != 0) ctx.getString(resId).trim().ifBlank { null } else null
        } catch (_: Exception) {
            null
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onError: (String) -> Unit = {}
    ) {
        val clientId = resolveDefaultWebClientId(activity) ?: run {
            val err = "Google Sign-In configuration missing: add app/google-services.json and rebuild."
            _authState.value = AuthUiState.Error(err)
            onError(err)
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        _authState.value = AuthUiState.Loading

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let {
                        _authState.value = AuthUiState.Authenticated(it)
                    } ?: run {
                        _authState.value = AuthUiState.Error("Google sign-in failed")
                        onError("Google sign-in failed")
                    }
                } else {
                    val err = "Unexpected credential returned"
                    _authState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
                _authState.value = AuthUiState.Unauthenticated
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                val errorMsg = e.localizedMessage ?: "Google sign-in failed"
                _authState.value = AuthUiState.Error(errorMsg)
                onError(errorMsg)
            }
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        auth.signOut()
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
}
