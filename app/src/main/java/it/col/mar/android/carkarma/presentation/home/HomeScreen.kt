package it.col.mar.android.carkarma.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.data.model.Amico
import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.ui.theme.CarKarmaTheme
import androidx.lifecycle.viewmodel.compose.viewModel


@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: HomeViewModel = viewModel()
) {
    val gruppi by viewModel.gruppi.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "I tuoi Gruppi",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn {
            items(gruppi) { gruppo ->
                GruppoCard(gruppo = gruppo, onClick = { navController.navigate("gruppo/${gruppo.id}")})
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    val navController = rememberNavController()

    // Gruppi di test solo per preview
    val gruppiDiTest = listOf(
        Gruppo(1, "Amici", emptyList()),
        Gruppo(2, "Famiglia", emptyList()),
        Gruppo(3, "Colleghi", emptyList())
    )

    CarKarmaTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "I tuoi Gruppi",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn {
                items(gruppiDiTest) { gruppo ->
                    GruppoCard(gruppo = gruppo, onClick = {})
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
