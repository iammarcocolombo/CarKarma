package it.col.mar.android.carkarma.presentation.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
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
import it.col.mar.android.carkarma.presentation.info.CreditsScreen
import it.col.mar.android.carkarma.presentation.login.LoginScreen
import it.col.mar.android.carkarma.presentation.login.LoginViewModel
import it.col.mar.android.carkarma.presentation.login.LoginViewModelFactory
import it.col.mar.android.carkarma.presentation.privacy.PrivacyScreen
import it.col.mar.android.carkarma.presentation.uscita.UscitaScreen
import it.col.mar.android.carkarma.presentation.uscita.UscitaViewModel
import it.col.mar.android.carkarma.presentation.uscita.UscitaViewModelFactory
import it.col.mar.android.carkarma.util.GoogleAuthClient
import it.col.mar.android.carkarma.util.UserData
import kotlinx.coroutines.launch

@Composable
fun CarKarmaNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
    googleAuthClient: GoogleAuthClient,
    onLoginSuccess: (UserData) -> Unit
) {
    val context = LocalContext.current
    // Determina la destinazione iniziale
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

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult(),
                onResult = { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        scope.launch {
                            val signInResult = googleAuthClient.signInWithIntent(intent = result.data ?: return@launch)
                            viewModel.onSignInResult(signInResult)
                        }
                    } else {
                        Toast.makeText(context, "Login annullato o fallito. Riprova.", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            LaunchedEffect(key1 = state.isSignInSuccessful) {
                if (state.isSignInSuccessful) {
                    googleAuthClient.getSignedInUser()?.let { user ->
                        onLoginSuccess(user)
                        // Salviamo l'utente nel DB pubblico per permettere gli inviti
                        AppContainer.gruppoRepository.rendiUtenteCercabile(user.userId, user.email, user.username)
                    }

                    Toast.makeText(context, "Accesso effettuato!", Toast.LENGTH_LONG).show()

                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                    viewModel.resetState()
                }
            }

            LoginScreen(
                state = state,
                onSignInClick = {
                    scope.launch {
                        val intentSender = googleAuthClient.signIn()
                        if (intentSender != null) launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                        else Toast.makeText(context, "Errore configurazione Google (SHA-1?)", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // --- HOME SCREEN ---
        composable("home") {
            HomeScreen(navController = navController)
        }

        // --- PRIVACY POLICY ---
        composable("privacy") {
            PrivacyScreen(navController)
        }

        // --- CREDITI E LICENZE ---
        composable("credits") {
            CreditsScreen(navController)
        }

        // --- JOIN GRUPPO VIA LINK (carkarma://) ---
        composable(
            route = "join/{groupId}",
            deepLinks = listOf(navDeepLink { uriPattern = "carkarma://join/{groupId}" }),
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val user = googleAuthClient.getSignedInUser()

            if (user == null) {
                // Se non è loggato, mandalo al login
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Accedi per unirti al gruppo", Toast.LENGTH_SHORT).show()
                    navController.navigate("login")
                }
            } else {
                // Proviamo ad unirci al gruppo
                var loading by remember { mutableStateOf(true) }
                val scope = rememberCoroutineScope()

                LaunchedEffect(groupId) {
                    AppContainer.gruppoRepository.uniscitiAlGruppo(groupId) { successo ->
                        if (successo) {
                            // MAGIA: Importiamo gli amici del gruppo nella rubrica personale!
                            scope.launch {
                                val membriDelGruppo = AppContainer.gruppoRepository.getMembriSnapshot(groupId)
                                if (membriDelGruppo.isNotEmpty()) {
                                    AppContainer.amicoRepository.importaAmici(membriDelGruppo)
                                    Toast.makeText(context, "Gruppo e Amici importati!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Ti sei unito al gruppo!", Toast.LENGTH_SHORT).show()
                                }
                                loading = false
                                navController.navigate("gruppo/$groupId") {
                                    popUpTo("home") { inclusive = false }
                                }
                            }
                        } else {
                            loading = false
                            Toast.makeText(context, "Errore: impossibile unirsi al gruppo.", Toast.LENGTH_LONG).show()
                            navController.navigate("home")
                        }
                    }
                }

                if (loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // --- DETTAGLIO GRUPPO ---
        composable("gruppo/{gruppoId}", arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })) {
            val vm: GruppoViewModel = viewModel(factory = GruppoViewModelFactory(AppContainer.amicoRepository, AppContainer.gruppoRepository, AppContainer.uscitaRepository))
            GruppoScreen(navController, it.arguments?.getString("gruppoId") ?: "", vm)
        }

        // --- MODIFICA GRUPPO ---
        composable("modificaGruppo?gruppoId={gruppoId}", arguments = listOf(navArgument("gruppoId") { type = NavType.StringType; defaultValue = "" })) {
            val vm: ModificaGruppoViewModel = viewModel(factory = ModificaGruppoViewModelFactory(AppContainer.gruppoRepository, AppContainer.amicoRepository))
            ModificaGruppoScreen(navController, it.arguments?.getString("gruppoId") ?: "", vm)
        }

        // --- USCITA (NUOVA O MODIFICA) ---
        composable("uscita/{gruppoId}?uscitaId={uscitaId}", arguments = listOf(navArgument("gruppoId") { type = NavType.StringType }, navArgument("uscitaId") { type = NavType.StringType; defaultValue = "" })) {
            val vm: UscitaViewModel = viewModel(factory = UscitaViewModelFactory(AppContainer.uscitaRepository, AppContainer.gruppoRepository))
            UscitaScreen(navController, it.arguments?.getString("gruppoId") ?: "", it.arguments?.getString("uscitaId") ?: "", vm)
        }

        // --- DETTAGLIO AMICO (NUOVO O MODIFICA) ---
        composable(
            route = "amico?amicoId={amicoId}&gruppoId={gruppoId}", // Accetta gruppoId opzionale
            arguments = listOf(
                navArgument("amicoId") { type = NavType.StringType; defaultValue = "" },
                navArgument("gruppoId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val amicoId = backStackEntry.arguments?.getString("amicoId") ?: ""
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""

            val vm: AmicoViewModel = viewModel(
                factory = AmicoViewModelFactory(
                    AppContainer.amicoRepository,
                    AppContainer.gruppoRepository
                )
            )
            // Carica l'amico dal contesto giusto (globale o gruppo)
            vm.loadAmico(amicoId, gruppoId)

            AmicoScreen(navController, amicoId, vm, gruppoId)
        }
    }
}