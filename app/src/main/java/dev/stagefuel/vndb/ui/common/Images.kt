package dev.stagefuel.vndb.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.stagefuel.vndb.api.Image
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType
import kotlin.math.roundToInt

/** Same rounding VNDB uses: 0 safe/tame, 1 suggestive/violent, 2 explicit/brutal. */
fun Image.sexualLevel() = sexual.roundToInt().coerceIn(0, 2)
fun Image.violenceLevel() = violence.roundToInt().coerceIn(0, 2)
fun Image.allowedBy(p: Prefs) = sexualLevel() <= p.maxSexual && violenceLevel() <= p.maxViolence

/**
 * Cover/screenshot honouring the image prefs: flagged images show a placeholder until tapped.
 * [thumb] picks the thumbnail URL where VNDB has one (screenshots).
 */
@Composable
fun GatedImage(image: Image?, prefs: Prefs, modifier: Modifier = Modifier, thumb: Boolean = false, contentScale: ContentScale = ContentScale.Fit) {
  val s = LocalSkin.current
  val url = image?.let { if (thumb) it.thumbnail ?: it.url else it.url }
  if (image == null || url == null) {
    Box(modifier.border(1.dp, s.border).background(s.tabBg), contentAlignment = Alignment.Center) {
      Text("no image", style = VndbType.small, color = s.grayedOut)
    }
    return
  }
  var revealed by rememberSaveable(url) { mutableStateOf(false) }
  if (image.allowedBy(prefs) || revealed) {
    AsyncImage(model = url, contentDescription = null, contentScale = contentScale, modifier = modifier)
  } else {
    val what = buildList {
      if (image.sexualLevel() > prefs.maxSexual) add(if (image.sexualLevel() == 2) "explicit" else "suggestive")
      if (image.violenceLevel() > prefs.maxViolence) add(if (image.violenceLevel() == 2) "brutal" else "violent")
    }.joinToString(" / ")
    Box(
      modifier.border(1.dp, s.statNok).background(s.tabBg).clickable { revealed = true }.padding(6.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text("$what image\ntap to show", style = VndbType.small, color = s.grayedOut, textAlign = TextAlign.Center)
    }
  }
}

@Composable
fun FillBox(content: @Composable () -> Unit) = Box(Modifier.fillMaxSize()) { content() }
