package com.marvyn.fintrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandGreen = Color(0xFF2E7D32)
val IncomeGreen = Color(0xFF2E7D32)
val ExpenseRed = Color(0xFFC62828)

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    secondary = Color(0xFF00695C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFF4DB6AC)
)

@Composable
fun FinTrackTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
