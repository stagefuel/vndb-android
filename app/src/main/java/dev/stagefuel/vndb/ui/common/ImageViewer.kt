package dev.stagefuel.vndb.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.stagefuel.vndb.api.Image
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType

data class ViewerImage(val image: Image, val caption: String)

/** Full-screen images: swipe between them, pinch or double-tap to zoom. Flagged images stay gated. */
@Composable
fun ImageViewer(images: List<ViewerImage>, start: Int, prefs: Prefs, onDismiss: () -> Unit) {
  val skin = LocalSkin.current
  Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
    val pager = rememberPagerState(start.coerceIn(0, (images.size - 1).coerceAtLeast(0))) { images.size }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
      HorizontalPager(pager, Modifier.fillMaxSize(), beyondViewportPageCount = 1) { i -> Zoomable(images[i].image, prefs) }
      Text(
        "✕", fontSize = 22.sp, color = Color.White,
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 36.dp, end = 12.dp).clickable(onClick = onDismiss).padding(12.dp),
      )
      images.getOrNull(pager.currentPage)?.let { cur ->
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.6f)).padding(12.dp)) {
          Text("${pager.currentPage + 1} / ${images.size}", style = VndbType.small, color = skin.grayedOut,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
          if (cur.caption.isNotBlank()) Text(cur.caption, style = VndbType.body, color = Color.White,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
      }
    }
  }
}

@Composable
private fun Zoomable(image: Image, prefs: Prefs) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }
  Box(
    Modifier.fillMaxSize()
      // Pinch zooms; one-finger drags only pan once zoomed in, so swipes still change pages.
      .pointerInput(Unit) {
        awaitEachGesture {
          awaitFirstDown(requireUnconsumed = false)
          do {
            val ev = awaitPointerEvent()
            val zoom = ev.calculateZoom()
            if (zoom != 1f || scale > 1f) {
              scale = (scale * zoom).coerceIn(1f, 6f)
              offset = if (scale > 1f) offset + ev.calculatePan() else Offset.Zero
              ev.changes.forEach { if (it.positionChanged()) it.consume() }
            }
          } while (ev.changes.any { it.pressed })
        }
      }
      .pointerInput(Unit) {
        detectTapGestures(onDoubleTap = { if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f })
      },
    contentAlignment = Alignment.Center,
  ) {
    GatedImage(
      image, prefs,
      Modifier.fillMaxSize().graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
    )
  }
}
