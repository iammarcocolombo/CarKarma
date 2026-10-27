package it.col.mar.android.carkarma.presentation.gruppo

import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.data.model.Uscita
import it.col.mar.android.carkarma.util.AvatarProvider
import it.col.mar.android.carkarma.util.QrCodeGenerator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruppoScreen(
    navController: NavController,
    gruppoId: String,
    viewModel: GruppoViewModel
) {
    LaunchedEffect(gruppoId) {
        viewModel.loadGruppo(gruppoId)
    }

    val gruppo by viewModel.gruppo.collectAsState()
    val uscite by viewModel.uscite.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var showLeaveDialog by remember { mutableStateOf(false) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    if (gruppo == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // --- HEADER COMPATTO CON TASTO INDIETRO ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp)
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
                    text = "Dettaglio Gruppo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
                    // COLONNA SINISTRA: Header Gruppo, Avatar e Tasti Azione
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        SezioneHeaderGruppo(
                            gruppo = gruppo!!,
                            onStatisticheClick = { navController.navigate("statistiche/${gruppo!!.id}") },
                            onInvitaClick = { scope.launch { sheetState.show() } },
                            onModificaClick = { navController.navigate("modificaGruppo?gruppoId=${gruppo!!.id}") },
                            onLasciaClick = { showLeaveDialog = true }
                        )
                    }

                    // COLONNA DESTRA: Lista Uscite
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    ) {
                        SezioneStoricoUscite(
                            uscite = uscite,
                            gruppoId = gruppoId,
                            navController = navController,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // --- LAYOUT VERTICALE CLASSICO (1 COLONNA SINGOLA) ---
                SezioneHeaderGruppo(
                    gruppo = gruppo!!,
                    onStatisticheClick = { navController.navigate("statistiche/${gruppo!!.id}") },
                    onInvitaClick = { scope.launch { sheetState.show() } },
                    onModificaClick = { navController.navigate("modificaGruppo?gruppoId=${gruppo!!.id}") },
                    onLasciaClick = { showLeaveDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                SezioneStoricoUscite(
                    uscite = uscite,
                    gruppoId = gruppoId,
                    navController = navController,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = { navController.navigate("uscita/${gruppoId}") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nuova Uscita") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    // --- BOTTOM SHEET PER CONDIVISIONE INVITO ---
    if (sheetState.isVisible && gruppo != null) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }
            },
            sheetState = sheetState
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = "Invita Amici",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fai scansionare il QR o condividi il codice",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                val qrBitmap = remember(gruppo!!.id) { QrCodeGenerator.generateQrCode(gruppo!!.id) }
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(200.dp)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(gruppo!!.id))
                        Toast.makeText(context, "Codice copiato!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = gruppo!!.id,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tocca per copiare",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Unisciti al mio gruppo su CarKarma! Usa questo codice:\n\n${gruppo!!.id}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Invia codice..."))
                        scope.launch {
                            sheetState.hide()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invia Codice")
                }
            }
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = {
                showLeaveDialog = false
            },
            title = { Text("Lasciare il gruppo?") },
            text = { Text("Se esci, non vedrai più questo gruppo nella tua Home, ma i dati non verranno cancellati per gli altri partecipanti.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.lasciaGruppo { navController.popBackStack() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Lascia Gruppo") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}

// ==========================================
// COMPONENTI MODULARI PER IL LAYOUT ADATTIVO
// ==========================================

@Composable
private fun SezioneHeaderGruppo(
    gruppo: Gruppo,
    onStatisticheClick: () -> Unit,
    onInvitaClick: () -> Unit,
    onModificaClick: () -> Unit,
    onLasciaClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(80.dp)
                    .padding(top = 4.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AvatarProvider.DisplayAvatar(
                        avatar = AvatarProvider.getAvatar(gruppo.avatarIndex),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = gruppo.nome,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "${gruppo.membriIds.size} componenti",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // BARRA AZIONI COMPATTA
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onStatisticheClick) {
                Icon(Icons.Default.BarChart, "Statistiche", tint = MaterialTheme.colorScheme.tertiary)
            }

            IconButton(onClick = onInvitaClick) {
                Icon(Icons.Default.Share, "Invita", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onModificaClick) {
                Icon(Icons.Default.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary)
            }

            IconButton(onClick = onLasciaClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Lascia Gruppo",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SezioneStoricoUscite(
    uscite: List<Uscita>,
    gruppoId: String,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Storico Uscite",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (uscite.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nessun viaggio registrato.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Aggiungi la prima uscita!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(uscite) { uscita ->
                    UscitaCard(
                        uscita = uscita,
                        onClick = { navController.navigate("uscita/${gruppoId}?uscitaId=${uscita.id}") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun UscitaCard(uscita: Uscita, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uscita.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                val sottotitolo = if (uscita.destinazione.isNotBlank()) "Verso: ${uscita.destinazione}" else "${uscita.partecipantiIds.size} partecipanti"
                Text(
                    text = sottotitolo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "${uscita.kmTotali} km",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}