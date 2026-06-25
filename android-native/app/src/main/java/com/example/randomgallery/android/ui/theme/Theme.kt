package com.example.randomgallery.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = XhsRed,
    onPrimary = NeutralWhite,
    primaryContainer = XhsRedSoft,
    onPrimaryContainer = XhsRed,
    secondary = IconGrey,
    onSecondary = NeutralWhite,
    background = FeedBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor,
    error = XhsRed,
    onError = NeutralWhite
)

private val DarkColors = darkColorScheme(
    primary = XhsRed,
    onPrimary = NeutralWhite,
    primaryContainer = XhsRedSoftDark,
    onPrimaryContainer = XhsRed,
    secondary = DarkIconGrey,
    onSecondary = DarkTextPrimary,
    background = DarkFeedBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
    outlineVariant = DarkDivider,
    error = XhsRed,
    onError = NeutralWhite
)

@Composable
fun RandomGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
