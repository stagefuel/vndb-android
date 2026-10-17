package dev.stagefuel.vndb.ui.vn

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stagefuel.vndb.VndbApp
import dev.stagefuel.vndb.api.Character
import dev.stagefuel.vndb.api.Image
import dev.stagefuel.vndb.api.Release
import dev.stagefuel.vndb.api.Vn
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.ui.Nav
import dev.stagefuel.vndb.ui.common.BbText
import dev.stagefuel.vndb.ui.common.BoxTitle
import dev.stagefuel.vndb.ui.common.GatedImage
import dev.stagefuel.vndb.ui.common.Grey
import dev.stagefuel.vndb.ui.common.InfoRow
import dev.stagefuel.vndb.ui.common.LinkText
import dev.stagefuel.vndb.ui.common.Loading
import dev.stagefuel.vndb.ui.common.Names
import dev.stagefuel.vndb.ui.common.SectionHeader
import dev.stagefuel.vndb.ui.common.VndbBox
import dev.stagefuel.vndb.ui.common.boxed
import dev.stagefuel.vndb.ui.common.stripe
import dev.stagefuel.vndb.ui.common.SpoilerSwitch
import dev.stagefuel.vndb.ui.common.ImageViewer
import dev.stagefuel.vndb.ui.common.ViewerImage
import androidx.compose.foundation.lazy.itemsIndexed
import dev.stagefuel.vndb.ui.common.VndbTabs
import dev.stagefuel.vndb.ui.common.LocalTabsBottom
import dev.stagefuel.vndb.ui.common.WarningBox
import dev.stagefuel.vndb.ui.common.formatRating
import dev.stagefuel.vndb.ui.list.ListEditSheet
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType

private val TABS = listOf("main", "tags", "releases", "characters", "staff")

@Composable
fun VnScreen(app: VndbApp, basePrefs: Prefs, nav: Nav, id: String) {
  val vm = viewModel(key = "vn-$id") { VnVm(app.repo, id, app.voteStats) }
  // Per-page spoiler level (starts at the setting); everything below reads it through prefs.spoilers.
  var spoil by rememberSaveable(id) { mutableIntStateOf(basePrefs.spoilers) }
  val prefs = basePrefs.copy(spoilers = spoil)
  var tab by rememberSaveable { mutableIntStateOf(0) }
  var editing by rememberSaveable { mutableStateOf(false) }

  when (val st = vm.vn) {
    Load.Idle, Load.Loading -> Loading()
    is Load.Failed -> WarningBox(st.message) { vm.loadVn() }
    is Load.Done -> {
      val v = st.value
      LaunchedEffect(tab) {
        when (TABS[tab]) {
          "main" -> { vm.loadVotes(); vm.loadCovers(); vm.loadQuotes() }
          "releases" -> vm.loadReleases()
          "characters" -> vm.loadCharacters()
        }
      }
      val bottomTabs = LocalTabsBottom.current
      // Everything the full-screen viewer can page through: main cover, release covers/artwork, screenshots.
      val shots = v.screenshots.filter { s -> s.release?.languages.orEmpty().any { it.lang == v.olang } }.ifEmpty { v.screenshots }
      val covers = (vm.covers as? Load.Done)?.value.orEmpty()
      val coverStart = if (v.image?.url != null) 1 else 0
      val shotStart = coverStart + covers.size
      val gallery = buildList {
        if (coverStart == 1) add(ViewerImage(v.image!!, "cover"))
        covers.forEach { (rel, img) ->
          val langs = img.languages.orEmpty().joinToString(", ") { Names.lang(it) }
          add(ViewerImage(img.asImage(), listOf(COVER_TYPES[img.type] ?: img.type, rel.title, langs).filter(String::isNotBlank).joinToString(" · ")))
        }
        shots.forEach { sh -> add(ViewerImage(Image(url = sh.url, sexual = sh.sexual, violence = sh.violence, thumbnail = sh.thumbnail), "screenshot")) }
      }
      var viewing by remember { mutableStateOf<Int?>(null) }
      viewing?.let { i -> ImageViewer(gallery, i, prefs) { viewing = null } }
      Column(Modifier.fillMaxSize()) {
      LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
        item {
          Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            BoxTitle(v.title, v.alttitle)
          }
          if (!bottomTabs) VndbTabs(TABS, tab) { tab = it }
        }
        when (TABS[tab]) {
          "main" -> {
            boxed {
              item { SpoilerSwitch(spoil) { spoil = it } }
              mainTab(v, prefs, nav, signedIn = prefs.account != null, onEdit = { editing = true },
                covers = covers, shots = shots, onView = { viewing = it }, coverStart = coverStart, shotStart = shotStart)
              vnQuotes(vm.quotes, nav)
            }
            item { VoteStatsBox(v.id, vm.votes, nav) { vm.loadVotes(force = true) } }
          }
          "tags" -> boxed { item { TagsTab(v, prefs, nav) { spoil = it } } }
          "releases" -> boxed { releasesTab(vm.releases, nav) { vm.loadReleases(force = true) } }
          "characters" -> boxed { item { SpoilerSwitch(spoil) { spoil = it } }; charactersTab(v.id, v.va, vm.characters, prefs, nav) { vm.loadCharacters(force = true) } }
          "staff" -> boxed { staffTab(v, nav) }
        }
      }
      if (bottomTabs) VndbTabs(TABS, tab) { tab = it }
      }
      if (editing) ListEditSheet(app, vnId = v.id, title = v.title, onDismiss = { editing = false })
    }
  }
}

