package com.hisham.todolist.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hisham.todolist.domain.model.ThemeMode

private val ObsidianDarkColors = darkColorScheme(
    primary = Color(0xFFBF9DFF),
    onPrimary = Color(0xFF200A44),
    secondary = Color(0xFF00D2FD),
    tertiary = Color(0xFFFF6A9F),
    background = Color(0xFF0D0E15),
    onBackground = Color(0xFFF5F2FD),
    surface = Color(0xFF0D0E15),
    onSurface = Color(0xFFF5F2FD),
    surfaceVariant = Color(0xFF252530),
    onSurfaceVariant = Color(0xFFACAAB4),
)

private val ObsidianLightColors = lightColorScheme(
    primary = Color(0xFF6E42D2),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF006D86),
    tertiary = Color(0xFFA92F61),
    background = Color(0xFFF6F4FB),
    onBackground = Color(0xFF17171F),
    surface = Color(0xFFF6F4FB),
    onSurface = Color(0xFF17171F),
    surfaceVariant = Color(0xFFE7E1F2),
    onSurfaceVariant = Color(0xFF4C4758),
)

@Composable
fun TodoListTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colorScheme = if (useDarkTheme) ObsidianDarkColors else ObsidianLightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
