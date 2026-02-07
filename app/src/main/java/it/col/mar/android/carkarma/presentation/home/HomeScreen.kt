package it.col.mar.android.carkarma.presentation.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Stato per il Dialog "Unisciti" manuale
    var showJoinDialog by remember { mutableStateOf(false) }
    var codiceGruppoInput by remember { mutableStateOf("") }

    // Gestione risultato Join
    LaunchedEffect(joinState) {
        when(joinState) {
            is JoinState.Success -> {
                Toast.makeText(context, "Ti sei unito al gruppo!", Toast.LENGTH_SHORT).show()
                showJoinDialog = false
                viewModel.resetJoinState()
                codiceGruppoInput = ""
            }
            is JoinState.Error -> {
                // L'errore viene mostrato nel dialog
            }
            else -> {}
        }
    }

    // MODIFICA: Usiamo un Box invece dello Scaffold interno per controllo totale del layout (come GruppoScreen)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Titolo interno (Padding ottimizzato)
            Text(
                text = "I tuoi Gruppi",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 0.dp)
            )

            if (gruppi.isEmpty()) {
                // --- EMPTY STATE ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
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
                // --- LISTA GRUPPI ---
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 120.dp) // Spazio abbondante per i FAB
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

        // FABs Posizionati manualmente in basso a destra
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            // FAB SECONDARIO: Unisciti a Gruppo (Codice manuale)
            SmallFloatingActionButton(
                onClick = { showJoinDialog = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Unisciti")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FAB PRINCIPALE: Crea Gruppo
            FloatingActionButton(
                onClick = { navController.navigate("modificaGruppo") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuovo Gruppo")
            }
        }
    }

    // --- DIALOG UNISCITI ---
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false; viewModel.resetJoinState() },
            title = { Text("Unisciti a un Gruppo") },
            text = {
                Column {
                    Text("Inserisci il codice ID del gruppo:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = codiceGruppoInput,
                        onValueChange = { codiceGruppoInput = it },
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
                TextButton(onClick = { showJoinDialog = false; viewModel.resetJoinState() }) {
                    Text("Annulla")
                }
            }
        )
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
                    // USO DELL'AVATAR PROVIDER
                    // Mostra l'icona scelta o quella di default se l'indice non è valido
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