private val COVER_TYPES = mapOf("pkgfront" to "front", "pkgback" to "back", "pkgside" to "side", "pkgmed" to "media",
  "pkgcontent" to "contents", "dig" to "digital")

private fun LazyListScope.mainTab(
  v: Vn, prefs: Prefs, nav: Nav, signedIn: Boolean, onEdit: () -> Unit,
  covers: List<Pair<dev.stagefuel.vndb.api.ReleaseImages, dev.stagefuel.vndb.api.ReleaseImage>>,
  shots: List<dev.stagefuel.vndb.api.Screenshot>, onView: (Int) -> Unit, coverStart: Int, shotStart: Int,
) {
  item {
    Column(Modifier.padding(horizontal = 4.dp)) {
      if (coverStart == 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        GatedImage(v.image, prefs, Modifier.widthIn(max = 250.dp).height(320.dp).clickable(enabled = coverStart == 1) { onView(0) })
      }
      Spacer(Modifier.height(8.dp))
      InfoTable(v, nav)
      Spacer(Modifier.height(8.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (signedIn) LinkText("edit list entry") { onEdit() }
        val ctx = androidx.compose.ui.platform.LocalContext.current
        LinkText("share") { shareVn(ctx, v) }
        LinkText("history") { nav.web("https://vndb.org/${v.id}/hist") }
        LinkText("open on vndb.org") { nav.web("https://vndb.org/${v.id}") }
      }
      if (!v.description.isNullOrBlank()) {
        SectionHeader("Description")
        BbText(v.description, onLink = nav::open, showSpoilers = prefs.spoilers >= 2)
      }
    }
  }
  if (covers.isNotEmpty()) item {
    SectionHeader("Covers & artwork", Modifier.padding(horizontal = 8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
      itemsIndexed(covers, key = { _, c -> c.second.id }) { i, (_, img) ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          GatedImage(img.asImage(), prefs, Modifier.size(105.dp, 150.dp).clickable { onView(coverStart + i) }, thumb = true)
          Grey(COVER_TYPES[img.type] ?: img.type)
        }
      }
    }
  }
  if (shots.isNotEmpty()) item {
    SectionHeader("Screenshots", Modifier.padding(horizontal = 8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
      itemsIndexed(shots, key = { _, sh -> sh.url }) { i, s ->
        val img = Image(url = s.url, sexual = s.sexual, violence = s.violence, thumbnail = s.thumbnail)
        GatedImage(img, prefs, Modifier.size(200.dp, 150.dp).clickable { onView(shotStart + i) }, thumb = true)
      }
    }
  }
  if (v.relations.isNotEmpty()) item {
    Column(Modifier.padding(horizontal = 8.dp)) {
      SectionHeader("Relations")
      v.relations.groupBy { it.relation }.toSortedMap(compareBy<String> { Names.relationOrder.indexOf(it).let { i -> if (i < 0) 99 else i } }.thenBy { it }).forEach { (rel, list) ->
        Text(Names.relations[rel] ?: rel, style = VndbType.body, color = LocalSkin.current.mainText, modifier = Modifier.padding(top = 4.dp))
        list.forEachIndexed { i, r ->
          Row(Modifier.fillMaxWidth().stripe(i, LocalSkin.current).padding(start = 12.dp, top = 1.dp, bottom = 1.dp)) {
            if (!r.official) Grey("[unofficial] ")
            LinkText(r.title) { nav.vn(r.id) }
          }
        }
      }
    }
  }
}

@Composable
private fun InfoTable(v: Vn, nav: Nav) {
  val skin = LocalSkin.current
  var odd = true
  @Composable fun row(key: String, content: @Composable () -> Unit) { InfoRow(key, odd, value = content); odd = !odd }
  @Composable fun text(s: String) = Text(s, style = VndbType.body, color = skin.mainText)

  val main = v.titles.firstOrNull { it.main }
  if (main != null && main.title != v.title) row("Original title") { text(main.title) }
  if (v.aliases.isNotEmpty()) row("Aliases") { text(v.aliases.joinToString(", ")) }
  row("Status") { text(Names.devstatus[v.devstatus] ?: "") }
  if (v.released != null) row("Released") { text(v.released) }
  // "Long (33h07m from 203 votes)": VNDB's length category, from the vote average when there is one.
  val bucket = v.lengthMinutes?.let { lengthBucket(it) } ?: v.length
  val label = bucket?.let { LENGTH_LABEL[it] }
  val len = when {
    v.lengthMinutes != null -> "${label?.let { "$it (" } ?: ""}${Names.minutes(v.lengthMinutes)} from ${v.lengthVotes ?: 0} votes${if (label != null) ")" else ""}"
    bucket != null -> Names.lengths[bucket]
    else -> null
  }
  if (len != null) row("Length") { text(len) }
  if (v.developers.isNotEmpty()) row("Developer") {
    Column { v.developers.forEach { d -> LinkText(d.name) { nav.item(d.id) } } }
  }
  if (v.languages.isNotEmpty()) row("Languages") { text(v.languages.joinToString(", ") { Names.lang(it) }) }
  if (v.platforms.isNotEmpty()) row("Platforms") { text(v.platforms.joinToString(", ") { Names.platform(it) }) }
  if (v.rating != null) row("Rating") {
    Text("${formatRating(v.rating)} weighted, ${formatRating(v.average)} average (${v.votecount} votes)", style = VndbType.body, color = skin.mainText)
  }
  if (v.extlinks.isNotEmpty()) row("Links") {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { v.extlinks.forEach { l -> LinkText(l.label) { nav.open(l.url) } } }
  }
}

/** `div#vntags`: tags sized by rating, spoiler level picker, summary vs all, category filter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsTab(v: Vn, prefs: Prefs, nav: Nav, onSpoil: (Int) -> Unit) {
  val skin = LocalSkin.current
  val spoil = prefs.spoilers
  var all by rememberSaveable { mutableStateOf(false) }
  var cats by rememberSaveable { mutableStateOf(setOf("cont", "tech") + if (prefs.maxSexual >= 1) setOf("ero") else emptySet()) }

  Column(Modifier.padding(8.dp)) {
    @Composable fun opt(label: String, on: Boolean, click: () -> Unit) =
      Text(label, style = VndbType.small, color = if (on) skin.mainText else skin.link, modifier = Modifier.clickable(onClick = click).padding(4.dp))
    FlowRow(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
      Names.tagCats.forEach { (k, label) -> opt(label, k in cats) { cats = if (k in cats) cats - k else cats + k } }
      Text(" | ", style = VndbType.small, color = skin.grayedOut, modifier = Modifier.padding(vertical = 4.dp))
      listOf("hide spoilers", "show minor spoilers", "spoil me!").forEachIndexed { i, l -> opt(l, spoil == i) { onSpoil(i) } }
      Text(" | ", style = VndbType.small, color = skin.grayedOut, modifier = Modifier.padding(vertical = 4.dp))
      opt("summary", !all) { all = false }
      opt("all", all) { all = true }
    }
    val shown = v.tags
      .filter { it.category in cats && it.spoiler <= spoil && it.rating > 0 }
      .sortedByDescending { it.rating }
      .let { list -> if (all) list else list.groupBy { it.category }.values.flatMap { it.take(15) }.sortedByDescending { it.rating } }
    Spacer(Modifier.height(6.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
      shown.forEach { t ->
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.clickable { nav.item(t.id) }) {
          Text(
            t.name,
            color = skin.link,
            fontSize = (t.rating * 3.5 + 6).coerceAtLeast(10.0).sp,
            textDecoration = if (t.lie) TextDecoration.LineThrough else null,
          )
          if (t.spoiler > 0) Text("S", fontSize = 9.sp, color = if (t.spoiler == 2) skin.standout else skin.grayedOut)
          Text(" %.1f".format(t.rating), fontSize = 10.sp, color = skin.grayedOut)
        }
      }
    }
    if (shown.isEmpty()) Grey("No tags to show.", style = VndbType.body)
  }
}

private fun LazyListScope.releasesTab(st: Load<List<Release>>, nav: Nav, retry: () -> Unit) {
  when (st) {
    Load.Idle, Load.Loading -> item { Loading() }
    is Load.Failed -> item { WarningBox(st.message, retry) }
    is Load.Done -> {
      if (st.value.isEmpty()) item { Grey("No releases.", Modifier.padding(12.dp), VndbType.body) }
      // One group per language, like the site's <details> per language.
      val byLang = st.value.flatMap { r -> r.languages.map { it.lang to r } }.groupBy({ it.first }, { it.second })
      byLang.forEach { (lang, rels) ->
        item(key = "lang-$lang") {
          val skin = LocalSkin.current
          Text(
            "${Names.lang(lang)} (${rels.size})",
            style = VndbType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = skin.mainText,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp)
              .border(1.dp, skin.border).background(skin.blendBg).padding(horizontal = 10.dp, vertical = 6.dp),
          )
        }
        itemsIndexed(rels, key = { _, it -> "$lang-${it.id}" }) { i, r -> ReleaseRow(r, lang, nav, i) }
      }
    }
  }
}

@Composable
private fun ReleaseRow(r: Release, lang: String, nav: Nav, index: Int) {
  val skin = LocalSkin.current
  val mtl = r.languages.firstOrNull { it.lang == lang }?.mtl == true
  Row(Modifier.fillMaxWidth().stripe(index, skin).clickable { nav.item(r.id) }.padding(horizontal = 10.dp, vertical = 4.dp)) {
    Grey(r.released ?: "TBA", Modifier.width(84.dp), VndbType.small)
    Grey(r.minage?.let { "$it+" } ?: "", Modifier.width(30.dp), VndbType.small)
    Column(Modifier.weight(1f)) {
      Text(r.title, style = VndbType.body, color = if (mtl) skin.grayedOut else skin.link)
      val flags = buildList {
        if (r.platforms.isNotEmpty()) add(r.platforms.joinToString(", ") { Names.platform(it) })
        r.vns.firstOrNull()?.rtype?.let { Names.rtypes[it] }?.takeIf { it.isNotEmpty() }?.let(::add)
        if (r.patch) add("patch")
        if (r.freeware) add("freeware")
        if (!r.official) add("unofficial")
        if (mtl) add("machine translation")
        r.voiced?.let { Names.voiced[it] }?.let(::add)
      }
      if (flags.isNotEmpty()) Grey(flags.joinToString(" · "))
    }
  }
}

private fun LazyListScope.charactersTab(vnId: String, va: List<dev.stagefuel.vndb.api.VnVa>, st: Load<List<Character>>, prefs: Prefs, nav: Nav, retry: () -> Unit) {
  val voices = va.groupBy { it.character?.id }
  when (st) {
    Load.Idle, Load.Loading -> item { Loading() }
    is Load.Failed -> item { WarningBox(st.message, retry) }
    is Load.Done -> {
      val entries = st.value.mapNotNull { c -> c.vns.firstOrNull { it.id == vnId }?.let { c to it } }
        .filter { (_, cv) -> cv.spoiler <= prefs.spoilers }
      if (entries.isEmpty()) item { Grey("No characters to show.", Modifier.padding(12.dp), VndbType.body) }
      Names.charRoleOrder.forEach { role ->
        val list = entries.filter { it.second.role == role }
        if (list.isNotEmpty()) {
          item(key = "role-$role") { SectionHeader(Names.charRoles[role] ?: role, Modifier.padding(horizontal = 8.dp)) }
          itemsIndexed(list, key = { _, it -> it.first.id }) { i, (c, _) ->
            Row(Modifier.fillMaxWidth().stripe(i, LocalSkin.current).clickable { nav.item(c.id) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
              GatedImage(c.image, prefs, Modifier.size(56.dp, 72.dp), contentScale = ContentScale.Crop)
              Spacer(Modifier.width(8.dp))
              Column {
                Text(c.name, style = VndbType.body, color = LocalSkin.current.link)
                c.original?.let { Grey(it) }
                // Voice actors (vn.va); the note usually says which edition.
                voices[c.id].orEmpty().forEach { v ->
                  val st = v.staff ?: return@forEach
                  Row(Modifier.padding(top = 2.dp)) {
                    Grey("CV ")
                    LinkText(st.name, style = VndbType.small) { nav.item(st.id) }
                    v.note?.takeIf { it.isNotBlank() }?.let { Grey(" ($it)") }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun LazyListScope.staffTab(v: Vn, nav: Nav) {
  if (v.staff.isEmpty()) item { Grey("No staff listed.", Modifier.padding(12.dp), VndbType.body) }
  // Grouped by edition like the site: original credits first, then each edition (Realta Nua, Remastered…).
  val byEdition = v.staff.groupBy { it.eid }
  val order = listOf<Int?>(null) + v.editions.map { it.eid } + (byEdition.keys - v.editions.map { it.eid }.toSet() - setOf(null))
  order.distinct().filter { byEdition.containsKey(it) }.forEach { eid ->
    val ed = v.editions.firstOrNull { it.eid == eid }
    if (eid != null || byEdition.size > 1) item(key = "ed-$eid") {
      Text(
        ed?.let { e -> e.name + (e.lang?.let { " (${Names.lang(it)})" } ?: "") + if (!e.official) " [unofficial]" else "" } ?: "Original edition",
        style = VndbType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = LocalSkin.current.mainText,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp)
          .border(1.dp, LocalSkin.current.border).background(LocalSkin.current.blendBg).padding(horizontal = 10.dp, vertical = 6.dp),
      )
    }
    staffRoles(byEdition.getValue(eid), "e$eid", nav)
  }
}

private fun LazyListScope.staffRoles(staff: List<dev.stagefuel.vndb.api.VnStaff>, keyPrefix: String, nav: Nav) {
  staff.groupBy { it.role }.toSortedMap(compareBy<String> { Names.staffRoleOrder.indexOf(it).let { i -> if (i < 0) 99 else i } }.thenBy { it }).forEach { (role, list) ->
    item(key = "staff-$keyPrefix-$role") {
      Column(Modifier.padding(horizontal = 8.dp)) {
        SectionHeader(Names.staffRoles[role] ?: role)
        list.forEachIndexed { i, s ->
          Row(Modifier.fillMaxWidth().stripe(i, LocalSkin.current).padding(vertical = 2.dp, horizontal = 4.dp)) {
            LinkText(s.name) { nav.item(s.id) }
            s.note?.takeIf { it.isNotBlank() }?.let { Grey("  $it") }
          }
        }
      }
    }
  }
}

/** The site's "User stats" box: vote distribution, totals, recent votes and a link to every vote. */
@Composable
private fun VoteStatsBox(vnId: String, state: Load<dev.stagefuel.vndb.web.VoteStats?>, nav: Nav, retry: () -> Unit) {
  val skin = LocalSkin.current
  VndbBox(Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp)) {
    SectionHeader("User stats")
    when (state) {
      Load.Idle, Load.Loading -> Loading()
      is Load.Failed -> WarningBox(state.message, onRetry = retry)
      is Load.Done -> {
        val s = state.value
        if (s == null || s.total == 0) {
          Grey("Nobody has voted on this visual novel yet.", style = VndbType.body)
        } else {
          val max = s.buckets.maxOf { it.second }.coerceAtLeast(1)
          s.buckets.forEach { (n, c) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
              Text("$n", style = VndbType.body, color = skin.mainText, textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.width(24.dp))
              Spacer(Modifier.width(8.dp))
              Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (c > 0) androidx.compose.foundation.layout.Box(
                  Modifier.fillMaxWidth(0.82f * c / max).height(14.dp).background(skin.border),
                )
                Text(" $c", style = VndbType.small, color = skin.mainText)
              }
            }
          }
          s.summary.forEach { Grey(it, Modifier.padding(top = 2.dp), VndbType.small) }
          if (s.recent.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Recent votes", style = VndbType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = skin.mainText)
            s.recent.forEachIndexed { i, r ->
              InfoRow(r.date, odd = i % 2 == 0, keyWidth = 96) {
                Row(Modifier.fillMaxWidth()) {
                  Box(Modifier.weight(1f)) {
                    if (r.userId != null) LinkText(r.user ?: r.userId) { nav.web("https://vndb.org/${r.userId}") }
                    else Grey(r.user ?: "hidden", style = VndbType.body)
                  }
                  Text(r.vote, style = VndbType.body, color = skin.mainText)
                }
              }
            }
          }
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          LinkText("all votes ›") { nav.web("https://vndb.org/$vnId/votes") }
          val n = state.value?.reviews ?: 0
          LinkText(if (n > 0) "$n review${if (n == 1) "" else "s"} ›" else "reviews ›") { nav.web("https://vndb.org/$vnId/reviews") }
        }
      }
    }
  }
}

