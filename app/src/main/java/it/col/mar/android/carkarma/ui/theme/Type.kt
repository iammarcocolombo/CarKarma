package it.col.mar.android.carkarma.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import it.col.mar.android.carkarma.R

val RobotoCondensed = FontFamily(
    Font(R.font.robotocondensed_regular, FontWeight.Normal),
    Font(R.font.robotocondensed_medium, FontWeight.Medium),
    Font(R.font.robotocondensed_light, FontWeight.Light),
    Font(R.font.robotocondensed_bold, FontWeight.Bold)
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RobotoCondensed,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)
