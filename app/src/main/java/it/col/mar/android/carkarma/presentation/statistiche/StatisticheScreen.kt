package it.col.mar.android.carkarma.presentation.statistiche

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.util.Locale

@Composable
fun StatisticheScreen(
    navController: NavController,
    gruppoId: String,
    viewModel: StatisticheViewModel
) {
    LaunchedEffect(gruppoId) {
        viewModel.loadStatistiche(gruppoId)
    }

    val nomeGruppo by viewModel.nomeGruppo.collectAsState()
    val kmTotali by viewModel.kmTotaliGruppo.collectAsState()
    val nUscite by viewModel.numeroUscite.collectAsState()
    val media by viewModel.mediaKm.collectAsState()
    val classifica by viewModel.classifica.collectAsState()
    val isRecalculating by viewModel.isRecalculating.collectAsState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // --- HEADER COMPATTO ---
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
                text = "Statistiche $nomeGruppo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            // --- PULSANTE DI RICALCOLO AUTOMATICO STORICO ---
            if (isRecalculating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(
                    onClick = {
                        viewModel.ricalcolaTuttoLoStorico(gruppoId) {
                            Toast.makeText(context, "Storico ricalcolato con successo! 🎉", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Ricalcola tutto",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- HEADER BOLLE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBubble(
                label = "Km Totali",
                value = "$kmTotali",
                icon = Icons.Default.DirectionsCar,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            StatBubble(
                label = "Uscite",
                value = "$nUscite",
                icon = Icons.Default.Timeline,
                color = MaterialTheme.colorScheme.secondaryContainer
            )
            StatBubble(
                label = "Media/Viaggio",
                value = "$media",
                icon = Icons.Default.Equalizer,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        // Intestazione Lista
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Classifica Karma",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- LISTA CLASSIFICA ---
        if (classifica.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessun dato sufficiente per la classifica.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(classifica) { stat ->
                    ClassificaItem(
                        stat = stat,
                        onClickItem = {
                            navController.navigate("dettaglio_karma/$gruppoId/${stat.amico.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatBubble(label: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun ClassificaItem(
    stat: StatisticaMembro,
    onClickItem: () -> Unit
) {
    Card(
        onClick = onClickItem, // Abilita il click nativo M3 sulla riga del membro
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stat.amico.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Su ${stat.usciteFatte} uscite",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    color = if (stat.isSanto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Karma ${String.format(Locale.US, "%.1f", stat.karma)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (stat.isSanto) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "${stat.kmGuidati} km tot",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stat.isSanto) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { stat.percentuale },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (stat.isSanto) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
        }
    }
}