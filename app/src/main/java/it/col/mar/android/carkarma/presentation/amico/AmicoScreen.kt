package it.col.mar.android.carkarma.presentation.amico

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    // Sincronizzazione dati all'avvio o al variare dell'ID
    LaunchedEffect(amicoId) {
        viewModel.loadAmico(amicoId, gruppoId)
    }

    val nome by viewModel.nome.collectAsState()
    val postiAuto by viewModel.postiAuto.collectAsState()
    val tipoCarburante by viewModel.tipoCarburante.collectAsState()
    val consumoMedio by viewModel.consumoMedio.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val isEditing = amicoId.isNotEmpty()

    var expanded by remember { mutableStateOf(false) }
    val carbuanteOptions = listOf("Benzina", "Diesel", "GPL", "Metano", "Elettrico", "Ibrida")

    val scrollState = rememberScrollState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

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
            // --- HEADER COMPATTO CON TASTO INDIETRO ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 20.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isEditing) "Modifica Amico" else "Nuovo Amico",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (isLandscape) {
                // --- LAYOUT TABLET ORIZZONTALE (2 COLONNE AFFIANCATE) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // COLONNA SINISTRA: Informazioni Personali
                    Column(modifier = Modifier.weight(1f)) {
                        SezioneInfoGenerali(
                            nome = nome,
                            onNomeChange = viewModel::onNomeChange,
                            postiAuto = postiAuto,
                            onPostiAutoChange = viewModel::onPostiAutoChange
                        )
                    }

                    // COLONNA DESTRA: Consumi Auto
                    Column(modifier = Modifier.weight(1f)) {
                        SezioneConsumiAuto(
                            tipoCarburante = tipoCarburante,
                            onTipoCarburanteChange = viewModel::onTipoCarburanteChange,
                            consumoMedio = consumoMedio,
                            onConsumoChange = viewModel::onConsumoChange,
                            carbuanteOptions = carbuanteOptions,
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        )
                    }
                }
            } else {
                // --- LAYOUT VERTICALE CLASSICO (1 COLONNA SINGOLA) ---
                SezioneInfoGenerali(
                    nome = nome,
                    onNomeChange = viewModel::onNomeChange,
                    postiAuto = postiAuto,
                    onPostiAutoChange = viewModel::onPostiAutoChange
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                SezioneConsumiAuto(
                    tipoCarburante = tipoCarburante,
                    onTipoCarburanteChange = viewModel::onTipoCarburanteChange,
                    consumoMedio = consumoMedio,
                    onConsumoChange = viewModel::onConsumoChange,
                    carbuanteOptions = carbuanteOptions,
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- BOTTONI SALVATAGGIO / ELIMINAZIONE ---
            Button(
                onClick = {
                    viewModel.salvaAmico {
                        navController.popBackStack()
                    }
                },
                enabled = nome.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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

// ==========================================
// COMPONENTI MODULARI PER IL LAYOUT ADATTIVO
// ==========================================

@Composable
private fun SezioneInfoGenerali(
    nome: String,
    onNomeChange: (String) -> Unit,
    postiAuto: String,
    onPostiAutoChange: (String) -> Unit
) {
    Column {
        Text(
            text = "Informazioni Personali",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = nome,
            onValueChange = onNomeChange,
            label = { Text("Nome e Cognome") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = postiAuto,
            onValueChange = onPostiAutoChange,
            label = { Text("Posti Auto") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text("posti") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SezioneConsumiAuto(
    tipoCarburante: String,
    onTipoCarburanteChange: (String) -> Unit,
    consumoMedio: String,
    onConsumoChange: (String) -> Unit,
    carbuanteOptions: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Dati Auto (Opzionale)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange,
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
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                carbuanteOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onTipoCarburanteChange(option)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = consumoMedio,
            onValueChange = onConsumoChange,
            label = { Text("Consumo Medio") },
            placeholder = { Text("Es. 7.5") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.EvStation, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            suffix = { Text(if (tipoCarburante == "Elettrico") "kWh/100km" else "L/100km") }
        )

        Text(
            text = "Inserisci quanti litri (o kWh) servono per fare 100km. Lascia vuoto per usare lo standard.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        )
    }
}