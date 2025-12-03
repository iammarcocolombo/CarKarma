package it.col.mar.android.carkarma.presentation.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Informativa sulla Privacy di CarKarma",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            SpacerText()

            SectionTitle("1. Dati Raccolti")
            BodyText("L'App raccoglie nome, email e foto profilo tramite Google Sign-In per identificare l'utente e permettere la creazione di gruppi. Raccoglie inoltre i dati inseriti relativi ai viaggi (indirizzi, km) e ai partecipanti.")

            SpacerText()

            SectionTitle("2. Servizi di Terze Parti")
            BodyText("Utilizziamo Google Firebase per l'autenticazione e il database. Utilizziamo OpenRouteService per il calcolo dei percorsi. I dati degli indirizzi vengono inviati a questi servizi per fornire le funzionalità richieste.")

            SpacerText()

            SectionTitle("3. Finalità")
            BodyText("I dati servono esclusivamente per il funzionamento dell'app: gestione gruppi, calcolo equo dei guidatori e storico viaggi.")

            SpacerText()

            SectionTitle("4. Cancellazione")
            BodyText("Puoi eliminare il tuo account e i tuoi dati personali in qualsiasi momento tramite l'apposita funzione nel menu profilo. I dati nei gruppi condivisi potrebbero rimanere visibili agli altri partecipanti per mantenere la coerenza dello storico.")

            SpacerText()

            SectionTitle("5. Contatti")
            BodyText("Per info: colombomarco2001@gmail.com") // Sostituisci con la tua email se vuoi
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun SpacerText() {
    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
}