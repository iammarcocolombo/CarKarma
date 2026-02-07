package it.col.mar.android.carkarma.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import it.col.mar.android.carkarma.R

@Composable
fun SplashScreen(
    isDataReady: Boolean,
    onAnimationFinished: () -> Unit
) {
    val scale = remember { Animatable(0.5f) }
    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // 1. INGRESSO (Più veloce: 800ms invece di 1700ms)
        launch {
            alpha.animateTo(1f, tween(1000))
        }
        launch {
            scale.animateTo(1.2f, tween(800, easing = LinearOutSlowInEasing))
        }
        launch {
            // Rotazione continua che dura un po' di più per coprire l'attesa
            rotation.animateTo(360f * 2, tween(1500, easing = LinearOutSlowInEasing))
        }

        // FONDAMENTALE: Aspettiamo che l'ingresso sia visibile prima di controllare i dati
        // Altrimenti se i dati sono già pronti, l'uscita parte sopra l'ingresso e non si vede nulla.
        delay(1000)

        // 2. ATTESA DATI (Max 2 secondi extra)
        var timeElapsed = 0L
        val maxWaitTime = 2000L
        val checkInterval = 100L

        while (!isDataReady && timeElapsed < maxWaitTime) {
            delay(checkInterval)
            timeElapsed += checkInterval
        }

        // 3. USCITA (Esplosione che parte piano e accelera)
        launch {
            // Curva personalizzata: parte lento e accelera progressivamente (EaseIn)
            scale.animateTo(50f, tween(800, easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)))
        }
        launch {
            alpha.animateTo(0f, tween(350, easing = CubicBezierEasing(0.7f, 0f, 1f, 1f)))
        }

        delay(500)
        onAnimationFinished()
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.carkarma_icon),
            contentDescription = "Logo CarKarma",
            modifier = Modifier
                .size(150.dp)
                .scale(scale.value)
                .rotate(rotation.value)
                .alpha(alpha.value)
        )
    }
}
