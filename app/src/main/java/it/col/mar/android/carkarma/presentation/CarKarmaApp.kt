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
import it.col.mar.android.carkarma.presentation.navigation.CarKarmaNavHost
import it.col.mar.android.carkarma.util.GoogleAuthClient
import kotlinx.coroutines.launch

@Composable
fun CarKarmaApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Client per gestire l'autenticazione Google
    val googleAuthClient = remember { GoogleAuthClient(context) }

    // Stato dell'utente corrente: controlla se siamo già loggati all'avvio
    var currentUser by remember { mutableStateOf(googleAuthClient.getSignedInUser()) }

    // Scaffold Principale: gestisce TopBar e Drawer (Menu Laterale)
    AppScaffold(
        navController = navController,
        userData = currentUser,

        // Callback chiamata quando l'utente preme "Esci" nel menu
        onSignOut = {
            scope.launch {
                googleAuthClient.signOut()
                currentUser = null // Resetta lo stato locale
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true } // Pulisce lo stack di navigazione
                }
            }
        },

        // Callback chiamata quando l'utente preme "Elimina Account" nel menu
        onDeleteAccount = {
            scope.launch {
                // PROCEDURA DI PULIZIA COMPLETA (GDPR compliance di base)

                // 1. Rimuovi l'ID utente da tutti i gruppi condivisi
                // (Così non rimane un membro "fantasma" nelle liste degli altri)
                AppContainer.gruppoRepository.rimuoviUtenteDaTuttiIGruppi()

                // 2. Cancella il profilo pubblico (quello usato per la ricerca email)
                AppContainer.gruppoRepository.eliminaDatiUtentePubblico()

                // 3. Cancella l'account di autenticazione (Firebase Auth)
                val successo = googleAuthClient.deleteAccount()

                if (successo) {
                    currentUser = null
                    Toast.makeText(context, "Account eliminato correttamente.", Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    // Sicurezza Google: se il login è vecchio, l'eliminazione sensibile viene bloccata.
                    Toast.makeText(context, "Per sicurezza, fai Logout e rientra prima di eliminare l'account.", Toast.LENGTH_LONG).show()
                }
            }
        }
    ) { paddingValues ->

        // NavHost: gestisce il cambio delle schermate interne
        CarKarmaNavHost(
            navController = navController,
            paddingValues = paddingValues,
            googleAuthClient = googleAuthClient,
            onLoginSuccess = { user ->
                // Callback fondamentale: quando il login ha successo, aggiorniamo lo stato qui
                // così l'AppScaffold ridisegna il menu laterale con i dati dell'utente!
                currentUser = user
            }
        )
    }
}