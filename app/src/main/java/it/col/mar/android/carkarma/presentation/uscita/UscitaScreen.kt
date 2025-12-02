package it.col.mar.android.carkarma.presentation.uscita

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

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

    // --- STATI UI ---
    val nomeUscita by viewModel.nomeUscita.collectAsState()
    val amiciDelGruppo by viewModel.amiciDelGruppo.collectAsState()
    val partecipantiSelezionati by viewModel.partecipantiSelezionati.collectAsState()
    val guidatoriSelezionati by viewModel.guidatoriSelezionati.collectAsState()
    val kmTotali by viewModel.kmTotali.collectAsState()

    // Stati Mappe & Calcolo
    val partenza by viewModel.indirizzoPartenza.collectAsState()
    val destinazione by viewModel.indirizzoDestinazione.collectAsState()
    val isAndataRitorno by viewModel.isAndataRitorno.collectAsState()
    val isLoadingMaps by viewModel.isLoadingMaps.collectAsState()

    // Stati Dialog
    val suggerimento by viewModel.suggerimentoGuidatore.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showEliminaDialog by remember { mutableStateOf(false) }
    val isEditing = uscitaId.isNotEmpty()

    // Validazione
    val isFormValid = nomeUscita.isNotBlank() && kmTotali > 0 &&
            partecipantiSelezionati.isNotEmpty() &&
            guidatoriSelezionati.isNotEmpty()

    // Scroll dell'intera pagina
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- TITOLO ---
        Text(
            text = if (isEditing) "Modifica Uscita" else "Nuova Uscita",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- CAMPO NOME ---
        OutlinedTextField(
            value = nomeUscita,
            onValueChange = { viewModel.onNomeUscitaChange(it) },
            label = { Text("Descrizione (es. Pizzata)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SEZIONE PERCORSO E KM ---
        Text(
            text = "Percorso",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Campi Indirizzo
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = partenza,
                        onValueChange = { viewModel.onPartenzaChange(it) },
                        label = { Text("Partenza") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = destinazione,
                        onValueChange = { viewModel.onDestinazioneChange(it) },
                        label = { Text("Destinazione") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RIGA: Bottone Calcolo + Switch A/R
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Il bottone per calcolare appare solo se è una NUOVA uscita
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
                        // Se siamo in modifica, riempiamo lo spazio a sinistra
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Switch A/R (Sempre visibile)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "A/R",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = isAndataRitorno,
                            onCheckedChange = { viewModel.onAndataRitornoChange(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Campo KM (Editabile)
                OutlinedTextField(
                    value = if (kmTotali == 0) "" else kmTotali.toString(),
                    onValueChange = { text ->
                        if (text.all { it.isDigit() }) viewModel.setKmTotali(text.toIntOrNull() ?: 0)
                    },
                    label = { Text("Km Totali") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
                                .padding(horizontal = 8.dp, vertical = 0.dp) // Padding ridotto per compattezza
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

        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTTONE SUGGERIMENTO ---
        // Solo per nuove uscite e con min. 2 partecipanti
        if (!isEditing && partecipantiSelezionati.size >= 2) {
            OutlinedButton(
                onClick = { viewModel.calcolaSuggerimento() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chi dovrebbe guidare?")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- SEZIONE GUIDATORI ---
        if (partecipantiSelezionati.isNotEmpty()) {
            Text(
                text = "Chi ha guidato?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    val candidati = amiciDelGruppo.filter { partecipantiSelezionati.contains(it.id) }

                    candidati.forEach { amico ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleGuidatoreSelezionato(amico.id) }
                                .padding(horizontal = 8.dp, vertical = 0.dp)
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

        // --- TASTO SALVA ---
        Button(
            onClick = { viewModel.salvaUscita { navController.popBackStack() } },
            enabled = isFormValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Salva Uscita")
        }

        // --- TASTO ELIMINA (Solo Modifica) ---
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

        // Spazio extra per lo scroll
        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- DIALOGHI ---
    if (suggerimento != null) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSuggerimento() },
            title = { Text("L'algoritmo consiglia...") },
            text = { Text(suggerimento ?: "", textAlign = TextAlign.Center) },
            confirmButton = { TextButton(onClick = { viewModel.resetSuggerimento() }) { Text("OK") } },
            icon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
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
            onDismissRequest = { showEliminaDialog = false },
            title = { Text("Eliminare?") },
            text = { Text("L'operazione è irreversibile e i km verranno rimossi dalle statistiche.") },
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