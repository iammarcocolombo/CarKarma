package it.col.mar.android.carkarma.presentation.home

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import it.col.mar.android.carkarma.data.database.AppContainer
import it.col.mar.android.carkarma.data.model.Gruppo
import it.col.mar.android.carkarma.util.AvatarProvider

@Composable
fun HomeScreen(
    navController: NavHostController
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(AppContainer.gruppoRepository)
    )

    val gruppi by viewModel.gruppi.collectAsState()
    val joinState by viewModel.joinState.collectAsState()

    val context = LocalContext.current

    var showJoinDialog by remember { mutableStateOf(false) }
    var codiceGruppoInput by remember { mutableStateOf("") }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun chiudiDialog() {
        showJoinDialog = false
        codiceGruppoInput = ""
        viewModel.resetJoinState()
    }

    LaunchedEffect(joinState) {
        when (joinState) {
            is JoinState.Success -> {
                Toast.makeText(context, "Ti sei unito al gruppo!", Toast.LENGTH_SHORT).show()
                chiudiDialog()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "I tuoi Gruppi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (isLandscape) {
                // --- LAYOUT TABLET ORIZZONTALE (2 COLONNE AFFIANCATE) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // COLONNA SINISTRA: Lista o Griglia dei Gruppi
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                    ) {
                        SezioneListaGruppi(
                            gruppi = gruppi,
                            navController = navController,
                            isLandscape = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // COLONNA DESTRA: Pannello Azioni Rapide
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        PannelloAzioniRapideTablet(
                            onCreaGruppoClick = { navController.navigate("modificaGruppo") },
                            onUniscitiClick = { showJoinDialog = true }
                        )
                    }
                }
            } else {
                // --- LAYOUT VERTICALE CLASSICO (1 COLONNA SINGOLA) ---
                SezioneListaGruppi(
                    gruppi = gruppi,
                    navController = navController,
                    isLandscape = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Floating Action Buttons (visibili solo in modalità verticale portrait)
        if (!isLandscape) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Unisciti")
                }

                Spacer(modifier = Modifier.height(16.dp))

                FloatingActionButton(
                    onClick = { navController.navigate("modificaGruppo") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuovo Gruppo")
                }
            }
        }
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { chiudiDialog() },
            title = { Text("Unisciti a un Gruppo") },
            text = {
                Column {
                    Text("Inserisci il codice ID o l'indirizzo d'invito del gruppo:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codiceGruppoInput,
                        onValueChange = {
                            codiceGruppoInput = it
                            if (joinState is JoinState.Error) viewModel.resetJoinState()
                        },
                        label = { Text("Codice Gruppo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = joinState is JoinState.Error,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    if (joinState is JoinState.Error) {
                        Text(
                            text = (joinState as JoinState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.uniscitiAlGruppo(codiceGruppoInput.trim()) },
                    enabled = codiceGruppoInput.isNotBlank() && joinState !is JoinState.Loading
                ) {
                    if (joinState is JoinState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Entra")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { chiudiDialog() }) {
                    Text("Annulla")
                }
            }
        )
    }
}

// ==========================================
// COMPONENTI MODULARI PER IL LAYOUT ADATTIVO
// ==========================================

@Composable
private fun SezioneListaGruppi(
    gruppi: List<Gruppo>,
    navController: NavHostController,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    if (gruppi.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Non hai ancora nessun gruppo.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Crea un gruppo o unisciti con un codice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        if (isLandscape) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(gruppi) { gruppo ->
                    GruppoCard(
                        gruppo = gruppo,
                        onClick = { navController.navigate("gruppo/${gruppo.id}") }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(gruppi) { gruppo ->
                    GruppoCard(
                        gruppo = gruppo,
                        onClick = { navController.navigate("gruppo/${gruppo.id}") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PannelloAzioniRapideTablet(
    onCreaGruppoClick: () -> Unit,
    onUniscitiClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Gestione Gruppi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crea un nuovo gruppo di viaggio per i tuoi turni guidatore o unisciti ad uno già esistente.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onCreaGruppoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crea Nuovo Gruppo")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onUniscitiClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unisciti con Codice")
            }
        }
    }
}

@Composable
fun GruppoCard(
    gruppo: Gruppo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AvatarProvider.DisplayAvatar(
                        avatar = AvatarProvider.getAvatar(gruppo.avatarIndex),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gruppo.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${gruppo.membriIds.size} membri",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}