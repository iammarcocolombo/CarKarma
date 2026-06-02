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

    // Inizializziamo l'AppContainer passando il contesto dell'applicazione Android.
    AppContainer.initialize(context)
    val authRepository: AuthRepository = AppContainer.authRepository

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    // Stato dell'utente corrente ricavato in modo sicuro tramite l'interfaccia di dominio
    var currentUser by remember { mutableStateOf(authRepository.getSignedInUser()) }

    // Scaffold Principale: gestisce la TopBar e il menu laterale (Drawer)
    AppScaffold(
        navController = navController,
        userData = currentUser,

        // Callback chiamata quando l'utente preme "Esci" dal Drawer menu
        onSignOut = {
            scope.launch {
                authRepository.signOut()
                // Resetta lo stato utente locale
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true } // Pulisce completamente lo stack di navigazione
                }
            }
        },

        onDeleteAccount = {
            scope.launch {
                // 1. Rimuoviamo in sicurezza l'ID utente da tutti i gruppi attivi
                AppContainer.gruppoRepository.rimuoviUtenteDaTuttiIGruppi()

                // 2. Cancelliamo l'account pubblico usato per la ricerca tramite email
                AppContainer.gruppoRepository.eliminaDatiUtentePubblico()

                // 3. Eliminiamo l'autenticazione su Firebase Auth e Google OneTap
                val successo = authRepository.deleteAccount()

                if (successo) {
                    Toast.makeText(context, "Account eliminato correttamente.", Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    // Sicurezza Google: se la sessione è troppo vecchia, Google ne impedisce l'eliminazione diretta.
                    Toast.makeText(context, "Per sicurezza, esegui il logout e rientra prima di eliminare l'account.", Toast.LENGTH_LONG).show()
                }
            }
        }
    ) { paddingValues ->

        // NavHost: si occupa di gestire il cambio delle schermate interne dell'app
        CarKarmaNavHost(
            navController = navController,
            paddingValues = paddingValues,
            authRepository = authRepository, // Passiamo il repository al posto del vecchio client
            onLoginSuccess = { _ ->
                // Aggiorna lo stato per ridisegnare il Drawer laterale con le nuove info
            }
        )
    }
}