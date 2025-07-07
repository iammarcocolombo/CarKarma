package it.col.mar.android.carkarma.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.presentation.AppScaffold

@Composable
fun HomeScreen(navController: NavController) {
    AppScaffold(navController, title = "Home") { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text("Benvenuto nella Home!", fontSize = 20.sp)

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = { navController.navigate("gruppi") }) {
                Text("Vai ai Gruppi")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()
    HomeScreen(navController)
}

