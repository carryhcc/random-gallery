package com.example.randomgallery.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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
    surfaceContainer = SurfaceCard,
    surfaceContainerLow = FeedBackground,
    surfaceContainerHigh = SurfaceMuted,
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
    surfaceContainer = DarkSurface,
    surfaceContainerLow = DarkFeedBackground,
    surfaceContainerHigh = DarkSurfaceMuted,
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
    darkMode: String = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = when (darkMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(LocalXhsColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

