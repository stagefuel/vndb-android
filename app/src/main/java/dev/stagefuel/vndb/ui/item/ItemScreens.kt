package dev.stagefuel.vndb.ui.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stagefuel.vndb.VndbApp
import dev.stagefuel.vndb.api.Character
import dev.stagefuel.vndb.api.CharacterFull
import dev.stagefuel.vndb.api.ExtLink
import dev.stagefuel.vndb.api.Producer
import dev.stagefuel.vndb.api.ReleaseFull
import dev.stagefuel.vndb.api.Staff
import dev.stagefuel.vndb.api.Tag
import dev.stagefuel.vndb.api.Trait
import dev.stagefuel.vndb.api.Vn
import dev.stagefuel.vndb.api.VnCredit
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
import dev.stagefuel.vndb.ui.common.VndbTabs
import dev.stagefuel.vndb.ui.common.LocalTabsBottom
import dev.stagefuel.vndb.ui.common.WarningBox
import dev.stagefuel.vndb.ui.search.VnRow
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType
import dev.stagefuel.vndb.ui.vn.Load

/** Native page for any non-VN DB id (c, s, p, g, i, r). */
@Composable
fun ItemScreen(app: VndbApp, prefs: Prefs, nav: Nav, id: String) {
  when (id.first()) {
    'c' -> Loaded(id, { app.repo.character(id) }) { CharacterPage(it, prefs, nav) }
    's' -> Loaded(id, { app.repo.staff(id) }) { StaffPage(app, it, prefs, nav) }
    'p' -> Loaded(id, { app.repo.producer(id) }) { ProducerPage(app, it, prefs, nav) }
    'g' -> Loaded(id, { app.repo.tag(id) }) { TagPage(app, it, prefs, nav) }
    'i' -> Loaded(id, { app.repo.trait(id) }) { TraitPage(app, it, prefs, nav) }
    'r' -> Loaded(id, { app.repo.release(id) }) { ReleasePage(it, nav) }
    else -> WarningBox("Unsupported page $id")
  }
}

@Composable
private fun <T> Loaded(id: String, fetch: suspend () -> T, content: @Composable (T) -> Unit) {
  val vm = viewModel(key = "item-$id") { LoaderVm(fetch) }
  when (val s = vm.state) {
    Load.Idle, Load.Loading -> Loading()
    is Load.Failed -> WarningBox(s.message) { vm.reload() }
    is Load.Done -> content(s.value)
  }
}

/** A box page: title + content in a LazyColumn, with a trailing "open on vndb.org". */
@Composable
private fun Page(id: String, title: String, alt: String?, nav: Nav, bottomBar: (@Composable () -> Unit)? = null, body: LazyListScope.() -> Unit) {
  Column(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
      item { Column(Modifier.padding(10.dp)) { BoxTitle(title, alt) } }
      body()
      item {
        // No API for edits or revisions: these open the site's own pages in the app.
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          LinkText("history") { nav.web("https://vndb.org/$id/hist") }
          LinkText("edit") { nav.web("https://vndb.org/$id/edit") }
          LinkText("open on vndb.org") { nav.web("https://vndb.org/$id") }
        }
      }
    }
    bottomBar?.invoke()
  }
}

@Composable
private fun Table(rows: List<Pair<String, @Composable () -> Unit>>) {
  rows.forEachIndexed { i, (k, v) -> InfoRow(k, odd = i % 2 == 0, value = v) }
}

@Composable
private fun T(s: String) = Text(s, style = VndbType.body, color = LocalSkin.current.mainText)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Links(links: List<ExtLink>, nav: Nav) =
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { links.forEach { l -> LinkText(l.label) { nav.open(l.url) } } }

private fun LazyListScope.description(text: String?, nav: Nav, prefs: Prefs) {
  if (!text.isNullOrBlank()) item {
    Column(Modifier.padding(horizontal = 8.dp)) {
      SectionHeader("Description")
      BbText(text, onLink = nav::open, showSpoilers = prefs.spoilers >= 2)
    }
  }
}

private fun LazyListScope.vnList(vm: PagedVm<Vn>, prefs: Prefs, nav: Nav, empty: String) {
  itemsIndexed(vm.items, key = { _, v -> "vn-" + v.id }) { i, v ->
    if (i >= vm.items.size - 5) LaunchedEffect(vm.items.size) { vm.loadMore() }
    Column(Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) { VnRow(v, prefs) { nav.vn(v.id) } }
  }
  item {
    LaunchedEffect(Unit) { vm.loadMore() }
    when {
      vm.loading -> Loading()
      vm.error != null -> WarningBox(vm.error!!) { vm.loadMore() }
      vm.items.isEmpty() && !vm.more -> Grey(empty, Modifier.padding(12.dp), VndbType.body)
    }
  }
}

