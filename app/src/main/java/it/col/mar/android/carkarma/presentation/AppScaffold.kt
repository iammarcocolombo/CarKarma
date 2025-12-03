package it.col.mar.android.carkarma.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import it.col.mar.android.carkarma.R
import it.col.mar.android.carkarma.data.database.AppContainer
import it.col.mar.android.carkarma.ui.theme.CarKarmaTheme
import it.col.mar.android.carkarma.util.UserData
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    navController: NavController? = null,
    title: String = stringResource(R.string.app_name),
    userData: UserData? = null,
    onSignOut: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}, // Nuova callback per eliminazione
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Determiniamo se mostrare le barre di navigazione (nascoste nel login)
    val navBackStackEntry = navController?.currentBackStackEntryAsState()?.value
    val currentRoute = navBackStackEntry?.destination?.route
    // Nascondiamo barre se siamo nel login o se la rotta non è ancora definita
    val showBars = currentRoute != "login" && currentRoute != null

    // TRUCCO PER IL DRAWER A DESTRA (END DRAWER)
    // Invertiamo il layout del contenitore del drawer per farlo aprire da destra
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showBars, // Disabilita lo swipe laterale nella schermata di login
            drawerContent = {
                // Ripristiniamo la direzione LTR (sinistra -> destra) per il contenuto del menu
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

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

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
                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            // TASTO ELIMINA ACCOUNT (Rosso)
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
                // Ripristiniamo la direzione LTR per il contenuto principale dell'app
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Scaffold(
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
                                        // Icona Profilo a DESTRA che apre il Drawer
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
                        content(paddingValues)
                    }
                }
            }
        )
    }

    // Dialog di conferma eliminazione account
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Eliminare l'account?") },
            text = { Text("Questa azione è irreversibile. I tuoi dati personali verranno rimossi, ma i dati nei gruppi condivisi potrebbero rimanere visibili agli altri membri per mantenere lo storico.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
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