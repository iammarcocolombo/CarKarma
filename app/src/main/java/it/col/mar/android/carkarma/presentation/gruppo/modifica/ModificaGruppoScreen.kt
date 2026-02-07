package it.col.mar.android.carkarma.presentation.gruppo.modifica

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.col.mar.android.carkarma.util.AvatarProvider

@Composable
fun ModificaGruppoScreen(
    navController: NavController,
    gruppoId: String,
    viewModel: ModificaGruppoViewModel
) {
    LaunchedEffect(gruppoId) {
        viewModel.loadGruppo(gruppoId)
    }

    val nomeGruppo by viewModel.nomeGruppo.collectAsState()
    val avatarIndex by viewModel.selectedAvatarIndex.collectAsState()
    val amiciDisponibili by viewModel.amiciDisponibili.collectAsState()
    val amiciSelezionati by viewModel.amiciSelezionati.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val isEditing = gruppoId.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            // MODIFICA: Padding solo orizzontale e inferiore.
            // Rimosso il padding 'top' implicito del 'padding(16.dp)' per attaccarlo alla barra.
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // Intestazione
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Aggiungiamo un minimo di spazio sotto il titolo
                .padding(bottom = 16.dp, top = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (!isEditing) "Crea Nuovo Gruppo" else "Modifica Gruppo",
                style = MaterialTheme.typography.headlineMedium,
                // MODIFICA COLORE: Uniformato a onBackground
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            if (isEditing) {
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Elimina Gruppo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Campo Nome
        OutlinedTextField(
            value = nomeGruppo,
            onValueChange = { viewModel.onNomeGruppoChange(it) },
            label = { Text("Nome Gruppo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- SELETTORE AVATAR ---
        Text("Scegli un'icona:", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(AvatarProvider.avatars) { index, avatar ->
                val isSelected = index == avatarIndex

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { viewModel.onAvatarSelected(index) }
                ) {
                    AvatarProvider.DisplayAvatar(
                        avatar = avatar,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp).size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- LISTA AMICI ---
        Text(
            text = "Seleziona Amici",
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
                        modifier = Modifier.padding(start = 8.dp).weight(1f)
                    )

                    IconButton(onClick = {
                        navController.navigate("amico?amicoId=${amico.id}&gruppoId=$gruppoId")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica Amico")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tasto Nuovo Amico
        Button(
            onClick = { navController.navigate("amico") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crea Nuovo Amico (Rubrica)", color = MaterialTheme.colorScheme.onSecondary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tasto Salva
        Button(
            onClick = {
                viewModel.salvaGruppo {
                    navController.popBackStack()
                }
            },
            enabled = nomeGruppo.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Salva Gruppo")
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminare il gruppo?") },
            text = { Text("Sei sicuro? Verranno eliminate anche tutte le uscite associate.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminaGruppo {
                            navController.navigate("home") { popUpTo("home") { inclusive = true } }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Annulla") } }
        )
    }
}