// ------------------------------------------------------------------------------------------ character

private val SEX = mapOf("m" to "Male", "f" to "Female", "b" to "Both", "n" to "Sexless")
private val MONTHS = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

@Composable
private fun CharacterPage(c: CharacterFull, basePrefs: Prefs, nav: Nav) {
  val skin = LocalSkin.current
  var spoil by rememberSaveable(c.id) { mutableIntStateOf(basePrefs.spoilers) }
  val prefs = basePrefs.copy(spoilers = spoil)
  Page(c.id, c.name, c.original, nav) {
    item {
      VndbBox(Modifier.padding(horizontal = 6.dp)) {
        dev.stagefuel.vndb.ui.common.SpoilerSwitch(spoil) { spoil = it }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
          GatedImage(c.image, prefs, Modifier.widthIn(max = 256.dp).height(300.dp).clickable(enabled = c.image?.url != null) {
            c.image?.let { nav.images(listOf(dev.stagefuel.vndb.ui.common.ViewerImage(it, c.name)), 0) }
          })
        }
        Spacer(Modifier.height(8.dp))
        val rows = buildList<Pair<String, @Composable () -> Unit>> {
          if (c.aliases.isNotEmpty()) add("Aliases" to { T(c.aliases.joinToString(", ")) })
          val sex = c.sex?.let { s -> s.getOrNull(0)?.let { SEX[it] } + (s.getOrNull(1)?.takeIf { it != s[0] && prefs.spoilers >= 2 }?.let { " (actually ${SEX[it]})" } ?: "") }
          if (sex != null) add("Sex" to { T(sex) })
          val measures = listOfNotNull(
            c.height?.let { "Height: ${it}cm" }, c.weight?.let { "Weight: ${it}kg" },
            if (c.bust != null || c.waist != null || c.hips != null) "Bust-Waist-Hips: ${c.bust ?: "?"}-${c.waist ?: "?"}-${c.hips ?: "?"}cm" else null,
            c.cup?.let { "$it cup" },
          )
          if (measures.isNotEmpty()) add("Measurements" to { T(measures.joinToString(", ")) })
          c.bloodType?.let { add("Blood type" to { T(it.uppercase()) }) }
          c.birthday?.takeIf { it.size == 2 }?.let { (m, d) -> add("Birthday" to { T("${MONTHS.getOrElse(m) { "" }} $d") }) }
          c.age?.let { add("Age" to { T("$it") }) }
        }
        Table(rows)
        val traits = c.traits.filter { it.spoiler <= prefs.spoilers }.groupBy { it.groupName ?: "Other" }
        if (traits.isNotEmpty()) {
          SectionHeader("Traits")
          traits.forEach { (group, list) ->
            InfoRow(group, odd = false, keyWidth = 110) {
              @OptIn(ExperimentalLayoutApi::class)
              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                list.forEach { t ->
                  Text(
                    t.name + if (t.spoiler > 0) " (S)" else "",
                    style = VndbType.body,
                    color = if (t.spoiler == 2) skin.standout else skin.link,
                    textDecoration = if (t.lie) TextDecoration.LineThrough else null,
                    modifier = Modifier.clickable { nav.item(t.id) },
                  )
                }
              }
            }
          }
        }
      }
    }
    description(c.description, nav, prefs)
    val vns = c.vns.filter { it.spoiler <= prefs.spoilers }.distinctBy { it.id }
    if (vns.isNotEmpty()) item {
      Column(Modifier.padding(horizontal = 8.dp)) {
        SectionHeader("Visual novels")
        vns.forEach { v ->
          Row(Modifier.padding(vertical = 2.dp)) {
            Grey((Names.charRoles[v.role] ?: v.role) + "  ", Modifier.width(130.dp))
            LinkText(v.title ?: v.id) { nav.vn(v.id) }
          }
        }
      }
    }
  }
}

// ------------------------------------------------------------------------------------------ staff

