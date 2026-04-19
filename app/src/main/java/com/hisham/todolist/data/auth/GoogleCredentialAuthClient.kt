package com.hisham.todolist.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.hisham.todolist.core.lifecycle.CurrentActivityTracker
import com.hisham.todolist.domain.model.UserSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

class GoogleCredentialAuthClient @Inject constructor(
    @ApplicationContext context: Context,
    private val currentActivityTracker: CurrentActivityTracker,
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(webClientId: String): UserSession {
        if (webClientId.isBlank()) {
            throw IllegalStateException(
                "Falta GOOGLE_WEB_CLIENT_ID. Configura esa propiedad de Gradle para activar el login con Google.",
            )
        }

        val activity = currentActivityTracker.currentActivity()
            ?: throw IllegalStateException("No hay una Activity activa para iniciar el flujo de Google.")

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetSignInWithGoogleOption.Builder(webClientId)
                    .setNonce(generateNonce())
                    .build(),
            )
            .build()

        val result = credentialManager.getCredential(
            context = activity,
            request = request,
        )

        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google devolvió un tipo de credencial no soportado.")
        }

        val googleCredential = try {
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (exception: GoogleIdTokenParsingException) {
            throw IllegalStateException("La credencial de Google no se pudo interpretar.", exception)
        }

        val email = googleCredential.id
        return UserSession(
            userId = email,
            displayName = googleCredential.displayName ?: email.substringBefore("@"),
            email = email,
            photoUrl = googleCredential.profilePictureUri?.toString(),
        )
    }

    suspend fun clearCredentialState() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }

    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rawNonce.toByteArray())
        return hash.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
