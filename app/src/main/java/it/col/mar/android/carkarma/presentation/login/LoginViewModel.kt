package it.col.mar.android.carkarma.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import it.col.mar.android.carkarma.util.SignInResult
import it.col.mar.android.carkarma.util.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    // Gestisce il risultato del login con Google (che arriva dall'esterno)
    fun onSignInResult(result: SignInResult) {
        _state.update { it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        ) }
    }

    fun resetState() {
        _state.update { LoginState() }
    }

    // --- EMAIL & PASSWORD ---

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _state.update { it.copy(signInError = "Inserisci email e password") }
            return
        }

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, pass).await()
                val user = auth.currentUser
                val userData = user?.run {
                    UserData(uid, displayName, photoUrl?.toString())
                }
                onSignInResult(SignInResult(userData, null))
            } catch (e: Exception) {
                _state.update { it.copy(signInError = "Errore Login: ${e.message}") }
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
                // 1. Creiamo l'utente
                auth.createUserWithEmailAndPassword(email, pass).await()

                // 2. (Opzionale) Impostiamo un nome di default basato sulla mail
                // perché la registrazione email base non chiede il nome
                val user = auth.currentUser
                val nomeProvvisorio = email.substringBefore("@")

                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(nomeProvvisorio)
                    .build()

                user?.updateProfile(profileUpdates)?.await()

                // 3. Completiamo il login
                val userData = user?.run {
                    UserData(uid, nomeProvvisorio, photoUrl?.toString())
                }
                onSignInResult(SignInResult(userData, null))
            } catch (e: Exception) {
                _state.update { it.copy(signInError = "Errore Registrazione: ${e.message}") }
            }
        }
    }
}

data class LoginState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)