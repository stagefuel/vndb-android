package dev.stagefuel.vndb.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stagefuel.vndb.VndbApp
import dev.stagefuel.vndb.api.FeedRelease
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.data.SearchFilter
import dev.stagefuel.vndb.data.VndbRepository
import dev.stagefuel.vndb.ui.Nav
import dev.stagefuel.vndb.ui.common.BoxTitle
import dev.stagefuel.vndb.ui.common.FilterSheet
import dev.stagefuel.vndb.ui.common.GatedImage
import dev.stagefuel.vndb.ui.common.Grey
import dev.stagefuel.vndb.ui.common.LinkText
import dev.stagefuel.vndb.ui.common.Loading
import dev.stagefuel.vndb.ui.common.Names
import dev.stagefuel.vndb.ui.common.VndbBox
import dev.stagefuel.vndb.ui.common.WarningBox
import dev.stagefuel.vndb.ui.common.stripe
import dev.stagefuel.vndb.ui.common.boxed
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

const val FEED_UPCOMING = "upcoming"
const val FEED_RECENT = "recent"
fun feedTitle(kind: String) = if (kind == FEED_UPCOMING) "Upcoming releases" else "Just released"

/** One VN per row: its first (soonest / newest) release in the feed, plus how many more editions there are. */
data class FeedItem(val release: FeedRelease, val editions: Int) {
  val vn get() = release.vns.first()
}

fun group(list: List<FeedRelease>): List<FeedItem> =
  list.filter { it.vns.isNotEmpty() }.groupBy { it.vns.first().id }.values.map { FeedItem(it.first(), it.size) }

/** Paged feed with filters; grouping by VN is applied over everything loaded so far. */
class FeedVm(private val repo: VndbRepository, private val upcoming: Boolean, results: Int = 50) : ViewModel() {
  private val pageSize = results
  var filter by mutableStateOf(SearchFilter()); private set
  private var raw = emptyList<FeedRelease>()
  var items by mutableStateOf<List<FeedItem>>(emptyList()); private set
  var loading by mutableStateOf(false); private set
  var error by mutableStateOf<String?>(null); private set
  var more by mutableStateOf(true); private set
  private var page = 0
  private var job: Job? = null

  init { loadMore() }

  fun apply(f: SearchFilter) { filter = f; raw = emptyList(); items = emptyList(); page = 0; more = true; job?.cancel(); loading = false; loadMore() }

  fun loadMore() {
    if (loading || !more) return
    loading = true; error = null
    job = viewModelScope.launch {
      runCatching { repo.releaseFeed(upcoming, filter, page + 1, pageSize) }
        .onSuccess { r -> page++; raw = (raw + r.results).distinctBy { it.id }; items = group(raw); more = r.more }
        .onFailure { error = it.message ?: "Failed to load" }
      loading = false
    }
  }
}

/** Start-page row: covers with the VN title and date; "more ›" opens the full list. */
@Composable
fun FeedRow(kind: String, items: List<FeedItem>, prefs: Prefs, nav: Nav) {
  if (items.isEmpty()) return
  val skin = LocalSkin.current
  // Compact (72×100 covers, one-line titles) so both rows fit on the start page without scrolling.
  Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
      Text(feedTitle(kind), style = VndbType.boxH1.copy(fontSize = androidx.compose.ui.unit.TextUnit(15f, androidx.compose.ui.unit.TextUnitType.Sp)),
        color = skin.boxTitle, modifier = Modifier.weight(1f))
      LinkText("more ›", style = VndbType.small) { nav.feed(kind) }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
      items(items, key = { it.vn.id }) { it ->
        Column(Modifier.width(72.dp).clickable { nav.vn(it.vn.id) }) {
          GatedImage(it.vn.image, prefs, Modifier.size(72.dp, 100.dp), thumb = true, contentScale = ContentScale.Crop)
          Text(it.vn.title, style = VndbType.small, color = skin.link, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(it.release.released.orEmpty(), style = VndbType.small.copy(fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = skin.grayedOut, maxLines = 1)
        }
      }
    }
  }
}

/** Full upcoming / just-released list, one VN per row, with language and platform filters. */
@Composable
fun FeedScreen(app: VndbApp, prefs: Prefs, nav: Nav, kind: String) {
  val vm = viewModel(key = "feed-$kind") { FeedVm(app.repo, kind == FEED_UPCOMING) }
  val skin = LocalSkin.current
  var filtering by remember { mutableStateOf(false) }
  Column(Modifier.fillMaxSize()) {
    Column(Modifier.padding(10.dp)) { BoxTitle(feedTitle(kind)) }
    Row(Modifier.padding(horizontal = 12.dp)) {
      LinkText("filters" + if (vm.filter.isEmpty) "" else " (${vm.filter.count})", style = VndbType.small) { filtering = true }
    }
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 6.dp)) {
      if (vm.items.isNotEmpty()) boxed {
      itemsIndexed(vm.items, key = { _, it -> it.vn.id }) { i, it ->
        if (i >= vm.items.size - 5) LaunchedEffect(vm.items.size) { vm.loadMore() }
        Column(Modifier.fillMaxWidth().stripe(i, skin).clickable { nav.vn(it.vn.id) }.padding(4.dp)) {
          Row {
            GatedImage(it.vn.image, prefs, Modifier.size(48.dp, 64.dp), thumb = true, contentScale = ContentScale.Crop)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
              Text(it.vn.title, style = VndbType.body, color = skin.link, maxLines = 2, overflow = TextOverflow.Ellipsis)
              if (it.release.title != it.vn.title) Grey(it.release.title, style = VndbType.small)
              Grey(listOfNotNull(
                it.release.released,
                it.release.platforms.joinToString(", ") { p -> Names.platform(p) }.ifBlank { null },
                it.release.languages.joinToString(", ") { l -> Names.lang(l.lang) }.ifBlank { null },
                if (it.editions > 1) "+${it.editions - 1} more" else null,
              ).joinToString(" · "))
            }
          }
        }
      }
      }
      item {
        when {
          vm.loading -> Loading()
          vm.error != null -> WarningBox(vm.error!!) { vm.loadMore() }
          vm.items.isEmpty() -> Grey("Nothing here.", Modifier.padding(12.dp), VndbType.body)
        }
      }
    }
  }
  if (filtering) FilterSheet(vm.filter, full = false, onDismiss = { filtering = false }) { f -> filtering = false; vm.apply(f) }
}

