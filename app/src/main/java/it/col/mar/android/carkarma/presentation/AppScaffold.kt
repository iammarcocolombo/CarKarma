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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
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
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Determiniamo se mostrare la TopBar e il Drawer (nascondiamoli nel Login)
    val navBackStackEntry = navController?.currentBackStackEntryAsState()?.value
    val currentRoute = navBackStackEntry?.destination?.route
    val showBars = currentRoute != "login"

    // TRUCCO PER IL DRAWER A DESTRA (END DRAWER)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = showBars, // Disabilita swipe nel login
            drawerContent = {
                // Ripristiniamo direzione LTR per il contenuto del menu
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
                                    text = "Profilo Google",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            // --- LOGOUT ---
                            NavigationDrawerItem(
                                label = { Text("Esci dall'account") },
                                selected = false,
                                icon = { Icon(Icons.Default.ExitToApp, null) },
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        onSignOut()
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            },
            content = {
                // Ripristiniamo LTR per il contenuto dell'app
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
}