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
import it.col.mar.android.carkarma.presentation.calcolo.CalcoloScreen
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
        composable("home") { HomeScreen(navController) }

        composable(
            route = "gruppo/{gruppoId}",
            arguments = listOf(navArgument("gruppoId") { type = NavType.IntType })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getInt("gruppoId") ?: -1

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

        composable(
            "modificaGruppo/{gruppoId}",
            arguments = listOf(navArgument("gruppoId") {
                type = NavType.IntType
                defaultValue = -1
            })
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getInt("gruppoId") ?: -1

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

        composable(
            "uscita/{gruppoId}/{uscitaId}",
            arguments = listOf(
                navArgument("gruppoId") { type = NavType.IntType },
                navArgument("uscitaId") { type = NavType.IntType; defaultValue = -1 } // -1 = nuova uscita
            )
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getInt("gruppoId") ?: -1
            val uscitaId = backStackEntry.arguments?.getInt("uscitaId") ?: -1

            val viewModel: UscitaViewModel = viewModel(
                factory = UscitaViewModelFactory(
                    AppContainer.uscitaRepository,
                    AppContainer.amicoRepository
                )
            )

            UscitaScreen(
                navController = navController,
                gruppoId = gruppoId,
                uscitaId = uscitaId,
                viewModel = viewModel
            )
        }

        composable("calcolo") { CalcoloScreen(navController) }
    }
}
