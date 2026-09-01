package com.frange.kmpacestream.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = AlwaysBlack,
    surface = AlwaysDarkGray,
    onPrimary = AlwaysWhite,
    onBackground = AlwaysWhite,
    onSurface = AlwaysWhite
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    secondary = SecondaryColor,
    background = AlwaysWhite,
    surface = AlwaysWhite,
    onPrimary = AlwaysBlack,
    onBackground = AlwaysBlack,
    onSurface = AlwaysBlack
)

@Composable
fun CoasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}