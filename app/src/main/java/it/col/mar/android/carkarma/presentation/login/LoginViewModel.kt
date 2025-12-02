package it.col.mar.android.carkarma.presentation.login

import androidx.lifecycle.ViewModel
import it.col.mar.android.carkarma.util.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    fun onSignInResult(result: SignInResult) {
        _state.update { it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        ) }
    }

    fun resetState() {
        _state.update { LoginState() }
    }
}

data class LoginState(
    val isSignInSuccessful: Boolean = false,
    val signInError: String? = null
)