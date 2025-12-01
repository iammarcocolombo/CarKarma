package it.col.mar.android.carkarma.presentation.uscita

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun UscitaScreen(
    navController: NavController,
    gruppoId: String,
    uscitaId: String, // Stringa (vuota se nuova uscita)
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

    // Stato locale per campi UI non ancora nel DB (Partenza/Arrivo)
    var partenza by remember { mutableStateOf("") }
    var arrivo by remember { mutableStateOf("") }
    var showEliminaDialog by remember { mutableStateOf(false) }

    val isEditing = uscitaId.isNotEmpty()

    // Validazione semplice per abilitare il tasto salva
    val isFormValid = nomeUscita.isNotBlank() &&
            kmTotali > 0 &&
            partecipantiSelezionati.isNotEmpty() &&
            guidatoriSelezionati.isNotEmpty()

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

        // Sezione Partecipanti
        Text("Chi era presente?", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(amiciDelGruppo) { amico ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.togglePartecipanteSelezionato(amico.id) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = partecipantiSelezionati.contains(amico.id),
                        onCheckedChange = { viewModel.togglePartecipanteSelezionato(amico.id) }
                    )
                    Text(
                        text = amico.nome,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sezione Guidatori (filtrata: mostra solo chi è stato selezionato come partecipante)
        // Se uno non c'era, non può aver guidato!
        if (partecipantiSelezionati.isNotEmpty()) {
            Text("Chi ha guidato?", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Mostriamo solo chi è tra i partecipanti
                val candidatiGuidatori = amiciDelGruppo.filter { partecipantiSelezionati.contains(it.id) }

                items(candidatiGuidatori) { amico ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleGuidatoreSelezionato(amico.id) }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = guidatoriSelezionati.contains(amico.id),
                            onCheckedChange = { viewModel.toggleGuidatoreSelezionato(amico.id) }
                        )
                        Text(amico.nome)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input KM
        OutlinedTextField(
            value = if (kmTotali == 0) "" else kmTotali.toString(),
            onValueChange = { text ->
                // Accetta solo numeri
                if (text.all { it.isDigit() }) {
                    viewModel.setKmTotali(text.toIntOrNull() ?: 0)
                }
            },
            label = { Text("Km Totali") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // Placeholder per integrazione futura Mappe
        /*
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = partenza, onValueChange = { partenza = it },
                label = { Text("Partenza") }, modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = arrivo, onValueChange = { arrivo = it },
                label = { Text("Arrivo") }, modifier = Modifier.weight(1f)
            )
        }
        Button(onClick = { viewModel.setKmTotali(42) }, modifier = Modifier.fillMaxWidth()) {
            Text("Calcola con Maps (Demo)")
        }
        */

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.salvaUscita {
                    navController.popBackStack()
                }
            },
            enabled = isFormValid, // Disabilitato se mancano dati
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
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

    if (showEliminaDialog) {
        AlertDialog(
            onDismissRequest = { showEliminaDialog = false },
            title = { Text("Eliminare l'uscita?") },
            text = { Text("Questa operazione annullerà i km registrati per questo viaggio.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminaUscita {
                            navController.popBackStack()
                        }
                        showEliminaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEliminaDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}