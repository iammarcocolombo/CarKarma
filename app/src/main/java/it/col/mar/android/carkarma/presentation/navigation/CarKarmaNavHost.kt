package it.col.mar.android.carkarma.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import it.col.mar.android.carkarma.presentation.calcolo.CalcoloScreen
import it.col.mar.android.carkarma.presentation.gruppo.ModificaGruppoScreen
import it.col.mar.android.carkarma.presentation.gruppo.GruppoScreen
import it.col.mar.android.carkarma.presentation.home.HomeScreen
import it.col.mar.android.carkarma.presentation.uscita.NuovaUscitaScreen


@Composable
fun CarKarmaNavHost(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier.padding(paddingValues)
    ) {
        composable("home") { HomeScreen(navController) }

        composable(
            "gruppo/{gruppoId}"
        ) { backStackEntry ->
            val gruppoId = backStackEntry.arguments?.getString("gruppoId")?.toIntOrNull() ?: -1
            GruppoScreen(navController, gruppoId)
        }


        composable("modificaGruppo") { ModificaGruppoScreen(navController) }
        composable("nuovaUscita") { NuovaUscitaScreen(navController) }
        composable("calcolo") { CalcoloScreen(navController) }
    }
}
