package it.col.mar.android.carkarma.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.presentation.navigation.CarKarmaNavHost

@Composable
fun CarKarmaApp() {
    val navController = rememberNavController()

    AppScaffold(navController = navController) { paddingValues ->
        CarKarmaNavHost(navController, paddingValues)
    }
}
