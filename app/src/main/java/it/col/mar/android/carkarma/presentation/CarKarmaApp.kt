package it.col.mar.android.carkarma.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.presentation.navigation.CarKarmaNavHost
import it.col.mar.android.carkarma.util.GoogleAuthClient
import it.col.mar.android.carkarma.util.UserData
import kotlinx.coroutines.launch

@Composable
fun CarKarmaApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Gestione Autenticazione
    val googleAuthClient = remember { GoogleAuthClient(context) }

    // Stato dell'utente corrente (inizialmente proviamo a prenderlo se già loggato)
    var currentUser by remember { mutableStateOf(googleAuthClient.getSignedInUser()) }

    // Scaffold Globale che contiene Drawer e TopBar
    AppScaffold(
        navController = navController,
        userData = currentUser, // Passiamo i dati utente al Drawer
        onSignOut = {
            scope.launch {
                googleAuthClient.signOut()
                currentUser = null // Resettiamo lo stato
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true } // Pulisce tutto lo stack
                }
            }
        }
    ) { paddingValues ->
        // NavHost gestisce il contenuto centrale
        CarKarmaNavHost(
            navController = navController,
            paddingValues = paddingValues,
            googleAuthClient = googleAuthClient,
            onLoginSuccess = { user ->
                currentUser = user // Aggiorniamo lo stato globale dopo il login!
            }
        )
    }
}