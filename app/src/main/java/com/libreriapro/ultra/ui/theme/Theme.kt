package com.libreriapro.ultra.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palette of the original "MYC Librería" design, kept identical so the visual
// identity of the app does not change.
val Bg = Color(0xFF080B19)
val Card = Color(0xFF11172A)
val CardElevated = Color(0xFF171E34)
val NavBg = Color(0xFF0D1223)
val Purple = Color(0xFF7B3FF2)
val Blue = Color(0xFF14B8FF)
val Green = Color(0xFF27D17F)
val Red = Color(0xFFFF5267)
val Amber = Color(0xFFFFC24B)
val Soft = Color(0xFFAAB3CB)

private val LibreriaColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = Purple,
    onPrimaryContainer = Color.White,
    secondary = Blue,
    onSecondary = Color.White,
    tertiary = Green,
    background = Bg,
    onBackground = Color.White,
    surface = Card,
    onSurface = Color.White,
    surfaceVariant = CardElevated,
    onSurfaceVariant = Soft,
    surfaceContainer = Card,
    surfaceContainerHigh = CardElevated,
    outline = Color(0xFF2B3550),
    error = Red,
    onError = Color.White,
)

private val LibreriaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun LibreriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LibreriaColors,
        shapes = LibreriaShapes,
        content = content,
    )
}
