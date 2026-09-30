package com.example.gpgrocery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalCrateColors = staticCompositionLocalOf { LightCrateColors }

val CrateShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun CrateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkCrateColors else LightCrateColors
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = CrateTypography,
        shapes = CrateShapes,
    ) {
        CompositionLocalProvider(LocalCrateColors provides colors, content = content)
    }
}

/** The Crate colours for the current theme: `Crate.colors.primary`. */
object Crate {
    val colors: CrateColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCrateColors.current
}
