package it.col.mar.android.carkarma.presentation.amico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmicoScreen(
    navController: NavController,
    amicoId: String,
    viewModel: AmicoViewModel,
    gruppoId: String = ""
) {
    // Carichiamo i dati
    LaunchedEffect(amicoId) {
        viewModel.loadAmico(amicoId, gruppoId)
    }

    val nome by viewModel.nome.collectAsState()
    val postiAuto by viewModel.postiAuto.collectAsState()
    val tipoCarburante by viewModel.tipoCarburante.collectAsState()
    val consumoMedio by viewModel.consumoMedio.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val isEditing = amicoId.isNotEmpty()

    // Stato per il menu a tendina
    var expanded by remember { mutableStateOf(false) }
    val carbuanteOptions = listOf("Benzina", "Diesel", "GPL", "Metano", "Elettrico", "Ibrida")

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Intestazione
            Text(
                text = if (isEditing) "Modifica Amico" else "Nuovo Amico",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 0.dp)
            )

            // --- INFO GENERALI ---
            OutlinedTextField(
                value = nome,
                onValueChange = { viewModel.onNomeChange(it) },
                label = { Text("Nome e Cognome") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
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
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text("posti") }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- SEZIONE AUTO & CONSUMI ---
            Text(
                text = "Dati Auto (Opzionale)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // MENU A TENDINA CARBURANTE
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = tipoCarburante,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Alimentazione") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    leadingIcon = { Icon(Icons.Default.LocalGasStation, null) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    carbuanteOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.onTipoCarburanteChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // INPUT CONSUMO (Aggiornato a Km/L)
            OutlinedTextField(
                value = consumoMedio,
                onValueChange = { viewModel.onConsumoChange(it) },
                label = { Text("Efficienza") },
                placeholder = { Text("Es. 15") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.EvStation, null) },
                // KeyboardType.Decimal per permettere punto e virgola
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                // Suffisso dinamico: Km/L per carburanti liquidi, Km/kWh per elettrico
                suffix = { Text(if (tipoCarburante == "Elettrico") "Km/kWh" else "Km/L") }
            )

            // Helper Text Aggiornato
            Text(
                text = "Inserisci quanti km percorri con un litro (es. 15,5). Lascia vuoto per usare il valore standard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // BOTTONI
            Button(
                onClick = {
                    viewModel.salvaAmico {
                        navController.popBackStack()
                    }
                },
                enabled = nome.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (isEditing) "Salva Modifiche" else "Aggiungi alla Rubrica")
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
                        viewModel.eliminaAmico { navController.popBackStack() }
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