package dev.stagefuel.vndb.ui.search

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stagefuel.vndb.VndbApp
import dev.stagefuel.vndb.api.Vn
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.data.VndbRepository
import dev.stagefuel.vndb.ui.Nav
import dev.stagefuel.vndb.ui.common.GatedImage
import dev.stagefuel.vndb.ui.common.Grey
import dev.stagefuel.vndb.ui.common.LinkText
import dev.stagefuel.vndb.ui.common.Loading
import dev.stagefuel.vndb.ui.common.VndbBox
import dev.stagefuel.vndb.ui.common.WarningBox
import dev.stagefuel.vndb.ui.common.formatRating
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

val SEARCH_SORTS = listOf(
  Triple("searchrank", "relevance", false), Triple("rating", "rating", true), Triple("votecount", "votes", true),
  Triple("released", "newest", true), Triple("released", "oldest", false), Triple("title", "title", false),
)

class SearchVm(private val repo: VndbRepository) : ViewModel() {
  var query by mutableStateOf("")
  var sort by mutableStateOf(0)
  var stats by mutableStateOf<dev.stagefuel.vndb.api.Stats?>(null); private set

  var quote by mutableStateOf<dev.stagefuel.vndb.api.Quote?>(null); private set

  init {
    viewModelScope.launch { runCatching { stats = repo.stats() } }
    viewModelScope.launch { runCatching { quote = repo.randomQuote() } }
  }
  var results by mutableStateOf<List<Vn>>(emptyList()); private set
  var loading by mutableStateOf(false); private set
  var error by mutableStateOf<String?>(null); private set
  var more by mutableStateOf(false); private set
  var searched by mutableStateOf(false); private set
  private var page = 1
  private var job: Job? = null

  /** A blank query browses the whole database in the chosen order (relevance falls back to rating). */
  fun search() {
    val q = query.trim()
    if (q.isEmpty() && sort == 0) return
    page = 1
    load(q, reset = true)
  }

  fun loadMore() {
    if (loading || !more) return
    page++
    load(query.trim(), reset = false)
  }

  private fun load(q: String, reset: Boolean) {
    job?.cancel()
    job = viewModelScope.launch {
      loading = true; error = null
      val (key, _, rev) = SEARCH_SORTS[sort]
      runCatching { repo.browseVns(q, key, rev, page) }
        .onSuccess { r -> results = if (reset) r.results else results + r.results; more = r.more; searched = true }
        .onFailure { error = it.message ?: "Search failed" }
      loading = false
    }
  }
}

@Composable
fun SearchScreen(app: VndbApp, prefs: Prefs, nav: Nav) {
  val vm = viewModel { SearchVm(app.repo) }
  val skin = LocalSkin.current
  Column(Modifier.fillMaxSize()) {
    Text(
      "the visual novel database",
      style = VndbType.siteTitle,
      color = skin.mainTitle,
      modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
    )
    BasicTextField(
      value = vm.query,
      onValueChange = { vm.query = it },
      singleLine = true,
      textStyle = VndbType.body.copy(color = skin.mainText),
      cursorBrush = SolidColor(skin.mainText),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { vm.search() }),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).border(1.dp, skin.secBorder).padding(8.dp),
      decorationBox = { inner ->
        if (vm.query.isEmpty()) Grey("search", style = VndbType.body)
        inner()
      },
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Grey("sort:", style = VndbType.small)
      SEARCH_SORTS.forEachIndexed { i, (_, label, _) ->
        Text(
          label, style = VndbType.small, color = if (vm.sort == i) skin.mainText else skin.link,
          modifier = Modifier.clickable { vm.sort = i; if (vm.query.isNotBlank() || i != 0) vm.search() },
        )
      }
    }
    if (!vm.searched && vm.results.isEmpty()) {
      vm.stats?.let { StatsBox(it) }
      vm.quote?.let { q -> QuoteLine(q) { q.vn?.id?.takeIf(String::isNotEmpty)?.let(nav::vn) } }
    }
    when {
      vm.error != null -> WarningBox(vm.error!!, onRetry = vm::search)
      vm.loading && vm.results.isEmpty() -> Loading()
      vm.searched && vm.results.isEmpty() -> Grey("No results.", Modifier.padding(12.dp), VndbType.body)
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      itemsIndexed(vm.results, key = { _, v -> v.id }) { i, v ->
        if (i >= vm.results.size - 5) LaunchedEffect(vm.results.size) { vm.loadMore() }
        VnRow(v, prefs) { nav.vn(v.id) }
      }
      if (vm.loading && vm.results.isNotEmpty()) item { Loading() }
      if (vm.more && !vm.loading) item { LinkText("next ›", Modifier.padding(12.dp)) { vm.loadMore() } }
    }
  }
}

@Composable
fun VnRow(v: Vn, prefs: Prefs, onClick: () -> Unit) {
  val skin = LocalSkin.current
  VndbBox(Modifier.clickable(onClick = onClick), padding = 4) {
    Row(verticalAlignment = Alignment.Top) {
      GatedImage(v.image, prefs, Modifier.size(64.dp, 86.dp), thumb = true, contentScale = ContentScale.Crop)
      Spacer(Modifier.width(8.dp))
      Column(Modifier.weight(1f)) {
        Text(v.title, style = VndbType.body, color = skin.link, maxLines = 2, overflow = TextOverflow.Ellipsis)
        v.alttitle?.takeIf { it != v.title }?.let { Grey(it, style = VndbType.small) }
        Grey(listOfNotNull(v.released, v.developers.joinToString(", ") { it.name }.ifBlank { null }).joinToString(" · "))
        if (v.rating != null) Text("${formatRating(v.rating)} (${v.votecount})", style = VndbType.small, color = skin.mainText)
      }
    }
  }
}

/** The sidebar's "Database Statistics" box. */
@Composable
private fun StatsBox(s: dev.stagefuel.vndb.api.Stats) {
  val skin = LocalSkin.current
  VndbBox(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
    Text("Database Statistics", style = VndbType.boxH1, color = skin.boxTitle, modifier = Modifier.padding(bottom = 4.dp))
    listOf("Visual Novels" to s.vn, "Releases" to s.releases, "Producers" to s.producers, "Characters" to s.chars,
      "Staff" to s.staff, "Tags" to s.tags, "Traits" to s.traits).forEach { (k, v) ->
      Row(Modifier.fillMaxWidth()) {
        Text(k, style = VndbType.body.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = skin.mainText, modifier = Modifier.weight(0.6f))
        Text("%,d".format(v), style = VndbType.body, color = skin.mainText, modifier = Modifier.weight(0.4f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
      }
    }
  }
}

/** The site footer's random quote: `"quote" — VN title`, tapping opens the VN. */
@Composable
private fun QuoteLine(q: dev.stagefuel.vndb.api.Quote, onClick: () -> Unit) {
  val skin = LocalSkin.current
  Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp)) {
    Text("\u201C${q.quote}\u201D", style = VndbType.small, color = skin.link, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth())
    val by = listOfNotNull(q.character?.name, q.vn?.title).joinToString(", ")
    if (by.isNotEmpty()) Grey("\u2014 $by", Modifier.fillMaxWidth(), VndbType.small)
  }
}
