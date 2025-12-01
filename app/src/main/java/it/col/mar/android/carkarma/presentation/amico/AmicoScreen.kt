package it.col.mar.android.carkarma.presentation.amico

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AmicoScreen(
    navController: NavController,
    amicoId: String, // Ora è String per Firebase
    viewModel: AmicoViewModel
) {
    // Carichiamo i dati appena entriamo nella schermata
    LaunchedEffect(amicoId) {
        viewModel.loadAmico(amicoId)
    }

    val nome by viewModel.nome.collectAsState()
    val postiAuto by viewModel.postiAuto.collectAsState()

    // Stato locale per gestire il dialog di eliminazione
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Determiniamo se stiamo creando o modificando
    val isEditing = amicoId.isNotEmpty()

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = if (isEditing) "Modifica Amico" else "Nuovo Amico",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = nome,
            onValueChange = { viewModel.onNomeChange(it) },
            label = { Text("Nome") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = postiAuto,
            onValueChange = { viewModel.onPostiAutoChange(it) },
            label = { Text("Posti Auto") },
            singleLine = true,
            // Ritocco: Tastiera numerica per i posti auto
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Bottone Salva
        Button(
            onClick = {
                viewModel.salvaAmico {
                    navController.popBackStack()
                }
            },
            enabled = nome.isNotBlank(), // Disabilita se non c'è il nome
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isEditing) "Salva Modifiche" else "Aggiungi Amico")
        }

        // Bottone Elimina (visibile solo se stiamo modificando)
        if (isEditing) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Elimina Amico")
            }
        }
    }

    // Dialog di conferma eliminazione
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminare l'amico?") },
            text = { Text("Sei sicuro? Questa azione non può essere annullata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminaAmico {
                            navController.popBackStack()
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
            }
        )
    }
}