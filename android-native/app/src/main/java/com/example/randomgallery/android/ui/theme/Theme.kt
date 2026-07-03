package com.example.randomgallery.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

@Immutable
data class XhsExtendedColors(
    val textTertiary: Color,
    val topicLink: Color,
    val accentIndigo: Color, val accentIndigoSoft: Color,
    val accentCoral: Color, val accentCoralSoft: Color,
    val accentBlue: Color, val accentBlueSoft: Color,
    val accentGreen: Color, val accentGreenSoft: Color,
    val accentOrange: Color, val accentOrangeSoft: Color,
    val accentGreySoft: Color
)

private val LightExtendedColors = XhsExtendedColors(
    textTertiary = TextTertiary,
    topicLink = TopicLinkBlue,
    accentIndigo = AccentIndigo, accentIndigoSoft = AccentIndigoSoft,
    accentCoral = AccentCoral, accentCoralSoft = AccentCoralSoft,
    accentBlue = AccentBlue, accentBlueSoft = AccentBlueSoft,
    accentGreen = AccentGreen, accentGreenSoft = AccentGreenSoft,
    accentOrange = AccentOrange, accentOrangeSoft = AccentOrangeSoft,
    accentGreySoft = AccentGreySoft
)

private val DarkExtendedColors = XhsExtendedColors(
    textTertiary = DarkTextTertiary,
    topicLink = TopicLinkBlueDark,
    accentIndigo = AccentIndigo, accentIndigoSoft = AccentIndigoSoftDark,
    accentCoral = AccentCoral, accentCoralSoft = AccentCoralSoftDark,
    accentBlue = AccentBlue, accentBlueSoft = AccentBlueSoftDark,
    accentGreen = AccentGreen, accentGreenSoft = AccentGreenSoftDark,
    accentOrange = AccentOrange, accentOrangeSoft = AccentOrangeSoftDark,
    accentGreySoft = AccentGreySoftDark
)

val LocalXhsColors = staticCompositionLocalOf { LightExtendedColors }

/** 用法：MaterialTheme.xhs.accentBlue */
@Suppress("UnusedReceiverParameter")
val androidx.compose.material3.MaterialTheme.xhs: XhsExtendedColors
    @Composable get() = LocalXhsColors.current

@Composable
fun RandomGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalXhsColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