@Composable
private fun StaffPage(app: VndbApp, s: Staff, prefs: Prefs, nav: Nav) {
  var tab by rememberSaveable { mutableIntStateOf(0) }
  val credits = viewModel(key = "credits-${s.id}") { LoaderVm { app.repo.staffCredits(s.id) } }
  val voiced = viewModel(key = "voiced-${s.id}") { LoaderVm { app.repo.voiced(s.id) } }
  val bottomTabs = LocalTabsBottom.current
  val tabs = @Composable { VndbTabs(listOf("credits", "voiced"), tab) { tab = it } }
  Page(s.id, s.name, s.original, nav, bottomBar = if (bottomTabs) tabs else null) {
    item {
      VndbBox(Modifier.padding(horizontal = 6.dp)) {
        val others = s.aliases.filter { !it.ismain }
        Table(buildList {
          if (others.isNotEmpty()) add("Aliases" to { T(others.joinToString(", ") { a -> a.latin?.let { "$it (${a.name})" } ?: a.name }) })
          s.lang?.let { add("Language" to { T(Names.lang(it)) }) }
          s.gender?.let { g -> add("Gender" to { T(SEX[g] ?: g) }) }
          if (s.extlinks.isNotEmpty()) add("Links" to { Links(s.extlinks, nav) })
        })
      }
    }
    description(s.description, nav, prefs)
    if (!bottomTabs) item { Spacer(Modifier.height(8.dp)); tabs() }
    if (tab == 0) when (val st = credits.state) {
      Load.Idle, Load.Loading -> item { Loading() }
      is Load.Failed -> item { WarningBox(st.message) { credits.reload() } }
      is Load.Done -> creditsList(st.value, nav)
    } else when (val st = voiced.state) {
      Load.Idle, Load.Loading -> item { Loading() }
      is Load.Failed -> item { WarningBox(st.message) { voiced.reload() } }
      is Load.Done -> voicedList(st.value, prefs, nav)
    }
  }
}

private fun LazyListScope.creditsList(list: List<VnCredit>, nav: Nav) {
  if (list.isEmpty()) item { Grey("No credits.", Modifier.padding(12.dp), VndbType.body) }
  items(list, key = { "cr-" + it.id }) { v ->
    Row(Modifier.fillMaxWidth().clickable { nav.vn(v.id) }.padding(horizontal = 10.dp, vertical = 4.dp)) {
      Grey(v.released ?: "TBA", Modifier.width(84.dp))
      Column(Modifier.weight(1f)) {
        Text(v.title, style = VndbType.body, color = LocalSkin.current.link)
        Grey(v.staff.joinToString(", ") { c -> (Names.staffRoles[c.role] ?: c.role) + (c.note?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "") })
      }
    }
  }
}

private fun LazyListScope.voicedList(list: List<Character>, prefs: Prefs, nav: Nav) {
  val rows = list.flatMap { c -> c.vns.filter { it.spoiler <= prefs.spoilers }.map { c to it } }
  if (rows.isEmpty()) item { Grey("No voiced characters.", Modifier.padding(12.dp), VndbType.body) }
  items(rows, key = { (c, v) -> "va-${c.id}-${v.id}" }) { (c, v) ->
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp)) {
      LinkText(c.name, Modifier.weight(1f)) { nav.item(c.id) }
      LinkText(v.title ?: v.id, Modifier.weight(1f)) { nav.vn(v.id) }
    }
  }
}

// ------------------------------------------------------------------------------------------ producer / tag / trait

private val PRODUCER_TYPE = mapOf("co" to "Company", "in" to "Individual", "ng" to "Amateur group")

@Composable
private fun ProducerPage(app: VndbApp, p: Producer, prefs: Prefs, nav: Nav) {
  val vns = viewModel(key = "pvns-${p.id}") { PagedVm { page -> app.repo.producerVns(p.id, page) } }
  Page(p.id, p.name, p.original, nav) {
    item {
      VndbBox(Modifier.padding(horizontal = 6.dp)) {
        Table(buildList {
          p.type?.let { add("Type" to { T(PRODUCER_TYPE[it] ?: it) }) }
          p.lang?.let { add("Language" to { T(Names.lang(it)) }) }
          if (p.aliases.isNotEmpty()) add("Aliases" to { T(p.aliases.joinToString(", ")) })
          if (p.extlinks.isNotEmpty()) add("Links" to { Links(p.extlinks, nav) })
        })
      }
    }
    description(p.description, nav, prefs)
    item { SectionHeader("Developed", Modifier.padding(horizontal = 8.dp)) }
    vnList(vns, prefs, nav, "No visual novels.")
  }
}

@Composable
private fun TagPage(app: VndbApp, t: Tag, prefs: Prefs, nav: Nav) {
  val vns = viewModel(key = "gvns-${t.id}") { PagedVm { page -> app.repo.tagVns(t.id, page) } }
  Page(t.id, t.name, null, nav) {
    item {
      VndbBox(Modifier.padding(horizontal = 6.dp)) {
        Table(buildList {
          add("Category" to { T(Names.tagCats[t.category] ?: t.category) })
          if (t.aliases.isNotEmpty()) add("Aliases" to { T(t.aliases.joinToString(", ")) })
          add("Visual novels" to { T("${t.vnCount}") })
        })
      }
    }
    description(t.description, nav, prefs)
    item { SectionHeader("Visual novels (by rating)", Modifier.padding(horizontal = 8.dp)) }
    vnList(vns, prefs, nav, "No visual novels.")
  }
}

