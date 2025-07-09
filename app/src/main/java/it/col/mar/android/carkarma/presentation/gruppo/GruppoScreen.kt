package it.col.mar.android.carkarma.presentation.gruppo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import it.col.mar.android.carkarma.data.database.GruppoRepository
import it.col.mar.android.carkarma.data.database.UscitaRepository
import it.col.mar.android.carkarma.data.model.Uscita

@Composable
fun GruppoScreen(navController: NavController, gruppoId: Int) {
    val gruppoRepository = remember { GruppoRepository() }
    val uscitaRepository = remember { UscitaRepository() }

    val gruppo = remember { gruppoRepository.getGruppoPerId(gruppoId) }
    val uscite = remember { uscitaRepository.getUscitePerGruppo(gruppoId) }

    if (gruppo == null) {
        Text("Gruppo non trovato", modifier = Modifier.padding(16.dp))
        return
    }
    if (gruppoId == -1) {
        Text("ID gruppo non valido", modifier = Modifier.padding(16.dp))
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Titolo gruppo e numero componenti
        Text(
            text = gruppo.nome,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "${gruppo.amici.size} componenti",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Lista uscite
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uscite) { uscita ->
                UscitaCard(uscita = uscita, onClick = {
                    // Azione sul click di una uscita (se serve)
                })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottone nuova uscita
        Button(
            onClick = { navController.navigate("nuovaUscita") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text("Nuova Uscita")
        }
    }
}

@Composable
fun UscitaCard(uscita: Uscita, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = uscita.nome,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Km totali: ${uscita.kmTotali}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GruppoScreenPreview() {
    val navController = rememberNavController()

    // Mock di repository di test
    val gruppoRepository = GruppoRepository()
    val uscitaRepository = UscitaRepository()

    // Aggiungiamo dati di test per essere sicuri che il gruppo con id 1 esista
    val gruppoId = 1

    // Serve perché nelle preview i remember non mantengono stato tra recomposition
    LaunchedEffect(Unit) {
        if (gruppoRepository.getGruppoPerId(gruppoId) == null) {
            gruppoRepository.aggiungiGruppo(
                it.col.mar.android.carkarma.data.model.Gruppo(
                    id = gruppoId,
                    nome = "Amici",
                    amici = listOf()
                )
            )
        }
    }

    GruppoScreen(navController = navController, gruppoId = gruppoId)
}
