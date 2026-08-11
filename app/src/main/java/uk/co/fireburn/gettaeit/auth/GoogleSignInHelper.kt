package uk.co.fireburn.gettaeit.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Wraps Credential Manager's "Sign in with Google" flow. Lives in :app (not :shared)
 * because it needs an Activity-hosting Context to show the account picker UI.
 */
object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    /**
     * The web client ID is generated into R.string.default_web_client_id by the
     * google-services Gradle plugin from google-services.json. Looked up by name
     * (rather than referenced as R.string.default_web_client_id directly) so the
     * app still compiles before that file exists — see :app/build.gradle.kts.
     */
    fun webClientId(context: Context): String {
        val resId = context.resources
            .getIdentifier("default_web_client_id", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else ""
    }

    /** Returns the raw Google ID token, or null if cancelled, unconfigured, or it failed. */
    suspend fun requestIdToken(context: Context): String? {
        val clientId = webClientId(context)
        if (clientId.isBlank()) {
            Log.w(TAG, "No default_web_client_id — Firebase isn't set up yet.")
            return null
        }

        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(clientId)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val response = CredentialManager.create(context).getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                Log.w(TAG, "Unexpected credential type: ${credential.type}")
                null
            }
        } catch (e: GetCredentialException) {
            Log.i(TAG, "Sign-in cancelled or unavailable: ${e.message}")
            null
        } catch (e: GoogleIdTokenParsingException) {
            Log.w(TAG, "Couldn't parse the Google ID token: ${e.message}")
            null
        }
    }
}