@Composable
private fun TraitPage(app: VndbApp, t: Trait, prefs: Prefs, nav: Nav) {
  val chars = viewModel(key = "ichars-${t.id}") { PagedVm { page -> app.repo.traitChars(t.id, page) } }
  Page(t.id, t.name, t.groupName, nav) {
    item {
      VndbBox(Modifier.padding(horizontal = 6.dp)) {
        Table(buildList {
          t.groupId?.let { gid -> add("Group" to { LinkText(t.groupName ?: gid) { nav.item(gid) } }) }
          if (t.aliases.isNotEmpty()) add("Aliases" to { T(t.aliases.joinToString(", ")) })
          add("Characters" to { T("${t.charCount}") })
        })
      }
    }
    description(t.description, nav, prefs)
    if (t.applicable) {
      item { SectionHeader("Characters", Modifier.padding(horizontal = 8.dp)) }
      itemsIndexed(chars.items, key = { _, c -> "ch-" + c.id }) { i, c ->
        if (i >= chars.items.size - 5) LaunchedEffect(chars.items.size) { chars.loadMore() }
        Row(Modifier.fillMaxWidth().clickable { nav.item(c.id) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
          GatedImage(c.image, prefs, Modifier.size(48.dp, 64.dp), contentScale = ContentScale.Crop)
          Spacer(Modifier.width(8.dp))
          Column {
            Text(c.name, style = VndbType.body, color = LocalSkin.current.link)
            c.original?.let { Grey(it) }
          }
        }
      }
      item {
        LaunchedEffect(Unit) { chars.loadMore() }
        if (chars.loading) Loading()
        chars.error?.let { WarningBox(it) { chars.loadMore() } }
      }
    }
  }
}

// ------------------------------------------------------------------------------------------ release

@Composable
private fun ReleasePage(r: ReleaseFull, nav: Nav) {
  Page(r.id, r.title, r.alttitle, nav) {
    item {
      VndbBox(Modifier.padding(horizontal = 6.dp)) {
        Table(buildList {
          if (r.vns.isNotEmpty()) add("Visual novel" to {
            Column { r.vns.forEach { v -> LinkText(v.title ?: v.id) { nav.vn(v.id) } } }
          })
          add("Type" to { T(listOfNotNull(r.vns.firstOrNull()?.rtype, if (r.patch) "patch" else null, if (!r.official) "unofficial" else null).joinToString(", ")) })
          if (r.languages.isNotEmpty()) add("Language" to { T(r.languages.joinToString(", ") { Names.lang(it.lang) + if (it.mtl) " (machine translation)" else "" }) })
          if (r.platforms.isNotEmpty()) add("Platform" to { T(r.platforms.joinToString(", ") { Names.platform(it) }) })
          if (r.media.isNotEmpty()) add("Media" to { T(r.media.joinToString(", ") { m -> (if (m.qty > 1) "${m.qty} " else "") + m.medium }) })
          add("Released" to { T(r.released ?: "TBA") })
          r.minage?.let { add("Age rating" to { T(if (it == 0) "All ages" else "$it+") }) }
          add("Publication" to { T((if (r.freeware) "Freeware" else "Non-free") + ", " + if (r.hasEro) "contains erotic scenes" else "no erotic scenes") })
          resolution(r)?.let { res -> add("Resolution" to { T(res) }) }
          r.engine?.let { add("Engine" to { T(it) }) }
          r.voiced?.let { v -> Names.voiced[v]?.let { add("Voiced" to { T(it) }) } }
          val devs = r.producers.filter { it.developer }
          val pubs = r.producers.filter { it.publisher }
          if (devs.isNotEmpty()) add("Developer" to { Column { devs.forEach { p -> LinkText(p.name) { nav.item(p.id) } } } })
          if (pubs.isNotEmpty()) add("Publisher" to { Column { pubs.forEach { p -> LinkText(p.name) { nav.item(p.id) } } } })
          r.gtin?.let { add("JAN/EAN/UPC" to { T(it) }) }
          r.catalog?.let { add("Catalog no." to { T(it) }) }
          if (r.extlinks.isNotEmpty()) add("Links" to { Links(r.extlinks, nav) })
        })
        if (!r.notes.isNullOrBlank()) {
          SectionHeader("Notes")
          BbText(r.notes, onLink = nav::open)
        }
      }
    }
  }
}

private fun resolution(r: ReleaseFull): String? = when (val res = r.resolution) {
  null, kotlinx.serialization.json.JsonNull -> null
  is kotlinx.serialization.json.JsonArray -> res.joinToString("x") { it.toString() }
  is kotlinx.serialization.json.JsonPrimitive -> if (res.content == "non-standard") "Non-standard" else res.content
  else -> null
}
