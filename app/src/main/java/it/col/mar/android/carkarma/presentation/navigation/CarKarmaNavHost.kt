package it.col.mar.android.carkarma.presentation.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import it.col.mar.android.carkarma.data.database.AppContainer
import it.col.mar.android.carkarma.presentation.amico.AmicoScreen
import it.col.mar.android.carkarma.presentation.amico.AmicoViewModel
import it.col.mar.android.carkarma.presentation.amico.AmicoViewModelFactory
import it.col.mar.android.carkarma.presentation.gruppo.GruppoScreen
import it.col.mar.android.carkarma.presentation.gruppo.GruppoViewModel
import it.col.mar.android.carkarma.presentation.gruppo.GruppoViewModelFactory
import it.col.mar.android.carkarma.presentation.gruppo.modifica.ModificaGruppoScreen
import it.col.mar.android.carkarma.presentation.gruppo.modifica.ModificaGruppoViewModel
import it.col.mar.android.carkarma.presentation.gruppo.modifica.ModificaGruppoViewModelFactory
import it.col.mar.android.carkarma.presentation.home.HomeScreen
import it.col.mar.android.carkarma.presentation.login.LoginScreen
import it.col.mar.android.carkarma.presentation.login.LoginViewModel
import it.col.mar.android.carkarma.presentation.login.LoginViewModelFactory
import it.col.mar.android.carkarma.presentation.uscita.UscitaScreen
import it.col.mar.android.carkarma.presentation.uscita.UscitaViewModel
import it.col.mar.android.carkarma.presentation.uscita.UscitaViewModelFactory
import it.col.mar.android.carkarma.util.GoogleAuthClient
import kotlinx.coroutines.launch

@Composable
fun CarKarmaNavHost(navController: NavHostController, paddingValues: PaddingValues) {

    val context = LocalContext.current
    // Inizializziamo il client di Google Auth
    val googleAuthClient = GoogleAuthClient(context)

    // Determiniamo dove iniziare: se l'utente è già loggato -> Home, altrimenti -> Login
    val startDestination = if (googleAuthClient.getSignedInUser() != null) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(paddingValues)
    ) {

        // --- LOGIN SCREEN ---
        composable("login") {
            val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory())
            val state by viewModel.state.collectAsState()
            val scope = rememberCoroutineScope()

            // Launcher per il risultato del login di Google
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        scope.launch {
                            val signInResult = googleAuthClient.signInWithIntent(
                                intent = result.data ?: return@launch
                            )
                            viewModel.onSignInResult(signInResult)
                        }
                    }
                }
            )

            // Effetto collaterale: se il login ha successo, vai alla Home
            LaunchedEffect(key1 = state.isSignInSuccessful) {
                if (state.isSignInSuccessful) {
                    Toast.makeText(context, "Accesso effettuato!", Toast.LENGTH_LONG).show()
                    navController.navigate("home") {
                        // Rimuovi la schermata di login dal backstack così premendo indietro si esce dall'app
                        popUpTo("login") { inclusive = true }
                    }
                    viewModel.resetState()
                }
            }

            LoginScreen(
                state = state,
                onSignInClick = {
                    scope.launch {
                        val signInIntentSender = googleAuthClient.signIn()
                        if (signInIntentSender != null) {
                            launcher.launch(
                                IntentSenderRequest.Builder(signInIntentSender).build()
                            )
                        } else {
                            Toast.makeText(context, "Errore avvio login Google", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // --- HOME SCREEN ---
        composable("home") { HomeScreen(navController) }

        // --- ALTRE SCHERMATE (Invariate) ---
        composable(
            route = "gruppo/{gruppoId}",
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""
            val viewModel: GruppoViewModel = viewModel(
                factory = GruppoViewModelFactory(
                    AppContainer.amicoRepository,
                    AppContainer.gruppoRepository,
                    AppContainer.uscitaRepository
                )
            )
            GruppoScreen(navController, gruppoId, viewModel)
        }

        composable(
            route = "modificaGruppo?gruppoId={gruppoId}",
            arguments = listOf(navArgument("gruppoId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""
            val viewModel: ModificaGruppoViewModel = viewModel(
                factory = ModificaGruppoViewModelFactory(
                    AppContainer.gruppoRepository,
                    AppContainer.amicoRepository
                )
            )
            ModificaGruppoScreen(navController, gruppoId, viewModel)
        }

        composable(
            route = "uscita/{gruppoId}?uscitaId={uscitaId}",
            arguments = listOf(
                navArgument("gruppoId") { type = NavType.StringType },
                navArgument("uscitaId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""
            val uscitaId = backStackEntry.arguments?.getString("uscitaId") ?: ""
            val viewModel: UscitaViewModel = viewModel(
                factory = UscitaViewModelFactory(
                    AppContainer.uscitaRepository,
                    AppContainer.gruppoRepository
                )
            )
            UscitaScreen(navController, gruppoId, uscitaId, viewModel)
        }

        composable(
            route = "amico?amicoId={amicoId}",
            arguments = listOf(navArgument("amicoId") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val amicoId = backStackEntry.arguments?.getString("amicoId") ?: ""
            val viewModel: AmicoViewModel = viewModel(
                factory = AmicoViewModelFactory(AppContainer.amicoRepository)
            )
            AmicoScreen(navController, amicoId, viewModel)
        }
    }
}