package it.col.mar.android.carkarma.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory per istanziare il [LoginViewModel].
 * Poiché il ViewModel non richiede parametri di iniezione diretta nel costruttore
 * (le operazioni complesse di OneTap Google vengono gestite ed estrapolate a livello di NavHost),
 * la Factory istanzia semplicemente la classe base.
 */
class LoginViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}