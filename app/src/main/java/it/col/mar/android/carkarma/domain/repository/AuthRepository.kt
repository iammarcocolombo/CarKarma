package it.col.mar.android.carkarma.domain.repository

import android.content.Intent
import android.content.IntentSender
import it.col.mar.android.carkarma.data.model.UserData

/**
 * Contratto di dominio per la gestione dell'autenticazione.
 */
interface AuthRepository {
    fun getSignedInUser(): UserData?
    suspend fun signIn(): IntentSender?
    suspend fun signInWithIntent(intent: Intent): SignInResult
    suspend fun signOut()
    suspend fun deleteAccount(): Boolean
}

/**
 * Wrapper per il risultato dell'autenticazione.
 */
data class SignInResult(
    val data: UserData?,
    val errorMessage: String?
)