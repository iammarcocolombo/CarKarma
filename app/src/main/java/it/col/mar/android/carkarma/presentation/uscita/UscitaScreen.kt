package it.col.mar.android.carkarma.presentation.uscita

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    val nomeUscita by viewModel.nomeUscita.collectAsState()
    val amiciDelGruppo by viewModel.amiciDelGruppo.collectAsState()
    val partecipantiSelezionati by viewModel.partecipantiSelezionati.collectAsState()
    val guidatoriSelezionati by viewModel.guidatoriSelezionati.collectAsState()
    val kmTotali by viewModel.kmTotali.collectAsState()

    // Stato per il dialog del suggerimento algoritmo
    val suggerimento by viewModel.suggerimentoGuidatore.collectAsState()

    var showEliminaDialog by remember { mutableStateOf(false) }
    val isEditing = uscitaId.isNotEmpty()

    // Validazione base per abilitare il salvataggio
    val isFormValid = nomeUscita.isNotBlank() && kmTotali > 0 && partecipantiSelezionati.isNotEmpty() && guidatoriSelezionati.isNotEmpty()

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text(
            text = if (isEditing) "Modifica Uscita" else "Nuova Uscita",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = nomeUscita,
            onValueChange = { viewModel.onNomeUscitaChange(it) },
            label = { Text("Descrizione (es. Gita al Lago)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- SEZIONE PARTECIPANTI ---
        Text("Chi è presente?", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(amiciDelGruppo) { amico ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { viewModel.togglePartecipanteSelezionato(amico.id) }
                        .padding(vertical = 0.dp)
                ) {
                    Checkbox(
                        checked = partecipantiSelezionati.contains(amico.id),
                        onCheckedChange = { viewModel.togglePartecipanteSelezionato(amico.id) }
                    )
                    Text(text = amico.nome, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- BOTTONE CALCOLO (Tra le due liste) ---
        // Visibile solo se c'è almeno un partecipante selezionato
        if (partecipantiSelezionati.isNotEmpty()) {
            OutlinedButton(
                onClick = { viewModel.calcolaSuggerimento() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chi dovrebbe guidare?")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // --- SEZIONE GUIDATORI ---
        if (partecipantiSelezionati.isNotEmpty()) {
            Text("Chi ha guidato effettivamente?", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Filtriamo: mostriamo solo chi è stato selezionato come partecipante
                val candidatiGuidatori = amiciDelGruppo.filter { partecipantiSelezionati.contains(it.id) }

                items(candidatiGuidatori) { amico ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                            .clickable { viewModel.toggleGuidatoreSelezionato(amico.id) }
                            .padding(vertical = 0.dp)
                    ) {
                        Checkbox(
                            checked = guidatoriSelezionati.contains(amico.id),
                            onCheckedChange = { viewModel.toggleGuidatoreSelezionato(amico.id) }
                        )
                        Text(text = amico.nome)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- KM TOTALI ---
        OutlinedTextField(
            value = if (kmTotali == 0) "" else kmTotali.toString(),
            onValueChange = { text ->
                if (text.all { it.isDigit() }) {
                    viewModel.setKmTotali(text.toIntOrNull() ?: 0)
                }
            },
            label = { Text("Km Totali") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- BOTTONE SALVA ---
        Button(
            onClick = { viewModel.salvaUscita { navController.popBackStack() } },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salva Uscita")
        }

        if (isEditing) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { showEliminaDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Elimina Uscita")
            }
        }
    }

    // --- DIALOG PER IL SUGGERIMENTO ---
    if (suggerimento != null) {
        AlertDialog(
            onDismissRequest = { viewModel.resetSuggerimento() },
            title = { Text("L'algoritmo consiglia...") },
            text = {
                Text(
                    text = suggerimento ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSuggerimento() }) {
                    Text("OK, Ho capito")
                }
            },
            icon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
    }

    // --- DIALOG CONFERMA ELIMINAZIONE ---
    if (showEliminaDialog) {
        AlertDialog(
            onDismissRequest = { showEliminaDialog = false },
            title = { Text("Eliminare l'uscita?") },
            text = { Text("Questa operazione annullerà i km registrati per questo viaggio.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminaUscita { navController.popBackStack() }
                        showEliminaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEliminaDialog = false }) { Text("Annulla") }
            }
        )
    }
}