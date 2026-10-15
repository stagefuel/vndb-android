package dev.stagefuel.vndb.ui.common

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import dev.stagefuel.vndb.api.Bbcode
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.Skin
import dev.stagefuel.vndb.ui.theme.VndbType

/**
 * VNDB BBCode as Compose text. Matches the site: [b] is the standout colour (not bold), spoilers are
 * black-on-black until tapped, links use --link. [onLink] gets absolute URLs (vndb.org ids expanded).
 */
@Composable
fun BbText(src: String?, modifier: Modifier = Modifier, style: TextStyle = VndbType.body, showSpoilers: Boolean = false, onLink: (String) -> Unit) {
  val skin = LocalSkin.current
  var revealed by remember(src) { mutableStateOf(showSpoilers) }
  val text = remember(src, revealed, skin) { annotate(src, skin, revealed, onLink) { revealed = true } }
  BasicText(text, modifier = modifier, style = style.copy(color = skin.mainText))
}

private fun annotate(src: String?, s: Skin, revealed: Boolean, onLink: (String) -> Unit, reveal: () -> Unit): AnnotatedString =
  buildAnnotatedString {
    for (span in Bbcode.parse(src)) {
      val hidden = span.spoiler && !revealed
      val style = SpanStyle(
        color = when {
          hidden -> androidx.compose.ui.graphics.Color.Black
          span.href != null -> s.link
          span.bold -> s.standout
          span.quote -> s.grayedOut
          else -> s.mainText
        },
        background = if (hidden) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Unspecified,
        fontStyle = if (span.italic) FontStyle.Italic else null,
        fontFamily = if (span.code) FontFamily.Monospace else null,
        textDecoration = listOfNotNull(
          if (span.underline) TextDecoration.Underline else null,
          if (span.strike) TextDecoration.LineThrough else null,
        ).takeIf { it.isNotEmpty() }?.let(TextDecoration::combine),
      )
      when {
        hidden -> withLink(LinkAnnotation.Clickable("spoiler", TextLinkStyles(style)) { reveal() }) { append(span.text) }
        span.href != null -> withLink(LinkAnnotation.Clickable(span.href, TextLinkStyles(style)) { onLink(span.href) }) { append(span.text) }
        else -> withStyle(style) { append(span.text) }
      }
    }
  }
