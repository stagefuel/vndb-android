package dev.stagefuel.vndb.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType

/** The site's `<article>`: 1px border, translucent box background, square corners. */
@Composable
fun VndbBox(modifier: Modifier = Modifier, padding: Int = 6, content: @Composable ColumnScope.() -> Unit) {
  val s = LocalSkin.current
  Column(
    modifier
      .fillMaxWidth()
      .border(1.dp, s.border)
      .background(s.boxBg)
      .padding(padding.dp),
    content = content,
  )
}

/** `main h1` / `h2.alttitle` pair. */
@Composable
fun BoxTitle(title: String, alt: String? = null) {
  val s = LocalSkin.current
  Text(title, style = VndbType.h1, color = s.boxTitle)
  if (!alt.isNullOrBlank() && alt != title) Text(alt, style = VndbType.altTitle, color = s.altTitle, modifier = Modifier.padding(start = 12.dp))
}

/** Sidebar-style `h2`: bottom border, box background. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
  val s = LocalSkin.current
  Text(
    text,
    style = VndbType.boxH1,
    color = s.boxTitle,
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 10.dp, bottom = 4.dp)
      .drawBehind { drawLine(s.border, Offset(0f, size.height), Offset(size.width, size.height), 1f) }
      .padding(bottom = 2.dp),
  )
}

/** Item tabs (main / releases / characters …): inactive = tabbg + grey, selected = blendbg + main text. */
/** True when the user wants tab rows pinned to the bottom of the screen. */
val LocalTabsBottom = androidx.compose.runtime.staticCompositionLocalOf { false }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VndbTabs(tabs: List<String>, selected: Int, small: Boolean = false, onSelect: (Int) -> Unit) {
  val s = LocalSkin.current
  val bottom = LocalTabsBottom.current
  // Wraps onto extra lines instead of scrolling sideways, so every tab is always visible.
  FlowRow(
    (if (bottom) Modifier.fillMaxWidth().background(s.bodyBg).drawBehind {
      drawLine(s.border, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
    }.padding(horizontal = 6.dp, vertical = 6.dp) else Modifier.fillMaxWidth().padding(horizontal = 6.dp)),
    horizontalArrangement = Arrangement.spacedBy(if (small) 6.dp else 8.dp),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    tabs.forEachIndexed { i, t ->
      val sel = i == selected
      Box(
        Modifier
          .border(1.dp, s.border)
          .background(if (sel) s.blendBg else s.tabBg)
          .clickable { onSelect(i) }
          .padding(horizontal = if (small) 8.dp else 10.dp, vertical = 5.dp),
      ) { Text(t, style = if (small) VndbType.small else VndbType.body, color = if (sel) s.mainText else s.grayedOut) }
    }
  }
}

/** Two-column zebra table row (`table.stripe`), key column ~96px like the VN info table. */
@Composable
fun InfoRow(key: String, odd: Boolean, keyWidth: Int = 96, value: @Composable () -> Unit) {
  val s = LocalSkin.current
  Row(
    Modifier.fillMaxWidth().background(if (odd) s.boxBg else androidx.compose.ui.graphics.Color.Transparent).padding(vertical = 3.dp, horizontal = 4.dp),
  ) {
    Text(key, style = VndbType.body, color = s.mainText, modifier = Modifier.width(keyWidth.dp))
    Box(Modifier.weight(1f)) { value() }
  }
}

@Composable
fun Grey(text: String, modifier: Modifier = Modifier, style: TextStyle = VndbType.small) =
  Text(text, style = style, color = LocalSkin.current.grayedOut, modifier = modifier)

@Composable
fun Loading(modifier: Modifier = Modifier) {
  Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(color = LocalSkin.current.link)
  }
}

/** `div.warning`. */
@Composable
fun WarningBox(text: String, onRetry: (() -> Unit)? = null) {
  val s = LocalSkin.current
  Column(
    Modifier.fillMaxWidth().padding(12.dp).border(1.dp, s.warnBorder).background(s.warnBg).padding(12.dp),
  ) {
    Text(text, style = VndbType.body, color = s.mainText)
    if (onRetry != null) {
      Spacer(Modifier.height(6.dp))
      Text("retry", style = VndbType.body, color = s.link, modifier = Modifier.clickable(onClick = onRetry))
    }
  }
}

@Composable
fun LinkText(text: String, modifier: Modifier = Modifier, style: TextStyle = VndbType.body, onClick: () -> Unit) =
  Text(text, style = style, color = LocalSkin.current.link, modifier = modifier.clickable(onClick = onClick))

fun formatVote(v: Int?): String = when {
  v == null -> "-"
  v % 10 == 0 -> (v / 10).toString()
  else -> "%.1f".format(v / 10.0)
}

fun formatRating(r: Double?): String = r?.let { "%.2f".format(it / 10.0) } ?: "-"
