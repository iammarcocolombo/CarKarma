package it.col.mar.android.carkarma.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object AvatarProvider {
    // Lista di icone disponibili come "Avatar" per i gruppi
    val avatars: List<ImageVector> = listOf(
        Icons.Default.Groups,          // 0: Default (Gruppo generico)
        Icons.Default.DirectionsCar,   // 1: Auto
        Icons.Default.TwoWheeler,      // 2: Moto
        Icons.Default.PedalBike,       // 3: Bici
        Icons.Default.Flight,          // 4: Aereo (Viaggi lunghi)
        Icons.Default.Pets,            // 5: Animali
        Icons.Default.SportsSoccer,    // 6: Calcio/Sport
        Icons.Default.MusicNote,       // 7: Concerti/Musica
        Icons.Default.LocalPizza,      // 8: Pizzata
        Icons.Default.Restaurant,      // 9: Ristorante
        Icons.Default.BeachAccess,     // 10: Mare
        Icons.Default.Hiking,          // 11: Montagna
        Icons.Default.Work,            // 12: Lavoro
        Icons.Default.School,          // 13: Università/Scuola
        Icons.Default.VideogameAsset   // 14: Gaming
    )

    // Funzione helper per recuperare l'icona sicura (se l'indice è fuori range, torna la default)
    fun getAvatar(index: Int): ImageVector {
        return if (index in avatars.indices) avatars[index] else avatars[0]
    }
}