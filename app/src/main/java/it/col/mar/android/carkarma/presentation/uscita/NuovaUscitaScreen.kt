package it.col.mar.android.carkarma.presentation.uscita

import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.presentation.home.HomeScreen

@Composable
fun NuovaUscitaScreen(navController: NavController) {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Crea Nuova Uscita")

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = { navController.navigate("calcolo") }) {
            Text("Calcola Chi Guida")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { navController.navigate("dettaglioGruppo") }) {
            Text("Torna al Dettaglio Gruppo")
        }
    }
}
