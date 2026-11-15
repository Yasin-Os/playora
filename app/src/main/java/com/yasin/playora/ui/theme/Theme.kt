package com.yasin.playora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = PlayoraPurple,
    secondary = PlayoraTeal,
    tertiary = PlayoraPink
)

private val DarkColors = darkColorScheme(
    primary = PlayoraPurpleLight,
    secondary = PlayoraTealLight,
    tertiary = PlayoraPinkLight
)

@Composable
fun PlayoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
