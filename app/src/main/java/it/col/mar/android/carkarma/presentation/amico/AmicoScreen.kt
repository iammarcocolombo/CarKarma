package it.col.mar.android.carkarma.presentation.amico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun AmicoScreen(
    navController: NavController,
    amicoId: String,
    viewModel: AmicoViewModel,
    gruppoId: String = ""
) {
    // Passiamo entrambi i parametri al ViewModel
    LaunchedEffect(amicoId) {
        viewModel.loadAmico(amicoId, gruppoId)
    }

    val nome by viewModel.nome.collectAsState()
    val postiAuto by viewModel.postiAuto.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val isEditing = amicoId.isNotEmpty()

    // Stato per lo scroll (utile se la tastiera copre i campi)
    val scrollState = rememberScrollState()

    // MODIFICA: Usiamo Box per coerenza con le altre schermate (edge-to-edge style)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // Padding solo orizzontale e inferiore, niente top extra
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Intestazione Uniformata
            Text(
                text = if (isEditing) "Modifica Amico" else "Nuovo Amico",
                style = MaterialTheme.typography.headlineMedium,
                // MODIFICA COLORE: Uniformato a onBackground
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                // Top a 0 per attaccarlo visivamente alla TopBar globale
                modifier = Modifier.padding(bottom = 16.dp, top = 0.dp)
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.salvaAmico {
                        navController.popBackStack()
                    }
                },
                enabled = nome.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isEditing) "Salva Modifiche" else "Aggiungi Amico")
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Elimina Amico")
                }
            }
        }
    }

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
                ) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") }
            }
        )
    }
}