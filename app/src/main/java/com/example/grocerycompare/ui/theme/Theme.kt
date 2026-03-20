package com.example.grocerycompare.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary          = FreshGreen,
    secondary        = ColesRed,
    tertiary         = WooliesGreen,
    background       = Color(0xFF0F172A),
    surface          = Color(0xFF1E293B),
    surfaceVariant   = Color(0xFF243347),
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onTertiary       = Color.White,
    onBackground     = Color.White,
    onSurface        = Color.White,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline          = Color(0xFF475569),
    outlineVariant   = Color(0xFF334155),
    error            = ColesRed,
)

private val LightColorScheme = lightColorScheme(
    primary          = FreshGreen,
    secondary        = ColesRed,
    tertiary         = WooliesGreen,
    background       = CleanSlate,
    surface          = PureWhite,
    surfaceVariant   = Color(0xFFF1F5F9),
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onTertiary       = Color.White,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = InactiveSlate,
    outline          = Color(0xFFCBD5E1),
    outlineVariant   = Color(0xFFE2E8F0),
    error            = ColesRed,
)

@Composable
fun GroceryCompareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = !darkTheme

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content,
    )
}
