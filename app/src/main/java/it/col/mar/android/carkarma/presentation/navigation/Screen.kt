package it.col.mar.android.carkarma.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Definisce le schermate dell'app e i percorsi di navigazione.
 * ADATTATO PER FIREBASE: Gli ID sono ora Stringhe.
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {

    // 1. Home
    data object Home : Screen("home", "CarKarma", Icons.Default.Home)

    // 2. Dettaglio Gruppo
    data object Gruppo : Screen("gruppo/{gruppoId}", "Dettaglio Gruppo", Icons.Default.Group) {
        fun createRoute(gruppoId: String) = "gruppo/$gruppoId"
    }

    // 3. Modifica Gruppo
    data object ModificaGruppo : Screen("modificaGruppo?gruppoId={gruppoId}", "Modifica Gruppo", Icons.Default.Edit) {
        fun createRoute(gruppoId: String = "") = "modificaGruppo?gruppoId=$gruppoId"
    }

    // 4. Uscita (Nuova o Esistente)
    data object Uscita : Screen("uscita/{gruppoId}?uscitaId={uscitaId}", "Uscita", Icons.Default.Add) {
        fun createRoute(gruppoId: String, uscitaId: String = ""): String {
            return "uscita/$gruppoId?uscitaId=$uscitaId"
        }
    }

    /// 5. Dettaglio Amico
    data object Amico : Screen("amico?amicoId={amicoId}", "Profilo Amico", Icons.Default.Person) {
        fun createRoute(amicoId: String = "") = "amico?amicoId=$amicoId"
    }

    // 6. NUOVA ROTTA: Dettaglio Karma Incrociato
    data object DettaglioKarma : Screen("dettaglio_karma/{gruppoId}/{componenteId}", "Dettaglio Karma") {
        fun createRoute(gruppoId: String, componenteId: String) = "dettaglio_karma/$gruppoId/$componenteId"
    }
}