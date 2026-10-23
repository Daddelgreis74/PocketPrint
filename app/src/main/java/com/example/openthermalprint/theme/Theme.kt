package com.example.openthermalprint.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.openthermalprint.ui.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// Nordic Paper Theme (Warm, Organic, Stationery)
private val NordicColorScheme = lightColorScheme(
    primary = NordicPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE7DF),
    onPrimaryContainer = Color(0xFF1E3823),
    secondary = NordicSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7D6D7),
    onSecondaryContainer = Color(0xFF5E1B1C),
    tertiary = NordicTertiary,
    background = NordicBackground,
    onBackground = NordicText,
    surface = NordicSurface,
    onSurface = NordicText,
    surfaceVariant = NordicSurfaceVariant,
    onSurfaceVariant = Color(0xFF49454F)
)

// Aurora Glass Theme (Midnight, Neon Cyan, Lavender Glow)
private val AuroraColorScheme = darkColorScheme(
    primary = AuroraPrimary,
    onPrimary = Color(0xFF03045E),
    primaryContainer = Color(0xFF0077B6),
    onPrimaryContainer = Color.White,
    secondary = AuroraSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5A189A),
    onSecondaryContainer = Color.White,
    tertiary = AuroraTertiary,
    background = AuroraBackground,
    onBackground = AuroraText,
    surface = AuroraSurface,
    onSurface = AuroraText,
    surfaceVariant = AuroraSurfaceVariant,
    onSurfaceVariant = Color(0xFFE0E1DD)
)

@Composable
fun OpenThermalPrintTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val colorScheme: ColorScheme = when (themeMode) {
        ThemeMode.NORDIC -> NordicColorScheme
        ThemeMode.AURORA -> AuroraColorScheme
        ThemeMode.LIGHT -> LightColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemInDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (systemInDark) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
