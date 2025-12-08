package it.col.mar.android.carkarma.presentation.info

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class CreditItem(val text: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(navController: NavController) {
    val context = LocalContext.current

    // Lista delle attribuzioni richieste da Flaticon
    val creditsList = listOf(
        CreditItem("Team sport icons created by Freepik", "https://www.flaticon.com/free-icons/team-sport"),
        CreditItem("Beer icons created by dDara", "https://www.flaticon.com/free-icons/beer"),
        CreditItem("Motorcycle icons created by Chattapat", "https://www.flaticon.com/free-icons/motorcycle"),
        CreditItem("Cars icons created by BZZRINCANTATION", "https://www.flaticon.com/free-icons/cars"),
        CreditItem("Gaming icons created by Freepik", "https://www.flaticon.com/free-icons/gaming"),
        CreditItem("Pizza icons created by Freepik", "https://www.flaticon.com/free-icons/pizza"),
        CreditItem("Education icons created by Freepik", "https://www.flaticon.com/free-icons/education"),
        CreditItem("Paw icons created by Freepik", "https://www.flaticon.com/free-icons/paw"),
        CreditItem("Mountain icons created by Freepik", "https://www.flaticon.com/free-icons/mountain"),
        CreditItem("Restaurant icons created by Freepik", "https://www.flaticon.com/free-icons/restaurant"),
        CreditItem("Coast icons created by Freepik", "https://www.flaticon.com/free-icons/coast"),
        CreditItem("Work icons created by Freepik", "https://www.flaticon.com/free-icons/work"),
        CreditItem("Travel icons created by Freepik", "https://www.flaticon.com/free-icons/travel"),
        CreditItem("Music icons created by Freepik", "https://www.flaticon.com/free-icons/music")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crediti e Licenze") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Sviluppo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "App sviluppata da Marco Colombo",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Divider(modifier = Modifier.padding(vertical = 16.dp))

                Text(
                    text = "Attribuzione Icone (Flaticon)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(creditsList) { credit ->
                Text(
                    text = credit.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(credit.url))
                            context.startActivity(intent)
                        }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Tutte le icone sono fornite da Flaticon sotto licenza gratuita con attribuzione.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}