package dev.stagefuel.vndb.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LocalSkin = staticCompositionLocalOf { Skins.byId(Skins.DEFAULT) }

/** Site type scale: Tahoma 13px body, Futura-ish headings. Android has neither, so sans-serif stands in. */
object VndbType {
  val body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 19.sp)
  val small = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 16.sp)
  val siteTitle = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, fontSize = 20.sp)
  val h1 = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 22.sp, lineHeight = 27.sp)
  val boxH1 = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 17.sp)
  val altTitle = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp)
}

@Composable
fun VndbTheme(skinId: String, content: @Composable () -> Unit) {
  val s = Skins.byId(skinId)
  val base = if (s.isDark) darkColorScheme() else lightColorScheme()
  val scheme = base.copy(
    primary = s.link,
    onPrimary = s.bodyBg,
    secondary = s.standout,
    background = s.bodyBg,
    onBackground = s.mainText,
    surface = s.solidBox,
    onSurface = s.mainText,
    surfaceVariant = s.tabBg,
    onSurfaceVariant = s.grayedOut,
    surfaceContainer = s.solidBox,
    surfaceContainerHigh = s.solidBox,
    surfaceContainerLow = s.solidBox,
    outline = s.border,
    outlineVariant = s.secBorder,
    error = s.statNok,
  )
  val typography = Typography(
    bodyLarge = VndbType.body, bodyMedium = VndbType.body, bodySmall = VndbType.small,
    titleLarge = VndbType.h1, titleMedium = VndbType.boxH1, titleSmall = VndbType.body.copy(fontWeight = FontWeight.Bold),
    labelLarge = VndbType.body, labelMedium = VndbType.small, labelSmall = VndbType.small,
  )
  CompositionLocalProvider(LocalSkin provides s) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
  }
}
