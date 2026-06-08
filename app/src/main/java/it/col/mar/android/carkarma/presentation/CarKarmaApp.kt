package it.col.mar.android.carkarma.presentation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.data.database.AppContainer
import it.col.mar.android.carkarma.domain.repository.AuthRepository
import it.col.mar.android.carkarma.presentation.navigation.CarKarmaNavHost
import kotlinx.coroutines.launch

@Composable
fun CarKarmaApp() {
    val context = LocalContext.current

    AppContainer.initialize(context)
    val authRepository: AuthRepository = AppContainer.authRepository

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf(authRepository.getSignedInUser()) }

    AppScaffold(
        navController = navController,
        userData = currentUser,
        onSignOut = {
            scope.launch {
                authRepository.signOut()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        },
        onDeleteAccount = {
            scope.launch {
                AppContainer.gruppoRepository.rimuoviUtenteDaTuttiIGruppi()
                AppContainer.gruppoRepository.eliminaDatiUtentePubblico()

                val successo = authRepository.deleteAccount()

                if (successo) {
                    Toast.makeText(context, "Account eliminato correttamente.", Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Per sicurezza, esegui il logout e rientra prima di eliminare l'account.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    ) {
        CarKarmaNavHost(
            navController = navController,
            authRepository = authRepository,
            onLoginSuccess = { userData ->
                currentUser = userData
            }
        )
    }
}