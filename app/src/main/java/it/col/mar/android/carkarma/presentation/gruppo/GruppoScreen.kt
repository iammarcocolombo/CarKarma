package it.col.mar.android.carkarma.presentation.gruppo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun GruppoScreen(
    navController: NavController,
    gruppoId: Int,
    viewModel: GruppoViewModel
) {
    LaunchedEffect(gruppoId, navController.currentBackStackEntry) {
        viewModel.loadGruppo(gruppoId)
    }

    val gruppo by viewModel.gruppo.collectAsState()
    val uscite by viewModel.uscite.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    if (gruppo == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = gruppo!!.nome,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "${gruppo!!.amici.size} componenti",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(uscite) { uscita ->
                UscitaCard(
                    uscita = uscita,
                    onClick = {
                        navController.navigate("uscita/${gruppoId}/${uscita.id}")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Button(
            onClick = { navController.navigate("uscita/${gruppoId}/-1") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                "Nuova Uscita",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { navController.navigate("modificaGruppo/${gruppo!!.id}") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(
                "Modifica Gruppo",
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}



@Composable
fun UscitaCard(uscita: it.col.mar.android.carkarma.data.model.Uscita, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = uscita.nome,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Km totali: ${uscita.kmTotali}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
