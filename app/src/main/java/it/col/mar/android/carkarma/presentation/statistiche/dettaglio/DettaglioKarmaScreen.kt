package it.col.mar.android.carkarma.presentation.statistiche.dettaglio

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.Locale
import kotlin.math.abs

@Composable
fun DettaglioKarmaScreen(
    navController: NavController,
    gruppoId: String,
    componenteId: String,
    viewModel: DettaglioKarmaViewModel
) {
    LaunchedEffect(gruppoId, componenteId) {
        viewModel.loadDettagliComponente(gruppoId, componenteId)
    }

    val nomeComponente by viewModel.nomeComponente.collectAsState()
    val listaBilanci by viewModel.listaBilanci.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Indietro",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Dettaglio Karma $nomeComponente",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (listaBilanci.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun altro componente nel gruppo.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(listaBilanci) { bilancio ->
                    RowBilancioIncrociato(bilancio)
                }
            }
        }
    }
}

@Composable
fun RowBilancioIncrociato(bilancio: BilancioIncrociato) {
    val (coloreStato, testoStato, iconaStato) = when {
        bilancio.saldo > 0.01 -> Triple(
            MaterialTheme.colorScheme.primary,
            "Sei in credito di ${String.format(Locale.US, "%.2f", bilancio.saldo)}€",
            Icons.AutoMirrored.Filled.TrendingUp
        )
        bilancio.saldo < -0.01 -> Triple(
            MaterialTheme.colorScheme.error,
            "Sei in debito di ${String.format(Locale.US, "%.2f", abs(bilancio.saldo))}€",
            Icons.AutoMirrored.Filled.TrendingDown
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            "Al pari",
            Icons.Default.HorizontalRule
        )
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = bilancio.nomeAltroMembro,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = testoStato,
                    style = MaterialTheme.typography.bodyMedium,
                    color = coloreStato,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = iconaStato,
                contentDescription = null,
                tint = coloreStato,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}