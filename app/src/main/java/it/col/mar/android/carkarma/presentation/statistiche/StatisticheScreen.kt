package it.col.mar.android.carkarma.presentation.statistiche

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Equalizer
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
import androidx.compose.ui.platform.LocalConfiguration
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

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // --- HEADER COMPATTO CON TASTO INDIETRO ---
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
        }

        if (isLandscape) {
            // --- LAYOUT TABLET ORIZZONTALE (2 COLONNE AFFIANCATE) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // COLONNA SINISTRA: Metriche e Bolle Statistiche
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                ) {
                    SezioneRiepilogoStatistiche(
                        kmTotali = kmTotali,
                        nUscite = nUscite,
                        media = media
                    )
                }

                // COLONNA DESTRA: Classifica Karma
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    SezioneClassificaKarma(
                        classifica = classifica,
                        gruppoId = gruppoId,
                        navController = navController,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // --- LAYOUT VERTICALE CLASSICO (1 COLONNA SINGOLA) ---
            SezioneBolleStatistiche(
                kmTotali = kmTotali,
                nUscite = nUscite,
                media = media
            )

            Spacer(modifier = Modifier.height(24.dp))

            SezioneClassificaKarma(
                classifica = classifica,
                gruppoId = gruppoId,
                navController = navController,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ==========================================
// COMPONENTI MODULARI PER IL LAYOUT ADATTIVO
// ==========================================

@Composable
private fun SezioneRiepilogoStatistiche(
    kmTotali: Int,
    nUscite: Int,
    media: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Panoramica Gruppo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SezioneBolleStatistiche(
                kmTotali = kmTotali,
                nUscite = nUscite,
                media = media
            )
        }
    }
}

@Composable
private fun SezioneBolleStatistiche(
    kmTotali: Int,
    nUscite: Int,
    media: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
}

@Composable
private fun SezioneClassificaKarma(
    classifica: List<StatisticaMembro>,
    gruppoId: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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

        if (classifica.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nessun dato sufficiente per la classifica.",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
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
        onClick = onClickItem,
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