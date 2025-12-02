package it.col.mar.android.carkarma.ui.theme

import androidx.compose.ui.graphics.Color

// --- COLORI BASATI SULLA TUA ICONA (Ciano -> Blu) ---

// Light Theme Colors (Per sfondo chiaro)
val BluePrimaryLight = Color(0xFF007AFF)      // Il blu elettrico della parte bassa dell'icona
val OnBluePrimaryLight = Color(0xFFFFFFFF)    // Testo bianco su bottone blu
val BlueContainerLight = Color(0xFFD1E4FF)    // Azzurrino chiaro per sfondi di box/card
val OnBlueContainerLight = Color(0xFF001D36)  // Testo scuro su azzurrino

val TealSecondaryLight = Color(0xFF00A2C7)    // Un turchese per elementi secondari (richiama la parte alta)
val OnTealSecondaryLight = Color(0xFFFFFFFF)
val TealContainerLight = Color(0xFF86F0FF)
val OnTealContainerLight = Color(0xFF001F29)

// Dark Theme Colors (Per sfondo scuro - Modalità Notte)
// In Dark Mode usiamo colori più luminosi/pastello per fare contrasto col nero
val CyanPrimaryDark = Color(0xFF40C4FF)       // Ciano luminoso (parte alta icona) per testi importanti
val OnCyanPrimaryDark = Color(0xFF00344F)     // Testo scuro sul bottone ciano (leggibilità massima)
val BlueContainerDark = Color(0xFF004B7A)     // Blu medio per i box
val OnBlueContainerDark = Color(0xFFD1E4FF)   // Testo chiaro nei box

val TealSecondaryDark = Color(0xFF4DD0E1)     // Turchese chiaro
val OnTealSecondaryDark = Color(0xFF003642)
val TealContainerDark = Color(0xFF004F5E)
val OnTealContainerDark = Color(0xFFA6EEFF)

// Colori di Errore (Rosso standard ma armonizzato)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)

// Sfondi Neutri (Bianchi/Grigi/Neri)
val BackgroundLight = Color(0xFFF8FDFF)       // Bianco ghiaccio leggerissimo
val OnBackgroundLight = Color(0xFF001F25)     // Quasi nero
val SurfaceLight = Color(0xFFF8FDFF)
val OnSurfaceLight = Color(0xFF001F25)

val BackgroundDark = Color(0xFF111418)        // Grigio molto scuro (quasi nero)
val OnBackgroundDark = Color(0xFFE6FAFF)      // Bianco sporco
val SurfaceDark = Color(0xFF111418)
val OnSurfaceDark = Color(0xFFE6FAFF)