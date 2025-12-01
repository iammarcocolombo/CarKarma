package it.col.mar.android.carkarma.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import it.col.mar.android.carkarma.presentation.uscita.UscitaScreen
import it.col.mar.android.carkarma.presentation.uscita.UscitaViewModel
import it.col.mar.android.carkarma.presentation.uscita.UscitaViewModelFactory

@Composable
fun CarKarmaNavHost(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.padding(paddingValues)
    ) {
        // HOME
        composable("home") { HomeScreen(navController) }

        // DETTAGLIO GRUPPO (ID obbligatorio)
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

            GruppoScreen(
                navController = navController,
                gruppoId = gruppoId,
                viewModel = viewModel
            )
        }

        // MODIFICA GRUPPO (ID opzionale: se vuoto è nuovo)
        composable(
            route = "modificaGruppo?gruppoId={gruppoId}",
            arguments = listOf(navArgument("gruppoId") {
                type = NavType.StringType
                defaultValue = "" // Stringa vuota = Nuovo Gruppo
            })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId") ?: ""

            val viewModel: ModificaGruppoViewModel = viewModel(
                factory = ModificaGruppoViewModelFactory(
                    AppContainer.gruppoRepository,
                    AppContainer.amicoRepository
                )
            )

            ModificaGruppoScreen(
                navController = navController,
                gruppoId = gruppoId,
                viewModel = viewModel
            )
        }

        // USCITA (UscitaID opzionale)
        composable(
            route = "uscita/{gruppoId}?uscitaId={uscitaId}",
            arguments = listOf(
                navArgument("gruppoId") { type = NavType.StringType },
                navArgument("uscitaId") {
                    type = NavType.StringType
                    defaultValue = "" // Stringa vuota = Nuova Uscita
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

            UscitaScreen(
                navController = navController,
                gruppoId = gruppoId,
                uscitaId = uscitaId,
                viewModel = viewModel
            )
        }

        // DETTAGLIO AMICO (ID opzionale)
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