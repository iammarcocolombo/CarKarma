package it.col.mar.android.carkarma.presentation.calcolo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import it.col.mar.android.carkarma.data.database.AppContainer
import java.util.Locale

@Composable
fun CalcoloScreen(navController: NavController) {
    val viewModel: CalcoloViewModel = viewModel(
        factory = CalcoloViewModelFactory(
            AppContainer.gruppoRepository,
            AppContainer.amicoRepository
        )
    )

    val gruppi by viewModel.gruppi.collectAsState()
    val gruppoSelezionato by viewModel.gruppoSelezionato.collectAsState()
    val membri by viewModel.membriDelGruppo.collectAsState()
    val presenti by viewModel.presentiSelezionati.collectAsState()
    val risultato by viewModel.risultatoCalcolo.collectAsState()

    var menuEspanso by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Chi guida stasera?",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Selezione Gruppo
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { menuEspanso = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(gruppoSelezionato?.nome ?: "Seleziona un Gruppo")
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = menuEspanso,
                onDismissRequest = { menuEspanso = false }
            ) {
                gruppi.forEach { gruppo ->
                    DropdownMenuItem(
                        text = { Text(gruppo.nome) },
                        onClick = {
                            viewModel.selezionaGruppo(gruppo.id)
                            menuEspanso = false
                        }
                    )
                }
            }
        }

        if (gruppoSelezionato != null) {
            Spacer(modifier = Modifier.height(16.dp))

            // 2. Selezione Presenti
            Text("Chi è presente?", style = MaterialTheme.typography.titleMedium)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(membri) { amico ->
                    val isSelected = presenti.contains(amico.id)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.togglePresente(amico.id) }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.togglePresente(amico.id) }
                        )
                        Text(amico.nome, modifier = Modifier.weight(1f))

                        // Mostriamo info utili
                        Text(
                            "${amico.km} km / ${amico.uscite} uscite",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            // 3. Bottone Calcola
            Button(
                onClick = { viewModel.calcola() },
                enabled = presenti.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("CALCOLA KARMA")
            }

            // 4. Risultato
            if (risultato.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ordine Guidatori:", style = MaterialTheme.typography.titleMedium)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Il primo della lista è il prescelto!
                        val (guidatore, score) = risultato.first()
                        Text(
                            text = "🚗 TOCCA A: ${guidatore.nome.uppercase()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Punteggio Karma: ${String.format(Locale.US, "%.1f", score)} (Più è basso, più toccherebbe a te)")

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Gli altri
                        risultato.drop(1).forEachIndexed { index, (amico, score) ->
                            Text(
                                text = "${index + 2}. ${amico.nome} (${String.format(Locale.US, "%.1f", score)})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        } else {
            // Messaggio se non è selezionato nulla
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Seleziona un gruppo per iniziare", color = Color.Gray)
            }
        }
    }
}