package com.weatherfocus.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SkyBlue = Color(0xFF2F80ED)
val SkyBlueDark = Color(0xFF1B3A6B)
val SunYellow = Color(0xFFF2C94C)

private val LightColors = lightColorScheme(
    primary = SkyBlue,
    secondary = SunYellow,
    background = Color(0xFFF3F7FC),
    surface = Color.White
)

private val DarkColors = darkColorScheme(
    primary = SkyBlue,
    secondary = SunYellow,
    background = Color(0xFF0E1626),
    surface = Color(0xFF152238)
)

@Composable
fun WeatherOnlyTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
