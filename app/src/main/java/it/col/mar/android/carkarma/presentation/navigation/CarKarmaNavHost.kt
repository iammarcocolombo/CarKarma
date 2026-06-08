package it.col.mar.android.carkarma.presentation.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import it.col.mar.android.carkarma.data.database.AppContainer
import it.col.mar.android.carkarma.data.database.AppContainer.gruppoRepository
import it.col.mar.android.carkarma.data.model.UserData
import it.col.mar.android.carkarma.domain.repository.AuthRepository
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
import it.col.mar.android.carkarma.presentation.statistiche.StatisticheScreen
import it.col.mar.android.carkarma.presentation.statistiche.StatisticheViewModel
import it.col.mar.android.carkarma.presentation.statistiche.StatisticheViewModelFactory
import it.col.mar.android.carkarma.presentation.statistiche.dettaglio.DettaglioKarmaScreen
import it.col.mar.android.carkarma.presentation.statistiche.dettaglio.DettaglioKarmaViewModel
import it.col.mar.android.carkarma.presentation.statistiche.dettaglio.DettaglioKarmaViewModelFactory
import it.col.mar.android.carkarma.presentation.uscita.UscitaScreen
import it.col.mar.android.carkarma.presentation.uscita.UscitaViewModel
import it.col.mar.android.carkarma.presentation.uscita.UscitaViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun CarKarmaNavHost(
    navController: NavHostController,
    authRepository: AuthRepository,
    onLoginSuccess: (UserData) -> Unit
) {
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch {
                val signInResult = authRepository.signInWithIntent(result.data ?: return@launch)
                if (signInResult.data != null) {
                    onLoginSuccess(signInResult.data)
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    val destinationAfterSplash =
        if (authRepository.getSignedInUser() != null) Screen.Home.route else "login"

    NavHost(
        navController = navController,
        startDestination = destinationAfterSplash,
        modifier = Modifier
    ) {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory())
            val state by loginViewModel.state.collectAsState()

            LoginScreen(
                state = state,
                onSignInClick = {
                    scope.launch {
                        val intentSender = authRepository.signIn()
                        if (intentSender != null) {
                            launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                    }
                },
                viewModel = loginViewModel
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable("privacy") {
            PrivacyScreen(navController)
        }

        composable("credits") {
            CreditsScreen(navController)
        }

        composable(
            route = "statistiche/{gruppoId}",
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""
            val vm: StatisticheViewModel = viewModel(
                factory = StatisticheViewModelFactory(
                    AppContainer.gruppoRepository,
                    AppContainer.uscitaRepository,
                    AppContainer.carburanteRepository
                )
            )
            StatisticheScreen(navController, gruppoId, vm)
        }

        composable(
            route = Screen.Gruppo.route,
            arguments = listOf(navArgument("gruppoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""
            val vm: GruppoViewModel = viewModel(
                factory = GruppoViewModelFactory(
                    AppContainer.gruppoRepository,
                    AppContainer.uscitaRepository
                )
            )
            GruppoScreen(navController, gruppoId, vm)
        }

        composable(
            route = Screen.ModificaGruppo.route,
            arguments = listOf(
                navArgument("gruppoId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""
            val vm: ModificaGruppoViewModel = viewModel(
                factory = ModificaGruppoViewModelFactory(
                    AppContainer.gruppoRepository,
                    AppContainer.amicoRepository
                )
            )
            ModificaGruppoScreen(navController, gruppoId, vm)
        }

        composable(
            route = "amico?amicoId={amicoId}&gruppoId={gruppoId}",
            arguments = listOf(
                navArgument("amicoId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("gruppoId") {
                    type = NavType.StringType
                    defaultValue = ""
                }
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
            AmicoScreen(navController, amicoId, vm, gruppoId)
        }

        composable(
            route = Screen.Uscita.route,
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
            val vm: UscitaViewModel = viewModel(
                factory = UscitaViewModelFactory(
                    AppContainer.uscitaRepository,
                    AppContainer.gruppoRepository,
                    AppContainer.carburanteRepository
                )
            )
            UscitaScreen(navController, gruppoId, uscitaId, vm)
        }

        composable("dettaglio_karma/{gruppoId}/{componenteId}") { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""
            val componenteId = backStackEntry.arguments?.getString("componenteId") ?: ""

            val factory = DettaglioKarmaViewModelFactory(gruppoRepository)
            val dettaglioViewModel: DettaglioKarmaViewModel = viewModel(factory = factory)

            DettaglioKarmaScreen(
                navController = navController,
                gruppoId = gruppoId,
                componenteId = componenteId,
                viewModel = dettaglioViewModel
            )
        }
    }
}