/** The VN's top quotes, highest score first. */
private fun LazyListScope.vnQuotes(quotes: List<dev.stagefuel.vndb.api.Quote>, nav: Nav) {
  if (quotes.isEmpty()) return
  item(key = "quotes") {
    Column(Modifier.padding(horizontal = 8.dp)) {
      SectionHeader("Quotes")
      quotes.forEachIndexed { i, q ->
        Column(Modifier.fillMaxWidth().stripe(i, LocalSkin.current).padding(vertical = 3.dp, horizontal = 4.dp)) {
          Text("\u201C${q.quote}\u201D", style = VndbType.body, color = LocalSkin.current.mainText)
          q.character?.takeIf { it.id.isNotEmpty() }?.let { c -> Row { Grey("\u2014 "); LinkText(c.name ?: c.id, style = VndbType.small) { nav.item(c.id) } } }
        }
      }
    }
  }
}

private val LENGTH_LABEL = mapOf(1 to "Very short", 2 to "Short", 3 to "Medium", 4 to "Long", 5 to "Very long")

/** VNDB's length buckets: < 2h, 2-10h, 10-30h, 30-50h, > 50h. */
internal fun lengthBucket(minutes: Int): Int = when {
  minutes < 2 * 60 -> 1
  minutes < 10 * 60 -> 2
  minutes < 30 * 60 -> 3
  minutes < 50 * 60 -> 4
  else -> 5
}

/** Share sheet with the VN's title and vndb.org link. */
private fun shareVn(ctx: android.content.Context, v: Vn) {
  val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(android.content.Intent.EXTRA_SUBJECT, v.title)
    putExtra(android.content.Intent.EXTRA_TEXT, "${v.title}\nhttps://vndb.org/${v.id}")
  }
  ctx.startActivity(android.content.Intent.createChooser(send, null))
}
