package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MukulColorScheme = darkColorScheme(
    primary = MukulRedPrimary,
    onPrimary = Color.White,
    primaryContainer = MukulRedDark,
    onPrimaryContainer = Color.White,
    secondary = MukulRedGlowing,
    onSecondary = Color.White,
    tertiary = MukulGold,
    background = MukulDarkBg,
    onBackground = MukulTextPrimary,
    surface = MukulCardBg,
    onSurface = MukulTextPrimary,
    surfaceVariant = MukulSurfaceVariant,
    onSurfaceVariant = MukulTextSecondary,
    outline = MukulCardBorder,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = MukulColorScheme,
        typography = Typography,
        content = content
    )
}
