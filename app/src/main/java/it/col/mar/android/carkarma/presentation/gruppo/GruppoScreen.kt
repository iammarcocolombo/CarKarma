package it.col.mar.android.carkarma.presentation.gruppo

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import it.col.mar.android.carkarma.util.AvatarProvider
import it.col.mar.android.carkarma.util.QrCodeGenerator

@Composable
fun GruppoScreen(
    navController: NavController,
    gruppoId: String,
    viewModel: GruppoViewModel
) {
    LaunchedEffect(gruppoId) {
        viewModel.loadGruppo(gruppoId)
    }

    val gruppo by viewModel.gruppo.collectAsState()
    val uscite by viewModel.uscite.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showShareDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
        }
        return
    }

    if (gruppo == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("uscita/${gruppoId}") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuova Uscita") }
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- AVATAR GRUPPO ---
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        // CORREZIONE: Usiamo DisplayAvatar per gestire il tipo CarKarmaAvatar
                        AvatarProvider.DisplayAvatar(
                            avatar = AvatarProvider.getAvatar(gruppo!!.avatarIndex),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- HEADER GRUPPO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gruppo!!.nome,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${gruppo!!.membriIds.size} componenti",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, "Invita", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { navController.navigate("modificaGruppo?gruppoId=${gruppo!!.id}") }) {
                        Icon(Icons.Default.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = { showLeaveDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Lascia Gruppo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Storico Uscite",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uscite.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nessun viaggio registrato.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Aggiungi la prima uscita!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uscite) { uscita ->
                        UscitaCard(
                            uscita = uscita,
                            onClick = { navController.navigate("uscita/${gruppoId}?uscitaId=${uscita.id}") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showShareDialog && gruppo != null) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Invita Amici", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Fai scansionare questo QR o invia il codice:", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))

                    val qrBitmap = remember(gruppo!!.id) { QrCodeGenerator.generateQrCode(gruppo!!.id) }
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(200.dp).padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        onClick = {
                            clipboardManager.setText(AnnotatedString(gruppo!!.id))
                            Toast.makeText(context, "Codice copiato!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(
                            text = gruppo!!.id,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Text("Tocca il codice per copiarlo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Unisciti al mio gruppo su CarKarma! Usa questo codice:\n\n${gruppo!!.id}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Invia invito..."))
                }) {
                    Text("Invia Codice")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("Chiudi") }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Lasciare il gruppo?") },
            text = { Text("Se esci, non vedrai più questo gruppo nella tua Home, ma i dati non verranno cancellati per gli altri partecipanti.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.lasciaGruppo { navController.popBackStack() }; showLeaveDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Lascia Gruppo") }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Annulla") } }
        )
    }
}

@Composable
fun UscitaCard(uscita: it.col.mar.android.carkarma.data.model.Uscita, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = uscita.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                val sottotitolo = if (uscita.destinazione.isNotBlank()) "Verso: ${uscita.destinazione}" else "${uscita.partecipantiIds.size} partecipanti"
                Text(text = sottotitolo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = MaterialTheme.colorScheme.background, shape = MaterialTheme.shapes.small) {
                Text(text = "${uscita.kmTotali} km", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(8.dp, 4.dp), color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}