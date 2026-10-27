package it.col.mar.android.carkarma.presentation.uscita

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.col.mar.android.carkarma.data.model.Amico
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UscitaScreen(
    navController: NavController,
    gruppoId: String,
    uscitaId: String,
    viewModel: UscitaViewModel
) {
    LaunchedEffect(gruppoId, uscitaId) {
        viewModel.loadUscita(gruppoId, uscitaId)
    }

    val nomeUscita by viewModel.nomeUscita.collectAsState()
    val amiciDelGruppo by viewModel.amiciDelGruppo.collectAsState()
    val partecipantiSelezionati by viewModel.partecipantiSelezionati.collectAsState()
    val ospitiMap by viewModel.ospitiMap.collectAsState()
    val guidatoriSelezionati by viewModel.guidatoriSelezionati.collectAsState()
    val kmTotali by viewModel.kmTotali.collectAsState()

    val partenza by viewModel.indirizzoPartenza.collectAsState()
    val destinazione by viewModel.indirizzoDestinazione.collectAsState()
    val isAndataRitorno by viewModel.isAndataRitorno.collectAsState()
    val isLoadingMaps by viewModel.isLoadingMaps.collectAsState()
    val isSalvataggioInCorso by viewModel.isSalvataggioInCorso.collectAsState()

    val suggerimento by viewModel.suggerimentoGuidatore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showEliminaDialog by remember { mutableStateOf(false) }
    val isEditing = uscitaId.isNotEmpty()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val isFormValid = nomeUscita.isNotBlank() && kmTotali > 0 &&
            partecipantiSelezionati.isNotEmpty() &&
            guidatoriSelezionati.isNotEmpty()

    val scrollState = rememberScrollState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = if (isEditing) "Modifica Uscita" else "Nuova Uscita",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 0.dp)
            )

            if (isLandscape) {
                // --- LAYOUT TABLET ORIZZONTALE (2 COLONNE AFFIANCATE) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // COLONNA SINISTRA: Dati Uscita & Percorso
                    Column(modifier = Modifier.weight(1f)) {
                        SezionePercorso(
                            nomeUscita = nomeUscita,
                            onNomeChange = viewModel::onNomeUscitaChange,
                            partenza = partenza,
                            onPartenzaChange = viewModel::onPartenzaChange,
                            destinazione = destinazione,
                            onDestinazioneChange = viewModel::onDestinazioneChange,
                            isAndataRitorno = isAndataRitorno,
                            onAndataRitornoChange = viewModel::onAndataRitornoChange,
                            kmTotali = kmTotali,
                            onKmChange = viewModel::setKmTotali,
                            isEditing = isEditing,
                            isLoadingMaps = isLoadingMaps,
                            isSalvataggioInCorso = isSalvataggioInCorso,
                            onCalcolaMaps = viewModel::calcolaDistanzaDaMaps
                        )
                    }

                    // COLONNA DESTRA: Partecipanti, Ospiti, Algoritmo e Guidatori
                    Column(modifier = Modifier.weight(1f)) {
                        SezionePersoneEGuidatori(
                            amiciDelGruppo = amiciDelGruppo,
                            partecipantiSelezionati = partecipantiSelezionati,
                            ospitiMap = ospitiMap,
                            guidatoriSelezionati = guidatoriSelezionati,
                            isEditing = isEditing,
                            isSalvataggioInCorso = isSalvataggioInCorso,
                            onTogglePartecipante = viewModel::togglePartecipanteSelezionato,
                            onIncrementaOspiti = viewModel::incrementaOspiti,
                            onDecrementaOspiti = viewModel::decrementaOspiti,
                            onCalcolaSuggerimento = viewModel::calcolaSuggerimento,
                            onToggleGuidatore = viewModel::toggleGuidatoreSelezionato
                        )
                    }
                }
            } else {
                // --- LAYOUT VERTICALE CLASSICO (1 COLONNA SINGOLA) ---
                SezionePercorso(
                    nomeUscita = nomeUscita,
                    onNomeChange = viewModel::onNomeUscitaChange,
                    partenza = partenza,
                    onPartenzaChange = viewModel::onPartenzaChange,
                    destinazione = destinazione,
                    onDestinazioneChange = viewModel::onDestinazioneChange,
                    isAndataRitorno = isAndataRitorno,
                    onAndataRitornoChange = viewModel::onAndataRitornoChange,
                    kmTotali = kmTotali,
                    onKmChange = viewModel::setKmTotali,
                    isEditing = isEditing,
                    isLoadingMaps = isLoadingMaps,
                    isSalvataggioInCorso = isSalvataggioInCorso,
                    onCalcolaMaps = viewModel::calcolaDistanzaDaMaps
                )

                Spacer(modifier = Modifier.height(24.dp))

                SezionePersoneEGuidatori(
                    amiciDelGruppo = amiciDelGruppo,
                    partecipantiSelezionati = partecipantiSelezionati,
                    ospitiMap = ospitiMap,
                    guidatoriSelezionati = guidatoriSelezionati,
                    isEditing = isEditing,
                    isSalvataggioInCorso = isSalvataggioInCorso,
                    onTogglePartecipante = viewModel::togglePartecipanteSelezionato,
                    onIncrementaOspiti = viewModel::incrementaOspiti,
                    onDecrementaOspiti = viewModel::decrementaOspiti,
                    onCalcolaSuggerimento = viewModel::calcolaSuggerimento,
                    onToggleGuidatore = viewModel::toggleGuidatoreSelezionato
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- PULSANTI AZIONE (IN BASSO A TUTTA LARGHEZZA) ---
            Button(
                onClick = { viewModel.salvaUscita { navController.popBackStack() } },
                enabled = isFormValid && !isSalvataggioInCorso,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSalvataggioInCorso) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvataggio...", fontWeight = FontWeight.Bold)
                } else {
                    Text("Salva Uscita", fontWeight = FontWeight.Bold)
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { showEliminaDialog = true },
                    enabled = !isSalvataggioInCorso,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina Uscita")
                }
            }
        }
    }

    // --- DIALOGS & BOTTOM SHEETS ---
    if (suggerimento != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.resetSuggerimento() },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "L'Algoritmo ha scelto! 🔮",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = suggerimento ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            viewModel.resetSuggerimento()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fantastico!")
                }
            }
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Attenzione") },
            text = { Text(errorMessage ?: "") },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }

    if (showEliminaDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSalvataggioInCorso) showEliminaDialog = false },
            title = { Text("Eliminare?") },
            text = { Text("L'operazione è irreversibile e i km verranno rimossi dalle statistiche.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.eliminaUscita { navController.popBackStack() }; showEliminaDialog = false },
                    enabled = !isSalvataggioInCorso,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = { TextButton(enabled = !isSalvataggioInCorso, onClick = { showEliminaDialog = false }) { Text("Annulla") } }
        )
    }
}

