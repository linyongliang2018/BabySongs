package com.swqsv.babysongs.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 柔和薄荷青绿主色；背景与容器带统一冷绿倾向，避免「白 + 冷灰」割裂。 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF3A6F62),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4EDE6),
    onPrimaryContainer = Color(0xFF06231C),
    secondary = Color(0xFF5C6462),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E6E4),
    onSecondaryContainer = Color(0xFF1B1F1E),
    tertiary = Color(0xFF5C6462),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1E6E4),
    onTertiaryContainer = Color(0xFF1B1F1E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFE8F1EE),
    onBackground = Color(0xFF1A1C1C),
    surface = Color(0xFFF7FBFA),
    onSurface = Color(0xFF1A1C1C),
    surfaceVariant = Color(0xFFCFE3DD),
    onSurfaceVariant = Color(0xFF2D3E3A),
    outline = Color(0xFF6F8A82),
    outlineVariant = Color(0xFFB4C9C2),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFDFEFE),
    surfaceContainerLow = Color(0xFFEEF6F4),
    surfaceContainer = Color(0xFFE2ECE9),
    surfaceContainerHigh = Color(0xFFD4E5E0),
    surfaceContainerHighest = Color(0xFFC2D8D1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ECFBE),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF1D4A40),
    onPrimaryContainer = Color(0xFFE5F5F0),
    secondary = Color(0xFFBFC7C4),
    onSecondary = Color(0xFF2A3230),
    secondaryContainer = Color(0xFF3A4240),
    onSecondaryContainer = Color(0xFFE1E6E4),
    tertiary = Color(0xFFBFC7C4),
    onTertiary = Color(0xFF2A3230),
    tertiaryContainer = Color(0xFF3A4240),
    onTertiaryContainer = Color(0xFFE1E6E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121414),
    onBackground = Color(0xFFE2E5E3),
    surface = Color(0xFF121414),
    onSurface = Color(0xFFE2E5E3),
    surfaceVariant = Color(0xFF3F4846),
    onSurfaceVariant = Color(0xFFBFC9C5),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4846),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF0D0F0E),
    surfaceContainerLow = Color(0xFF1A1C1C),
    surfaceContainer = Color(0xFF1E2120),
    surfaceContainerHigh = Color(0xFF282B2A),
    surfaceContainerHighest = Color(0xFF333635),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val AppTypography = Typography(
    titleLarge = Typography().titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    ),
    titleMedium = Typography().titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
    ),
    labelSmall = Typography().labelSmall.copy(
        letterSpacing = 0.2.sp,
    ),
)

@Composable
fun BabySongsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
