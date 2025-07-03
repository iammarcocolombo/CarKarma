package it.col.mar.android.carkarma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.presentation.calcolo.CalcoloScreen
import it.col.mar.android.carkarma.presentation.gruppo.DettaglioGruppoScreen
import it.col.mar.android.carkarma.presentation.gruppo.GruppiScreen
import it.col.mar.android.carkarma.presentation.home.HomeScreen
import it.col.mar.android.carkarma.presentation.uscita.NuovaUscitaScreen
import it.col.mar.android.carkarma.ui.theme.CarKarmaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarKarmaTheme {
                // Creo il NavController
                val navController = rememberNavController()

                // Scaffold se vuoi lasciare margini/padding comuni (opzionale)
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // NavHost: contiene tutte le schermate dell’app
                    NavHost(
                        navController = navController,
                        startDestination = "home",  // schermata di partenza
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // composable: definisce una schermata navigabile
                        composable("home") { HomeScreen(navController) }
                        composable("gruppi") { GruppiScreen(navController) }
                        composable("dettaglioGruppo") { DettaglioGruppoScreen(navController) }
                        composable("nuovaUscita") { NuovaUscitaScreen(navController) }
                        composable("calcolo") { CalcoloScreen(navController) }
                    }
                }
            }
        }
    }
}
