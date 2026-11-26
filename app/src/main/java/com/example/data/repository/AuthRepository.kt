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
    private val credentialManager: CredentialManager? = null
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

        // CredentialManager UI/auto-sign-in requires an Activity context; avoid calling it with ApplicationContext
        // so we never race with or overwrite an active manual Google Sign-In flow.
        val activityContext = context as? Activity ?: return

        scope.launch {
            val clientId = resolveDefaultWebClientId(activityContext) ?: run {
                if (_authState.value is AuthUiState.Initial) {
                    _authState.value = AuthUiState.Unauthenticated
                }
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
                val cm = credentialManager ?: CredentialManager.create(activityContext)
                val result = cm.getCredential(activityContext, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let {
                        _authState.value = AuthUiState.Authenticated(it)
                    } ?: run {
                        if (_authState.value is AuthUiState.Initial) {
                            _authState.value = AuthUiState.Unauthenticated
                        }
                    }
                } else if (_authState.value is AuthUiState.Initial) {
                    _authState.value = AuthUiState.Unauthenticated
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (_authState.value is AuthUiState.Initial) {
                    _authState.value = AuthUiState.Unauthenticated
                }
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

    @Suppress("DEPRECATION")
    internal fun resolveAppSigningSha1(ctx: Context = context): String? {
        return try {
            val pm = ctx.packageManager
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(
                    ctx.packageName,
                    android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = info.signingInfo ?: return null
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                pm.getPackageInfo(
                    ctx.packageName,
                    android.content.pm.PackageManager.GET_SIGNATURES
                ).signatures
            }
            val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
            val digest = java.security.MessageDigest.getInstance("SHA-1").digest(certBytes)
            digest.joinToString(":") { "%02X".format(it) }
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
                val cm = credentialManager ?: CredentialManager.create(activity)
                val result = cm.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let {
                        _authState.value = AuthUiState.Authenticated(it)
                    } ?: run {
                        val err = "Firebase sign-in returned no user. Please try again."
                        _authState.value = AuthUiState.Error(err)
                        onError(err)
                    }
                } else {
                    val err = "Unexpected credential type returned (${credential.type})."
                    _authState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled/rejected: ${e.message}", e)
                val sha1 = resolveAppSigningSha1(activity)
                val shaHint = if (sha1 != null) " APK SHA-1: $sha1" else ""
                val msg = "Google Sign-In did not complete (${e.message ?: "cancelled"}). " +
                    "If this happened right after selecting your account, verify that this APK's SHA-1 is registered in Firebase Console and matches your GOOGLE_SERVICES_JSON secret.$shaHint"
                _authState.value = AuthUiState.Error(msg)
                onError(msg)
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                val sha1 = resolveAppSigningSha1(activity)
                val shaHint = if (sha1 != null) " (APK SHA-1: $sha1)" else ""
                val baseMsg = e.localizedMessage?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
                val errorMsg = "Google Sign-In failed: $baseMsg$shaHint"
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
                    val cm = credentialManager ?: CredentialManager.create(context)
                    cm.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.e("Auth", "Failed to clear credential state", e)
                }
            }
            _authState.value = AuthUiState.Unauthenticated
            onComplete()
        }
    }
}
