package com.openfilament.cfs.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Dark = darkColorScheme(
    primary = Color(0xFF7CFFB2), onPrimary = Color(0xFF002111),
    secondary = Color(0xFF65D9FF), tertiary = Color(0xFFFFC46B),
    background = Color(0xFF0B0E13), surface = Color(0xFF121720),
    surfaceVariant = Color(0xFF1A2230), onBackground = Color(0xFFF4F7FA), onSurface = Color(0xFFF4F7FA)
)
private val Light = lightColorScheme(
    primary = Color(0xFF006D45), onPrimary = Color.White,
    secondary = Color(0xFF006780), tertiary = Color(0xFF855300),
    background = Color(0xFFF7FAF8), surface = Color.White,
    surfaceVariant = Color(0xFFE6EEE9), onBackground = Color(0xFF101411), onSurface = Color(0xFF101411)
)

@Composable fun OpenFilamentTheme(content: @Composable () -> Unit) {
    val dynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = LocalContext.current
    val scheme = when {
        dynamic && isSystemInDarkTheme() -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        isSystemInDarkTheme() -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
