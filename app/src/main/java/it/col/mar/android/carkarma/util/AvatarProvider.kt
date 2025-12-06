package it.col.mar.android.carkarma.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import it.col.mar.android.carkarma.R // Assicurati che importi il tuo R

// Classe che rappresenta un Avatar: può essere un'icona di sistema O una risorsa locale (tua)
sealed class CarKarmaAvatar {
    data class System(val imageVector: ImageVector) : CarKarmaAvatar()
    data class Resource(val resId: Int) : CarKarmaAvatar()
}

object AvatarProvider {
    // La lista mista
    val avatars: List<CarKarmaAvatar> = listOf(
        // 0-14: Icone di Sistema (quelle che avevamo già)
//        CarKarmaAvatar.System(Icons.Default.Groups),
//        CarKarmaAvatar.System(Icons.Default.DirectionsCar),
//        CarKarmaAvatar.System(Icons.Default.TwoWheeler),
//        CarKarmaAvatar.System(Icons.Default.PedalBike),
//        CarKarmaAvatar.System(Icons.Default.Flight),
//        CarKarmaAvatar.System(Icons.Default.Pets),
//        CarKarmaAvatar.System(Icons.Default.SportsSoccer),
//        CarKarmaAvatar.System(Icons.Default.MusicNote),
//        CarKarmaAvatar.System(Icons.Default.LocalPizza),
//        CarKarmaAvatar.System(Icons.Default.Restaurant),
//        CarKarmaAvatar.System(Icons.Default.BeachAccess),
//        CarKarmaAvatar.System(Icons.Default.Hiking),
//        CarKarmaAvatar.System(Icons.Default.Work),
//        CarKarmaAvatar.System(Icons.Default.School),
//        CarKarmaAvatar.System(Icons.Default.VideogameAsset),

        // --- LE TUE ICONE PERSONALIZZATE ---
        // Aggiungile qui in fondo alla lista.
        // Esempio: Se hai caricato un file chiamato "mio_avatar.png" in res/drawable:
        // CarKarmaAvatar.Resource(R.drawable.mio_avatar),

        // Esempio con l'icona di Google che hai già caricato (giusto per provare):
        CarKarmaAvatar.Resource(R.drawable.airplane),
        CarKarmaAvatar.Resource(R.drawable.basketball),
        CarKarmaAvatar.Resource(R.drawable.cheers),
        CarKarmaAvatar.Resource(R.drawable.cutlery),
        CarKarmaAvatar.Resource(R.drawable.education),
        CarKarmaAvatar.Resource(R.drawable.mountain),
        CarKarmaAvatar.Resource(R.drawable.paw),
        CarKarmaAvatar.Resource(R.drawable.pizza),
        CarKarmaAvatar.Resource(R.drawable.sandy),
        CarKarmaAvatar.Resource(R.drawable.suitcase),
        CarKarmaAvatar.Resource(R.drawable.motorcycle),
        CarKarmaAvatar.Resource(R.drawable.music),
        CarKarmaAvatar.Resource(R.drawable.console),






    )

    fun getAvatar(index: Int): CarKarmaAvatar {
        return if (index in avatars.indices) avatars[index] else avatars[0]
    }

    // Componente Composable che sa come disegnare l'avatar giusto
    @Composable
    fun DisplayAvatar(
        avatar: CarKarmaAvatar,
        contentDescription: String?,
        tint: Color, // Passiamo il colore desiderato
        modifier: Modifier = Modifier
    ) {
        when (avatar) {
            is CarKarmaAvatar.System -> {
                // Se è di sistema, la coloriamo noi (es. blu)
                Icon(
                    imageVector = avatar.imageVector,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = modifier
                )
            }
            is CarKarmaAvatar.Resource -> {
                // Se è una tua immagine PNG/JPG, la mostriamo coi suoi colori originali (Unspecified)
                // Se invece è un'icona nera trasparente (XML), potresti volerla colorare.
                // Per ora assumiamo siano immagini colorate (tipo foto/disegni) e non applichiamo la tinta.
                Icon(
                    painter = painterResource(id = avatar.resId),
                    contentDescription = contentDescription,
                    tint = Color.Unspecified, // Usa i colori originali dell'immagine
                    modifier = modifier
                )
            }
        }
    }
}