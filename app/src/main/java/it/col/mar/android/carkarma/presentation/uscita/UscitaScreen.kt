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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun UscitaScreen(
    navController: NavController,
    gruppoId: Int,
    uscitaId: Int,
    viewModel: UscitaViewModel
) {
    LaunchedEffect(gruppoId, uscitaId) {
        viewModel.loadUscita(gruppoId, uscitaId)
    }

    val nomeUscita by viewModel.nomeUscita.collectAsState()
    val amiciDisponibili by viewModel.amiciDisponibili.collectAsState()
    val partecipantiSelezionati by viewModel.partecipantiSelezionati.collectAsState()
    val guidatoriSelezionati by viewModel.guidatoriSelezionati.collectAsState()
    val kmTotali by viewModel.kmTotali.collectAsState()

    var partenza by remember { mutableStateOf("") }
    var arrivo by remember { mutableStateOf("") }
    var showEliminaDialog by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        OutlinedTextField(
            value = nomeUscita,
            onValueChange = { viewModel.onNomeUscitaChange(it) },
            label = { Text("Nome Uscita") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Partecipanti:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(amiciDisponibili) { amico ->
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
                    Text(amico.nome)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Guidatori:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(amiciDisponibili) { amico ->
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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = kmTotali.toString(),
            onValueChange = { text ->
                text.toIntOrNull()?.let { viewModel.setKmTotali(it) }
            },
            label = { Text("Km Totali") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = partenza,
            onValueChange = { partenza = it },
            label = { Text("Punto di Partenza") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = arrivo,
            onValueChange = { arrivo = it },
            label = { Text("Punto di Arrivo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            // TODO: qui integri API Google Maps per calcolare km
            val kmSimulati = 42
            viewModel.setKmTotali(kmSimulati)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Calcola Km Totali")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.salvaUscita {
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Salva Uscita")
        }
        if (uscitaId != -1) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showEliminaDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Elimina Uscita")
            }
        }

    }
    if (showEliminaDialog) {
        AlertDialog(
            onDismissRequest = { showEliminaDialog = false },
            title = { Text("Conferma eliminazione") },
            text = { Text("Sei sicuro di voler eliminare questa uscita? L'operazione non è reversibile.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEliminaDialog = false
                        viewModel.eliminaUscita {
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                Button(onClick = { showEliminaDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

}
