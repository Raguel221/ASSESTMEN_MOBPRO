package com.raguel0087.myapplication1.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.raguel0087.myapplication1.R
import kotlinx.coroutines.tasks.await

/**
 * Handles Google Sign-In using the modern Credential Manager API.
 * Replaces the deprecated GoogleSignInClient approach.
 */
class GoogleSignInHelper(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    /**
     * Returns the currently signed-in Firebase user, or null if not signed in.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Launches the Google Sign-In bottom sheet and authenticates with Firebase.
     * @param activityContext Must be an Activity context for Credential Manager to present UI.
     * @return FirebaseUser on success.
     * @throws Exception with a descriptive message on failure.
     */
    suspend fun signIn(activityContext: Context): FirebaseUser {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(activityContext.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = try {
            credentialManager.getCredential(
                request = request,
                context = activityContext
            )
        } catch (e: GetCredentialCancellationException) {
            throw Exception("Sign-in cancelled by user.")
        } catch (e: Exception) {
            throw Exception("Google Sign-In failed: ${e.localizedMessage}")
        }

        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw Exception("Unexpected credential type received.")
        }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

        val authResult = auth.signInWithCredential(firebaseCredential).await()
        return authResult.user ?: throw Exception("Authentication succeeded but user is null.")
    }

    /**
     * Signs out from Firebase.
     */
    suspend fun signOut() {
        auth.signOut()
        // Credential Manager doesn't require explicit sign-out for Google;
        // clearing Firebase auth is sufficient for app-level logout.
    }
}
