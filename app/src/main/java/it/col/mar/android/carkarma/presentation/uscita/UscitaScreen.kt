package it.col.mar.android.carkarma.presentation.uscita

import androidx.compose.foundation.BorderStroke // <--- IMPORT AGGIUNTO
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
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

    // Stati
    val nomeUscita by viewModel.nomeUscita.collectAsState()
    val amiciDelGruppo by viewModel.amiciDelGruppo.collectAsState()
    val partecipantiSelezionati by viewModel.partecipantiSelezionati.collectAsState()
    val guidatoriSelezionati by viewModel.guidatoriSelezionati.collectAsState()
    val kmTotali by viewModel.kmTotali.collectAsState()

    val partenza by viewModel.indirizzoPartenza.collectAsState()
    val destinazione by viewModel.indirizzoDestinazione.collectAsState()
    val isAndataRitorno by viewModel.isAndataRitorno.collectAsState()
    val isLoadingMaps by viewModel.isLoadingMaps.collectAsState()

    val suggerimento by viewModel.suggerimentoGuidatore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showEliminaDialog by remember { mutableStateOf(false) }
    val isEditing = uscitaId.isNotEmpty()

    // Bottom Sheet per il suggerimento
    val sheetState = rememberModalBottomSheetState()
    var showSuggestionSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Mostra il sheet quando arriva un suggerimento
    LaunchedEffect(suggerimento) {
        if (suggerimento != null) {
            showSuggestionSheet = true
        }
    }

    val isFormValid = nomeUscita.isNotBlank() && kmTotali > 0 &&
            partecipantiSelezionati.isNotEmpty() &&
            guidatoriSelezionati.isNotEmpty()

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // FIX SPAZIO: Padding top calcolato dallo scaffold per evitare il buco bianco
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Intestazione
            Text(
                text = if (isEditing) "Modifica Uscita" else "Nuova Uscita",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Campo Nome
            OutlinedTextField(
                value = nomeUscita,
                onValueChange = { viewModel.onNomeUscitaChange(it) },
                label = { Text("Descrizione (es. Pizzata)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                leadingIcon = { Icon(Icons.Default.DirectionsCar, null) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- SEZIONE PERCORSO ---
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
                    // Campi Indirizzo
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = partenza,
                            onValueChange = { viewModel.onPartenzaChange(it) },
                            label = { Text("Partenza") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Place, null) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = destinazione,
                            onValueChange = { viewModel.onDestinazioneChange(it) },
                            label = { Text("Destinazione") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.AddLocation, null) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // RIGA: Bottone Calcolo + Switch A/R
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isEditing) {
                            Button(
                                onClick = { viewModel.calcolaDistanzaDaMaps() },
                                modifier = Modifier.weight(1f),
                                enabled = !isLoadingMaps,
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

                        // Switch A/R
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "A/R",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = isAndataRitorno,
                                onCheckedChange = { viewModel.onAndataRitornoChange(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo KM
                    OutlinedTextField(
                        value = if (kmTotali == 0) "" else kmTotali.toString(),
                        onValueChange = { text ->
                            if (text.all { it.isDigit() }) viewModel.setKmTotali(text.toIntOrNull() ?: 0)
                        },
                        label = { Text("Km Totali") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text("km") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SEZIONE PARTECIPANTI ---
            Text(
                text = "Chi è presente? (${partecipantiSelezionati.size})",
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.togglePartecipanteSelezionato(amico.id) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = partecipantiSelezionati.contains(amico.id),
                                    onCheckedChange = { viewModel.togglePartecipanteSelezionato(amico.id) }
                                )
                                Text(
                                    text = "${amico.nome} (${amico.postiAuto} posti)",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTTONE SUGGERIMENTO ALGORITMO (Con stile diverso) ---
            if (!isEditing && partecipantiSelezionati.size >= 2) {
                OutlinedButton(
                    onClick = { viewModel.calcolaSuggerimento() },
                    modifier = Modifier.fillMaxWidth(),
                    // QUI serviva l'import di BorderStroke
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chi dovrebbe guidare?", color = MaterialTheme.colorScheme.tertiary)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- SEZIONE GUIDATORI ---
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
                                    .clickable { viewModel.toggleGuidatoreSelezionato(amico.id) }
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = guidatoriSelezionati.contains(amico.id),
                                    onCheckedChange = { viewModel.toggleGuidatoreSelezionato(amico.id) }
                                )
                                Text(text = amico.nome, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- SALVATAGGIO ---
            Button(
                onClick = { viewModel.salvaUscita { navController.popBackStack() } },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Salva Uscita", fontWeight = FontWeight.Bold)
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = { showEliminaDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina Uscita")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // --- BOTTOM SHEET PER IL SUGGERIMENTO (NUOVO!) ---
    if (showSuggestionSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSuggestionSheet = false
                viewModel.resetSuggerimento()
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp), // Spazio per la navigation bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "L'Algoritmo ha scelto! 🔮",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Icona grande auto
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = suggerimento ?: "",
                    style = MaterialTheme.typography.headlineSmall, // Testo grande per il nome
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSuggestionSheet = false
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

    // Dialog Errore
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Attenzione") },
            text = { Text(errorMessage ?: "") },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("OK") } },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
        )
    }

    // Dialog Elimina
    if (showEliminaDialog) {
        AlertDialog(
            onDismissRequest = { showEliminaDialog = false },
            title = { Text("Eliminare?") },
            text = { Text("L'operazione è irreversibile.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.eliminaUscita { navController.popBackStack() }; showEliminaDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { showEliminaDialog = false }) { Text("Annulla") } }
        )
    }
}