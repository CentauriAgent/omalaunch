package me.derekross.omalaunch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Omarchy/Tokyo-Night flavored dark palette.
val Bg = Color(0xFF1A1B26)
val Panel = Color(0xCC16161E)
val PanelField = Color(0xFF1F2335)
val PanelStroke = Color(0x337AA2F7)
val TextMain = Color(0xFFC0CAF5)
val TextDim = Color(0xFF565F89)
val Accent = Color(0xFF7AA2F7)
val Green = Color(0xFF9ECE6A)
val Yellow = Color(0xFFE0AF68)
val Red = Color(0xFFF7768E)

private val OmalaunchScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    secondary = Green,
    onSecondary = Bg,
    tertiary = Yellow,
    background = Bg,
    onBackground = TextMain,
    surface = PanelField,
    onSurface = TextMain,
)

@Composable
fun OmalaunchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OmalaunchScheme,
        content = content,
    )
}
