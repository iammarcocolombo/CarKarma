package it.col.mar.android.carkarma.presentation.gruppo.modifica

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ModificaGruppoScreen(
    navController: NavController,
    gruppoId: Int,
    viewModel: ModificaGruppoViewModel
) {
    LaunchedEffect(gruppoId) {
        viewModel.loadGruppo(gruppoId)
    }

    val nomeGruppo by viewModel.nomeGruppo.collectAsState()
    val amiciDisponibili by viewModel.amiciDisponibili.collectAsState()
    val amiciSelezionati by viewModel.amiciSelezionati.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (gruppoId == -1) "Crea Nuovo Gruppo" else "Modifica Gruppo",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (gruppoId != -1) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina Gruppo")
                }
            }
        }


        OutlinedTextField(
            value = nomeGruppo,
            onValueChange = { viewModel.onNomeGruppoChange(it) },
            label = { Text("Nome Gruppo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Amici del Gruppo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(amiciDisponibili) { amico ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.toggleAmicoSelezionato(amico.id) }
                ) {
                    Checkbox(
                        checked = amiciSelezionati.contains(amico.id),
                        onCheckedChange = { viewModel.toggleAmicoSelezionato(amico.id) }
                    )
                    Text(
                        text = amico.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate("nuovoAmico") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Aggiungi Amico")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Aggiungi Amico",
                color = MaterialTheme.colorScheme.onSecondary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.salvaGruppo {
                    navController.popBackStack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Salva Gruppo")
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminare il gruppo?") },
            text = { Text("Sei sicuro di voler eliminare il gruppo e tutte le informazioni associate?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminaGruppo {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                    showDeleteDialog = false
                }
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }
}
