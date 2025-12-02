package it.col.mar.android.carkarma.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Schema Colori Scuro (Dark Mode)
private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimaryDark,          // Tasti principali (Ciano luminoso)
    onPrimary = OnCyanPrimaryDark,      // Testo sui tasti (Blu scuro)
    primaryContainer = BlueContainerDark, // Sfondo Card attive
    onPrimaryContainer = OnBlueContainerDark,

    secondary = TealSecondaryDark,      // Elementi secondari
    onSecondary = OnTealSecondaryDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = OnTealContainerDark,

    tertiary = TealSecondaryDark,       // Usiamo il teal anche come terziario per coerenza

    background = BackgroundDark,        // Sfondo app (Scuro)
    onBackground = OnBackgroundDark,    // Testo su sfondo (Chiaro)

    surface = SurfaceDark,              // Sfondo Card/Dialog
    onSurface = OnSurfaceDark,

    error = ErrorDark,
    onError = OnErrorDark
)

// Schema Colori Chiaro (Light Mode)
private val LightColorScheme = lightColorScheme(
    primary = BluePrimaryLight,         // Tasti principali (Blu Elettrico)
    onPrimary = OnBluePrimaryLight,     // Testo sui tasti (Bianco)
    primaryContainer = BlueContainerLight,
    onPrimaryContainer = OnBlueContainerLight,

    secondary = TealSecondaryLight,
    onSecondary = OnTealSecondaryLight,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = OnTealContainerLight,

    tertiary = TealSecondaryLight,

    background = BackgroundLight,       // Sfondo app (Bianco ghiaccio)
    onBackground = OnBackgroundLight,   // Testo su sfondo (Scuro)

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,

    error = ErrorLight,
    onError = OnErrorLight
)

@Composable
fun CarKarmaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // IMPORTANTE: Mettiamo false per usare I TUOI colori e non quelli dello sfondo del telefono
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Colora la barra di stato (quella con l'orologio) con il colore Primario o di Sfondo
            window.statusBarColor = colorScheme.background.toArgb()
            // Icone scure se il tema è chiaro, icone chiare se il tema è scuro
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}