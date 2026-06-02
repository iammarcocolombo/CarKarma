package it.col.mar.android.carkarma.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.col.mar.android.carkarma.domain.repository.SignInResult // CORREZIONE: Import allineato al modello dati globale
import it.col.mar.android.carkarma.data.model.UserData     // CORREZIONE: Import allineato al modello dati globale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun onSignInResult(result: SignInResult) {
        _state.update { it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        ) }
    }

    // --- EMAIL & PASSWORD ---

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(signInError = "Inserisci email e password") }
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, signInError = null) } // Stato caricamento
                auth.signInWithEmailAndPassword(email, pass).await()
                val user = auth.currentUser
                val userData = user?.run {
                    UserData(uid, displayName, photoUrl?.toString(), email)
                }
                onSignInResult(SignInResult(userData, null))
            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is FirebaseAuthInvalidUserException -> "Account non trovato. Registrati prima."
                    is FirebaseAuthInvalidCredentialsException -> "Email o password non validi."
                    else -> "Errore Login: ${e.localizedMessage}"
                }
                _state.update { it.copy(signInError = errorMsg, isLoading = false) }
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(signInError = "Inserisci email e password") }
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, signInError = null) }

                // 1. Creiamo l'utente
                auth.createUserWithEmailAndPassword(email, pass).await()

                // 2. Impostiamo nome default
                val user = auth.currentUser
                val nomeProvvisorio = email.substringBefore("@")

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(nomeProvvisorio)
                    .build()

                user?.updateProfile(profileUpdates)?.await()

                // 3. Completiamo
                val userData = user?.run {
                    UserData(uid, nomeProvvisorio, photoUrl?.toString(), email)
                }
                onSignInResult(SignInResult(userData, null))
            } catch (e: Exception) {
                // GESTIONE ERRORI SPECIFICI
                val errorMsg = when (e) {
                    is FirebaseAuthUserCollisionException -> "Email già registrata! Prova ad accedere."
                    is FirebaseAuthWeakPasswordException -> "La password è troppo debole (min 6 caratteri)."
                    is FirebaseAuthInvalidCredentialsException -> "Formato email non valido."
                    else -> "Errore Registrazione: ${e.localizedMessage}"
                }
                _state.update { it.copy(signInError = errorMsg, isLoading = false) }
            }
        }
    }
}

data class LoginState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null,
    val isLoading: Boolean = false
)