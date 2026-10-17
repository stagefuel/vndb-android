package dev.stagefuel.vndb.ui.list

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import dev.stagefuel.vndb.ui.common.LocalTabsBottom
import dev.stagefuel.vndb.ui.common.VndbTabs
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stagefuel.vndb.VndbApp
import dev.stagefuel.vndb.api.Image
import dev.stagefuel.vndb.api.UlistEntry
import dev.stagefuel.vndb.api.UlistLabel
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.data.VndbRepository
import dev.stagefuel.vndb.ui.Nav
import dev.stagefuel.vndb.ui.common.GatedImage
import dev.stagefuel.vndb.ui.common.Grey
import dev.stagefuel.vndb.ui.common.LinkText
import dev.stagefuel.vndb.ui.common.Loading
import dev.stagefuel.vndb.ui.common.VndbBox
import dev.stagefuel.vndb.ui.common.WarningBox
import dev.stagefuel.vndb.ui.common.stripe
import dev.stagefuel.vndb.ui.common.boxed
import dev.stagefuel.vndb.ui.common.formatVote
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val SORTS = listOf(
  "lastmod" to "last modified", "vote" to "vote", "voted" to "vote date", "added" to "added",
  "title" to "title", "released" to "release date", "started" to "started", "finished" to "finished",
)

class MyListVm(private val repo: VndbRepository, val uid: String) : ViewModel() {
  var labels by mutableStateOf<List<UlistLabel>>(emptyList()); private set
  var label by mutableStateOf<Int?>(1)
  var sort by mutableStateOf("lastmod")
  var reverse by mutableStateOf(true)
  var entries by mutableStateOf<List<UlistEntry>>(emptyList()); private set
  var loading by mutableStateOf(false); private set
  var error by mutableStateOf<String?>(null); private set
  var more by mutableStateOf(false); private set
  private var page = 1
  private var job: Job? = null

  init { refresh() }

  fun refresh() {
    viewModelScope.launch { runCatching { labels = repo.labels(uid) } }
    page = 1; load(reset = true)
  }

  fun loadMore() { if (!loading && more) { page++; load(reset = false) } }

  private fun load(reset: Boolean) {
    job?.cancel()
    job = viewModelScope.launch {
      loading = true; error = null
      runCatching { repo.ulist(uid, label, sort, reverse, page) }
        .onSuccess { r -> entries = if (reset) r.results else (entries + r.results).distinctBy { it.id }; more = r.more }
        .onFailure { error = it.message ?: "Failed to load list" }
      loading = false
    }
  }
}

/** A user's list; [uid] null = the signed-in user. Only your own list is editable. */
@Composable
fun MyListScreen(app: VndbApp, prefs: Prefs, nav: Nav, uid: String? = null, name: String? = null) {
  val own = prefs.account
  val account = if (uid == null || uid == own?.id) own else dev.stagefuel.vndb.data.Account(uid, name ?: uid, emptyList())
  val editable = account != null && account.id == own?.id
  if (account == null) {
    Column(Modifier.padding(16.dp)) {
      Text("Add a VNDB API token on the Me tab to see and edit your list.", style = VndbType.body, color = LocalSkin.current.mainText)
    }
    return
  }
  val vm = viewModel(key = "list-${account.id}") { MyListVm(app.repo, account.id) }
  val skin = LocalSkin.current
  var editing by remember { mutableStateOf<UlistEntry?>(null) }
  var sortOpen by remember { mutableStateOf(false) }

  Column(Modifier.fillMaxSize()) {
    Text("${account.username}'s list", style = VndbType.h1, color = skin.boxTitle, modifier = Modifier.padding(12.dp))
    val bottomTabs = LocalTabsBottom.current
    val all = listOf<UlistLabel?>(null) + vm.labels.filter { it.id != 0 }
    val labelTabs = @Composable {
      VndbTabs(all.map { l -> (l?.label ?: "All") + (l?.count?.let { " ($it)" } ?: "") },
        all.indexOfFirst { it?.id == vm.label }.coerceAtLeast(0), small = true) { i -> vm.label = all[i]?.id; vm.refresh() }
    }
    if (!bottomTabs) labelTabs()
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
      Grey("sort: ", style = VndbType.small)
      Column {
        LinkText(SORTS.first { it.first == vm.sort }.second, style = VndbType.small) { sortOpen = true }
        DropdownMenu(sortOpen, { sortOpen = false }, containerColor = skin.solidBox) {
          SORTS.forEach { (k, label) ->
            DropdownMenuItem(text = { Text(label, color = skin.mainText) }, onClick = { sortOpen = false; vm.sort = k; vm.refresh() })
          }
        }
      }
      Spacer(Modifier.width(10.dp))
      LinkText(if (vm.reverse) "▼ desc" else "▲ asc", style = VndbType.small) { vm.reverse = !vm.reverse; vm.refresh() }
    }
    vm.error?.let { WarningBox(it, vm::refresh) }
    if (vm.loading && vm.entries.isEmpty()) Loading()
    if (!vm.loading && vm.error == null && vm.entries.isEmpty()) Grey("Nothing here.", Modifier.padding(12.dp), VndbType.body)
    LazyColumn(Modifier.weight(1f)) {
      // One box with striped rows, like the site's list table.
      if (vm.entries.isNotEmpty()) boxed {
        itemsIndexed(vm.entries, key = { _, e -> e.id }) { i, e ->
          if (i >= vm.entries.size - 5) LaunchedEffect(vm.entries.size) { vm.loadMore() }
          EntryRow(e, prefs, i, onOpen = { nav.vn(e.id) }, onEdit = if (editable) ({ editing = e }) else null)
        }
      }
      if (vm.loading && vm.entries.isNotEmpty()) item { Loading() }
    }
    if (bottomTabs) labelTabs()
  }
  editing?.let { e ->
    ListEditSheet(app, vnId = e.id, title = e.vn?.title ?: e.id, onDismiss = { editing = null }, onSaved = vm::refresh)
  }
}

@Composable
private fun EntryRow(e: UlistEntry, prefs: Prefs, index: Int, onOpen: () -> Unit, onEdit: (() -> Unit)?) {
  val skin = LocalSkin.current
  val vn = e.vn
  Column(Modifier.fillMaxWidth().stripe(index, skin).clickable(onClick = onOpen).padding(4.dp)) {
    Row {
      GatedImage(
        vn?.image?.let { Image(url = it.url, sexual = it.sexual, violence = it.violence) }, prefs,
        Modifier.size(48.dp, 64.dp), contentScale = ContentScale.Crop,
      )
      Spacer(Modifier.width(8.dp))
      Column(Modifier.weight(1f)) {
        Text(vn?.title ?: e.id, style = VndbType.body, color = skin.link, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Grey(e.labels.filter { it.id != 7 }.joinToString(", ") { it.label })
        val dates = listOfNotNull(e.started?.let { "started $it" }, e.finished?.let { "finished $it" }).joinToString(" · ")
        if (dates.isNotEmpty()) Grey(dates)
        e.notes?.takeIf { it.isNotBlank() }?.let { Text(it, style = VndbType.small, color = skin.grayedOut, maxLines = 3, overflow = TextOverflow.Ellipsis) }
      }
      Column(horizontalAlignment = Alignment.End) {
        Text(formatVote(e.vote), style = VndbType.boxH1, color = if (e.vote != null) skin.standout else skin.grayedOut)
        if (onEdit != null) LinkText("edit", style = VndbType.small, modifier = Modifier.padding(top = 8.dp)) { onEdit() }
      }
    }
  }
}
