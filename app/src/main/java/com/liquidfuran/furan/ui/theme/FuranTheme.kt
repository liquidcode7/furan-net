package com.liquidfuran.furan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FuranColorScheme = darkColorScheme(
    primary = FuranColors.Cyan,
    onPrimary = FuranColors.Background,
    secondary = FuranColors.Violet,
    onSecondary = FuranColors.White,
    tertiary = FuranColors.Magenta,
    background = FuranColors.Background,
    surface = FuranColors.Navy,
    onBackground = FuranColors.White,
    onSurface = FuranColors.White,
    error = FuranColors.Magenta,
    onError = FuranColors.Background
)

@Composable
fun FuranTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FuranColorScheme,
        content = content
    )
}
