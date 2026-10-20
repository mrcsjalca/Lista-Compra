package org.insbaixcamp.cestadecompra

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

suspend fun loginConGoogle(context: Context): Boolean {
    val credentialManager = CredentialManager.create(context)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(context.getString(R.string.default_web_client_id))
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val result = credentialManager.getCredential(context, request)
        val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)
        val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(firebaseCred).await()
        true
    } catch (e: Exception) {
        Log.e("Login", "Error Google: ${e.message}")
        false
    }
}