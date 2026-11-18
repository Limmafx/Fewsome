package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemePreference

data class SemanticColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val errorContainer: Color
)

val LocalSemanticColors = staticCompositionLocalOf {
    SemanticColors(
        success = SuccessGreenLight,
        successContainer = SuccessGreenContainerLight,
        warning = WarningAmberLight,
        warningContainer = WarningAmberContainerLight,
        error = ErrorRedLight,
        errorContainer = ErrorRedContainerLight
    )
}

private val LightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = OnCobaltPrimary,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = TerracottaSecondary,
    onSecondary = OnTerracottaSecondary,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = GoldTertiary,
    onTertiary = OnGoldTertiary,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = IvoryBackground,
    onBackground = InkOnBackground,
    surface = CrispSurface,
    onSurface = InkOnBackground,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = SlateOnSurfaceVariant,
    outline = WarmOutline,
    error = ErrorRedLight,
    onError = Color.White,
    errorContainer = ErrorRedContainerLight,
    onErrorContainer = OnTerracottaContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyCobaltDark,
    onPrimary = OnSkyCobaltDark,
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = OnCobaltContainerDark,
    secondary = CoralDark,
    onSecondary = OnCoralDark,
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = OnCoralContainerDark,
    tertiary = SoftGoldDark,
    onTertiary = OnSoftGoldDark,
    tertiaryContainer = GoldContainerDark,
    onTertiaryContainer = OnGoldContainerDark,
    background = MidnightBackground,
    onBackground = CrispOnDark,
    surface = SlateSurfaceDark,
    onSurface = CrispOnDark,
    surfaceVariant = ElevatedSurfaceVariantDark,
    onSurfaceVariant = MutedOnSurfaceDark,
    outline = SlateOutlineDark,
    error = ErrorRedDark,
    onError = OnCoralDark,
    errorContainer = ErrorRedContainerDark,
    onErrorContainer = OnCoralContainerDark
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun FrenchNumbersTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val semantics = if (useDark) {
        SemanticColors(
            success = SuccessGreenDark,
            successContainer = SuccessGreenContainerDark,
            warning = WarningAmberDark,
            warningContainer = WarningAmberContainerDark,
            error = ErrorRedDark,
            errorContainer = ErrorRedContainerDark
        )
    } else {
        SemanticColors(
            success = SuccessGreenLight,
            successContainer = SuccessGreenContainerLight,
            warning = WarningAmberLight,
            warningContainer = WarningAmberContainerLight,
            error = ErrorRedLight,
            errorContainer = ErrorRedContainerLight
        )
    }

    CompositionLocalProvider(LocalSemanticColors provides semantics) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
