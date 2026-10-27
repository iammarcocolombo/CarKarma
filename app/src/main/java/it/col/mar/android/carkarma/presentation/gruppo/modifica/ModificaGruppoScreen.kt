package it.col.mar.android.carkarma.presentation.gruppo.modifica

import android.content.res.Configuration
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.col.mar.android.carkarma.data.model.Amico
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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        // --- HEADER CON TASTO INDIETRO E ELIMINAZIONE ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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
                    text = if (!isEditing) "Crea Gruppo" else "Modifica Gruppo",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            }
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

        if (isLandscape) {
            // --- LAYOUT TABLET ORIZZONTALE (2 COLONNE AFFIANCATE) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // COLONNA SINISTRA: Info Gruppo, Icona e Azioni
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        SezioneDatiGruppo(
                            nomeGruppo = nomeGruppo,
                            onNomeGruppoChange = viewModel::onNomeGruppoChange,
                            avatarIndex = avatarIndex,
                            onAvatarSelected = viewModel::onAvatarSelected
                        )
                    }

                    Column {
                        SezionePulsantiAzione(
                            nomeGruppo = nomeGruppo,
                            onCreaAmico = { navController.navigate("amico?amicoId=&gruppoId=") },
                            onSalvaGruppo = {
                                viewModel.salvaGruppo { navController.popBackStack() }
                            }
                        )
                    }
                }

                // COLONNA DESTRA: Lista Amici Selezionabili
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    SezioneListaAmici(
                        amiciDisponibili = amiciDisponibili,
                        amiciSelezionati = amiciSelezionati,
                        gruppoId = gruppoId,
                        navController = navController,
                        onToggleAmico = viewModel::toggleAmicoSelezionato,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // --- LAYOUT VERTICALE CLASSICO (1 COLONNA SINGOLA) ---
            SezioneDatiGruppo(
                nomeGruppo = nomeGruppo,
                onNomeGruppoChange = viewModel::onNomeGruppoChange,
                avatarIndex = avatarIndex,
                onAvatarSelected = viewModel::onAvatarSelected
            )

            Spacer(modifier = Modifier.height(24.dp))

            SezioneListaAmici(
                amiciDisponibili = amiciDisponibili,
                amiciSelezionati = amiciSelezionati,
                gruppoId = gruppoId,
                navController = navController,
                onToggleAmico = viewModel::toggleAmicoSelezionato,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SezionePulsantiAzione(
                nomeGruppo = nomeGruppo,
                onCreaAmico = { navController.navigate("amico?amicoId=&gruppoId=") },
                onSalvaGruppo = {
                    viewModel.salvaGruppo { navController.popBackStack() }
                }
            )
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

// ==========================================
// COMPONENTI MODULARI PER IL LAYOUT ADATTIVO
// ==========================================

@Composable
private fun SezioneDatiGruppo(
    nomeGruppo: String,
    onNomeGruppoChange: (String) -> Unit,
    avatarIndex: Int,
    onAvatarSelected: (Int) -> Unit
) {
    Column {
        OutlinedTextField(
            value = nomeGruppo,
            onValueChange = onNomeGruppoChange,
            label = { Text("Nome Gruppo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onAvatarSelected(index) }
                ) {
                    AvatarProvider.DisplayAvatar(
                        avatar = avatar,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SezioneListaAmici(
    amiciDisponibili: List<Amico>,
    amiciSelezionati: Set<String>,
    gruppoId: String,
    navController: NavController,
    onToggleAmico: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Seleziona Amici",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(amiciDisponibili) { amico ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onToggleAmico(amico.id) }
                ) {
                    Checkbox(
                        checked = amiciSelezionati.contains(amico.id),
                        onCheckedChange = { onToggleAmico(amico.id) }
                    )
                    Text(
                        text = amico.nome,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f)
                    )

                    IconButton(onClick = {
                        val gid = if (gruppoId.isEmpty()) "" else gruppoId
                        navController.navigate("amico?amicoId=${amico.id}&gruppoId=$gid")
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica Amico")
                    }
                }
            }
        }
    }
}

@Composable
private fun SezionePulsantiAzione(
    nomeGruppo: String,
    onCreaAmico: () -> Unit,
    onSalvaGruppo: () -> Unit
) {
    Column {
        Button(
            onClick = onCreaAmico,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crea Nuovo Amico (Rubrica)", color = MaterialTheme.colorScheme.onSecondary)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSalvaGruppo,
            enabled = nomeGruppo.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Salva Gruppo")
        }
    }
}