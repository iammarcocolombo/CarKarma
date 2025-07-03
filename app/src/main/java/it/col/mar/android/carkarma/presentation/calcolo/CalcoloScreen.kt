package it.col.mar.android.carkarma.presentation.calcolo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.presentation.home.HomeScreen

@Composable
fun CalcoloScreen(navController: NavController) {
    Column (
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Risultato Calcolo Guidatori")

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = { navController.navigate("home") }) {
            Text("Torna a Home")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalcoloScreenPreview() {
    val navController = rememberNavController()
    CalcoloScreen(navController)
}