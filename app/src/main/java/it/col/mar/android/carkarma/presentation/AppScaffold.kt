package it.col.mar.android.carkarma.presentation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import it.col.mar.android.carkarma.R
import it.col.mar.android.carkarma.data.model.UserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController? = null,
    title: String = stringResource(R.string.app_name),
    userData: UserData? = null,
    onSignOut: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val navBackStackEntry = navController?.currentBackStackEntryAsState()?.value
    val currentRoute = navBackStackEntry?.destination?.route
    val showBars = currentRoute != "login" && currentRoute != null && currentRoute != "splash"

    // --- GESTIONE TASTO INDIETRO (BACK HANDLER) ---
    var backPressedOnce by remember { mutableStateOf(false) }

    // Si attiva SOLO se il drawer è aperto OPPURE se siamo nella Home
    BackHandler(enabled = drawerState.isOpen || currentRoute == "home") {
        if (drawerState.isOpen) {
            // Se il drawer è aperto, il tasto indietro lo chiude semplicemente
            scope.launch { drawerState.close() }
        } else if (currentRoute == "home") {
            // Se siamo in home e il drawer è chiuso, gestiamo il doppio tap per uscire
            if (backPressedOnce) {
                (context as? Activity)?.finish() // Chiude l'app
            } else {
                backPressedOnce = true
                Toast.makeText(context, "Premi di nuovo per uscire", Toast.LENGTH_SHORT).show()
                // Avvia un timer: se non premi di nuovo entro 2 secondi, resetta lo stato
                scope.launch {
                    delay(2000)
                    backPressedOnce = false
                }
            }
        }
    }

    // TRUCCO PER IL DRAWER A DESTRA (END DRAWER)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showBars,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    if (showBars) {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(24.dp))

                            // --- HEADER PROFILO ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                if (userData?.username != null) {
                                    Text(
                                        text = userData.username,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = userData?.email ?: "Profilo Google",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            // --- MENU ITEMS ---
                            NavigationDrawerItem(
                                label = { Text("Privacy Policy") },
                                selected = false,
                                icon = { Icon(Icons.Default.PrivacyTip, null) },
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        navController?.navigate("privacy")
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                label = { Text("Crediti e Licenze") },
                                selected = false,
                                icon = { Icon(Icons.Default.Info, null) },
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        navController?.navigate("credits")
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                label = { Text("Esci dall'account") },
                                selected = false,
                                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        onSignOut()
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            Spacer(modifier = Modifier.weight(1f))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            NavigationDrawerItem(
                                label = { Text("Elimina Account", color = MaterialTheme.colorScheme.error) },
                                selected = false,
                                icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        showDeleteConfirmDialog = true
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            },
            content = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        topBar = {
                            if (showBars) {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = title,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    actions = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = "Profilo Utente",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            }
                        }
                    ) { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues)) {
                            content(paddingValues)
                        }
                    }
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminare l'account?") },
            text = { Text("Questa azione è irreversibile. I tuoi dati personali verranno rimossi, ma i dati nei gruppi condivisi potrebbero rimanere visibili agli altri membri per mantenere lo storico.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        // FIX: Attendiamo che l'animazione di chiusura del Dialog sia
                        // completata prima di scatenare l'evento pesante di eliminazione
                        // che resetterà il NavHost. Evita che il Dialog rimanga bloccato a schermo!
                        scope.launch {
                            delay(300)
                            onDeleteAccount()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina Definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Annulla") }
            },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) }
        )
    }
}