// ==========================================
// COMPONENTI MODULARI PER IL LAYOUT ADATTIVO
// ==========================================

@Composable
private fun SezionePercorso(
    nomeUscita: String,
    onNomeChange: (String) -> Unit,
    partenza: String,
    onPartenzaChange: (String) -> Unit,
    destinazione: String,
    onDestinazioneChange: (String) -> Unit,
    isAndataRitorno: Boolean,
    onAndataRitornoChange: (Boolean) -> Unit,
    kmTotali: Int,
    onKmChange: (Int) -> Unit,
    isEditing: Boolean,
    isLoadingMaps: Boolean,
    isSalvataggioInCorso: Boolean,
    onCalcolaMaps: () -> Unit
) {
    Column {
        OutlinedTextField(
            value = nomeUscita,
            onValueChange = onNomeChange,
            label = { Text("Descrizione (es. Pizzata)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSalvataggioInCorso,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            leadingIcon = { Icon(Icons.Default.DirectionsCar, null) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Percorso",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = partenza,
                        onValueChange = onPartenzaChange,
                        label = { Text("Partenza") },
                        modifier = Modifier.weight(1f),
                        enabled = !isSalvataggioInCorso,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Place, null) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = destinazione,
                        onValueChange = onDestinazioneChange,
                        label = { Text("Destinazione") },
                        modifier = Modifier.weight(1f),
                        enabled = !isSalvataggioInCorso,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AddLocation, null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isEditing) {
                        Button(
                            onClick = onCalcolaMaps,
                            modifier = Modifier.weight(1f),
                            enabled = !isLoadingMaps && !isSalvataggioInCorso,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            if (isLoadingMaps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Calcolo...")
                            } else {
                                Icon(Icons.Default.Place, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Calcola Km")
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "A/R",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = isAndataRitorno,
                            enabled = !isSalvataggioInCorso,
                            onCheckedChange = onAndataRitornoChange
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = if (kmTotali == 0) "" else kmTotali.toString(),
                    onValueChange = { text ->
                        if (text.all { it.isDigit() }) onKmChange(text.toIntOrNull() ?: 0)
                    },
                    label = { Text("Km Totali") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSalvataggioInCorso,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("km") }
                )
            }
        }
    }
}

@Composable
private fun SezionePersoneEGuidatori(
    amiciDelGruppo: List<Amico>,
    partecipantiSelezionati: Set<String>,
    ospitiMap: Map<String, Int>,
    guidatoriSelezionati: Set<String>,
    isEditing: Boolean,
    isSalvataggioInCorso: Boolean,
    onTogglePartecipante: (String) -> Unit,
    onIncrementaOspiti: (String) -> Unit,
    onDecrementaOspiti: (String) -> Unit,
    onCalcolaSuggerimento: () -> Unit,
    onToggleGuidatore: (String) -> Unit
) {
    val totaleOspiti = ospitiMap.values.sum()
    val totaleFisicoPersone = partecipantiSelezionati.size + totaleOspiti

    Column {
        Text(
            text = "Chi è presente? (${partecipantiSelezionati.size}${if (totaleOspiti > 0) " + $totaleOspiti ospiti = $totaleFisicoPersone persone" else ""})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                if (amiciDelGruppo.isEmpty()) {
                    Text("Nessun amico nel gruppo.", modifier = Modifier.padding(16.dp))
                } else {
                    amiciDelGruppo.forEach { amico ->
                        val isSelezionato = partecipantiSelezionati.contains(amico.id)
                        val numOspiti = ospitiMap[amico.id] ?: 0

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSalvataggioInCorso) { onTogglePartecipante(amico.id) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = isSelezionato,
                                    enabled = !isSalvataggioInCorso,
                                    onCheckedChange = { onTogglePartecipante(amico.id) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${amico.nome} (${amico.postiAuto} posti)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (isSelezionato) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp, end = 16.dp, bottom = 6.dp)
                                ) {
                                    Text(
                                        text = if (numOspiti > 0) "Ospiti al seguito: $numOspiti" else "Aggiungi ospiti:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (numOspiti > 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if (numOspiti > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onDecrementaOspiti(amico.id) },
                                            enabled = !isSalvataggioInCorso && numOspiti > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Meno ospiti", modifier = Modifier.size(16.dp))
                                        }

                                        Text(
                                            text = "+$numOspiti",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (numOspiti > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(
                                            onClick = { onIncrementaOspiti(amico.id) },
                                            enabled = !isSalvataggioInCorso,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Più ospiti", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isEditing && partecipantiSelezionati.size >= 2) {
            OutlinedButton(
                onClick = onCalcolaSuggerimento,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSalvataggioInCorso,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chi dovrebbe guidare?", color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (partecipantiSelezionati.isNotEmpty()) {
            Text(
                text = "Chi ha guidato?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    val candidati = amiciDelGruppo.filter { partecipantiSelezionati.contains(it.id) }

                    candidati.forEach { amico ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSalvataggioInCorso) { onToggleGuidatore(amico.id) }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = guidatoriSelezionati.contains(amico.id),
                                enabled = !isSalvataggioInCorso,
                                onCheckedChange = { onToggleGuidatore(amico.id) }
                            )
                            Text(text = amico.nome, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}