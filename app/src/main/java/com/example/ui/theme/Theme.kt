package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NewFarmLightColorScheme = lightColorScheme(
    primary = DeepNavyBlue,
    onPrimary = Color.White,
    primaryContainer = DeepNavyContainer,
    onPrimaryContainer = OnDeepNavyContainer,
    secondary = FreshTeal,
    onSecondary = Color.White,
    secondaryContainer = FreshTealContainer,
    onSecondaryContainer = OnFreshTealContainer,
    tertiary = SubtleOrange,
    onTertiary = Color.White,
    tertiaryContainer = SubtleOrangeContainer,
    onTertiaryContainer = OnSubtleOrangeContainer,
    background = WarmWhiteBg,
    onBackground = SlateTextPrimary,
    surface = PureCardWhite,
    onSurface = SlateTextPrimary,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = SlateTextSecondary,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = AlertCrimson
)

@Composable
fun NewFarmProductTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NewFarmLightColorScheme,
        typography = Typography,
        content = content
    )
}
