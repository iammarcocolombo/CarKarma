package it.col.mar.android.carkarma.data.database

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import it.col.mar.android.carkarma.data.model.UserData
import it.col.mar.android.carkarma.domain.repository.AuthRepository
import it.col.mar.android.carkarma.domain.repository.SignInResult
import it.col.mar.android.carkarma.util.Config
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

class AuthRepositoryImpl(
    context: Context,
    private val auth: FirebaseAuth
) : AuthRepository {

    private val oneTapClient: SignInClient = Identity.getSignInClient(context)

    override fun getSignedInUser(): UserData? = auth.currentUser?.run {
        UserData(
            userId = uid,
            username = displayName,
            profilePictureUrl = photoUrl?.toString(),
            email = email
        )
    }

    override suspend fun signIn(): IntentSender? {
        val result = try {
            oneTapClient.beginSignIn(
                BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(Config.GOOGLE_WEB_CLIENT_ID)
                            .build()
                    )
                    .setAutoSelectEnabled(true)
                    .build()
            ).await()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
        return result?.pendingIntent?.intentSender
    }

    override suspend fun signInWithIntent(intent: Intent): SignInResult {
        return try {
            val credential = oneTapClient.getSignInCredentialFromIntent(intent)
            val googleIdToken = credential.googleIdToken
            val googleCredentials = GoogleAuthProvider.getCredential(googleIdToken, null)

            val user = auth.signInWithCredential(googleCredentials).await().user
            SignInResult(
                data = user?.let {
                    UserData(
                        userId = it.uid,
                        username = it.displayName,
                        profilePictureUrl = it.photoUrl?.toString(),
                        email = it.email
                    )
                },
                errorMessage = null
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            SignInResult(data = null, errorMessage = e.localizedMessage)
        }
    }

    override suspend fun signOut() {
        try {
            oneTapClient.signOut().await()
            auth.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun deleteAccount(): Boolean {
        return try {
            auth.currentUser?.delete()?.await()
            oneTapClient.signOut().await()
            true
        } catch (_: Exception) {
            false
        }
    }
}