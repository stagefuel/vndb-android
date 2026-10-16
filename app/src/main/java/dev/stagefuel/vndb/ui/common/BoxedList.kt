package dev.stagefuel.vndb.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.stagefuel.vndb.ui.theme.LocalSkin

/**
 * Puts a run of lazy-list items inside one continuous site box (box background, 1dp border), the way
 * vndb.org shows a tab's content inside its `article`. Each item becomes a slice with side borders; the
 * run gets a top and bottom edge.
 */
fun LazyListScope.boxed(content: LazyListScope.() -> Unit) {
  item(contentType = "box-edge") { BoxEdge(top = true) }
  BoxedScope(this).content()
  item(contentType = "box-edge") { BoxEdge(top = false) }
}

private class BoxedScope(private val d: LazyListScope) : LazyListScope by d {
  override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) =
    d.item(key, contentType) { Slice { content() } }

  override fun items(count: Int, key: ((Int) -> Any)?, contentType: (Int) -> Any?, itemContent: @Composable LazyItemScope.(Int) -> Unit) =
    d.items(count, key, contentType) { i -> Slice { itemContent(i) } }
}

@Composable
private fun Slice(content: @Composable () -> Unit) {
  val s = LocalSkin.current
  // A Column, not a Box: an item's children (e.g. a heading and an image row) go one under the other.
  Column(
    Modifier.fillMaxWidth().padding(horizontal = 6.dp).background(s.boxBg).drawBehind {
      val w = 1.dp.toPx()
      drawLine(s.border, Offset(w / 2, 0f), Offset(w / 2, size.height), w)
      drawLine(s.border, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
    }.padding(horizontal = 2.dp),
  ) { content() }
}

@Composable
private fun BoxEdge(top: Boolean) {
  val s = LocalSkin.current
  Box(
    Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(6.dp).background(s.boxBg).drawBehind {
      val w = 1.dp.toPx()
      val y = if (top) w / 2 else size.height - w / 2
      drawLine(s.border, Offset(0f, y), Offset(size.width, y), w)
      drawLine(s.border, Offset(w / 2, 0f), Offset(w / 2, size.height), w)
      drawLine(s.border, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
    },
  )
}
