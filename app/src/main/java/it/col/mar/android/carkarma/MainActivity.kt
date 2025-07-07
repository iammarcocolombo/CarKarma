package it.col.mar.android.carkarma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.col.mar.android.carkarma.presentation.CarKarmaApp
import it.col.mar.android.carkarma.ui.theme.CarKarmaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CarKarmaTheme {
                CarKarmaApp()
            }
        }
    }
}
