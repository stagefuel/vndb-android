package dev.stagefuel.vndb.data

import dev.stagefuel.vndb.api.Character
import dev.stagefuel.vndb.api.CharacterFull
import dev.stagefuel.vndb.api.Producer
import dev.stagefuel.vndb.api.Quote
import dev.stagefuel.vndb.api.ReleaseImage
import dev.stagefuel.vndb.api.ReleaseImages
import dev.stagefuel.vndb.api.ReleaseFull
import dev.stagefuel.vndb.api.Staff
import dev.stagefuel.vndb.api.Stats
import dev.stagefuel.vndb.api.Tag
import dev.stagefuel.vndb.api.Trait
import dev.stagefuel.vndb.api.VnCredit
import dev.stagefuel.vndb.api.Fields
import dev.stagefuel.vndb.api.Filters
import dev.stagefuel.vndb.api.KanaClient
import dev.stagefuel.vndb.api.Query
import dev.stagefuel.vndb.api.QueryResult
import dev.stagefuel.vndb.api.Release
import dev.stagefuel.vndb.api.UlistEntry
import dev.stagefuel.vndb.api.UlistLabel
import dev.stagefuel.vndb.api.Vn
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Everything the screens ask VNDB for. Small in-memory cache so back-navigation doesn't refetch. */
class VndbRepository(private val api: KanaClient) {
  private val vnCache = object : LinkedHashMap<String, Vn>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Vn>?) = size > 50
  }

  suspend fun searchVns(q: String, page: Int = 1): QueryResult<Vn> {
    val id = Filters.vnId(q)
    return api.query(
      "vn",
      Query(
        filters = if (id != null) Filters.id(id) else Filters.search(q),
        fields = Fields.VN_BRIEF,
        sort = if (id != null) null else "searchrank",
        results = 25,
        page = page,
      ),
      Vn.serializer(),
    )
  }

  suspend fun vn(id: String, refresh: Boolean = false): Vn {
    if (!refresh) synchronized(vnCache) { vnCache[id] }?.let { return it }
    val v = api.query("vn", Query(filters = Filters.id(id), fields = Fields.VN_FULL, results = 1), Vn.serializer())
      .results.firstOrNull() ?: throw NoSuchElementException("VNDB has no entry $id")
    synchronized(vnCache) { vnCache[id] = v }
    return v
  }

  suspend fun releases(vnId: String): List<Release> = pages { page ->
    api.query("release", Query(filters = Filters.vn(vnId), fields = Fields.RELEASE, sort = "released", results = 100, page = page), Release.serializer())
  }

  /** Covers and other package art from every release of the VN, front covers first, one entry per image. */
  suspend fun releaseImages(vnId: String): List<Pair<ReleaseImages, ReleaseImage>> {
    val rels = pages(3) { page ->
      api.query("release", Query(filters = Filters.vn(vnId), fields = Fields.RELEASE_IMAGES, sort = "released", results = 100, page = page), ReleaseImages.serializer())
    }
    val order = listOf("pkgfront", "dig", "pkgback", "pkgcontent", "pkgside", "pkgmed")
    return rels.flatMap { r -> r.images.map { r to it } }
      .distinctBy { it.second.id }
      .sortedBy { order.indexOf(it.second.type).let { i -> if (i < 0) order.size else i } }
  }

  suspend fun characters(vnId: String): List<Character> = pages { page ->
    api.query("character", Query(filters = Filters.vn(vnId), fields = Fields.CHARACTER_BRIEF, sort = "name", results = 100, page = page), Character.serializer())
  }

  /** The "search" sort is VNDB's search rank; anything else needs a query (or browses everything). */
  suspend fun browseVns(q: String, sort: String, reverse: Boolean, page: Int, extra: SearchFilter = SearchFilter()): QueryResult<Vn> {
    val id = Filters.vnId(q)
    val base = when {
      id != null -> Filters.id(id)
      q.isNotBlank() -> Filters.search(q)
      else -> null
    }
    val filter = SearchFilter.and(listOfNotNull(base) + extra.parts())
    val s = if (sort == "searchrank" && (id != null || q.isBlank())) "rating" else sort
    return api.query("vn", Query(filters = filter, fields = Fields.VN_BRIEF, sort = s, reverse = reverse, results = 25, page = page), Vn.serializer())
  }

  suspend fun stats(): Stats = api.stats()

  /**
   * Official releases after today (soonest first) or up to today (newest first). The same VN shows up once per
   * edition/platform, so callers group by VN.
   */
  suspend fun releaseFeed(upcoming: Boolean, extra: SearchFilter, page: Int, results: Int = 50): QueryResult<dev.stagefuel.vndb.api.FeedRelease> {
    val today = java.time.LocalDate.now().toString()
    val parts = listOf(Filters.f("released", if (upcoming) ">" else "<=", today), Filters.f("official", "=", 1)) + extra.parts()
    return api.query("release", Query(filters = SearchFilter.and(parts), fields = Fields.FEED, sort = "released", reverse = !upcoming,
      results = results, page = page), dev.stagefuel.vndb.api.FeedRelease.serializer())
  }

  /** A random existing VN: the first id at or after a random number below the newest id (ids have gaps). */
  suspend fun randomVnId(): String {
    val newest = api.query("vn", Query(fields = "id", sort = "id", reverse = true, results = 1), dev.stagefuel.vndb.api.IdOnly.serializer())
      .results.first().id.drop(1).toInt()
    repeat(3) {
      val r = kotlin.random.Random.nextInt(1, newest + 1)
      api.query("vn", Query(filters = Filters.f("id", ">=", "v$r"), fields = "id", sort = "id", results = 1), dev.stagefuel.vndb.api.IdOnly.serializer())
        .results.firstOrNull()?.let { return it.id }
    }
    return "v$newest"
  }
  /** The VN's best-rated quotes (the site shows them under the description). */
  suspend fun vnQuotes(vnId: String, n: Int = 10): List<Quote> =
    api.query("quote", Query(filters = Filters.quotesOf(vnId), fields = Fields.QUOTE, sort = "score", reverse = true, results = n), Quote.serializer()).results

  /**
   * Search for anything other than VNs. Results stay raw JSON so one row layout fits every type;
   * [SearchTypes] says which fields to ask for and how to show them.
   */
  suspend fun searchOther(type: String, q: String, page: Int): QueryResult<kotlinx.serialization.json.JsonObject> {
    val t = SearchTypes.byKey.getValue(type)
    return api.query(type, Query(filters = Filters.search(q), fields = t.fields, sort = "searchrank", results = 30, page = page),
      kotlinx.serialization.json.JsonObject.serializer())
  }

  suspend fun randomQuote(): Quote? =
    api.query("quote", Query(filters = Filters.randomQuote, fields = Fields.QUOTE, results = 1), Quote.serializer()).results.firstOrNull()
  suspend fun user(q: String) = api.user(q)

  suspend fun character(id: String): CharacterFull = one("character", id, Fields.CHARACTER_FULL, CharacterFull.serializer())
  suspend fun staff(id: String): Staff = one("staff", id, Fields.STAFF, Staff.serializer())
  suspend fun producer(id: String): Producer = one("producer", id, Fields.PRODUCER, Producer.serializer())
  suspend fun tag(id: String): Tag = one("tag", id, Fields.TAG, Tag.serializer())
  suspend fun trait(id: String): Trait = one("trait", id, Fields.TRAIT, Trait.serializer())
  suspend fun release(id: String): ReleaseFull = one("release", id, Fields.RELEASE_FULL, ReleaseFull.serializer())

  suspend fun staffCredits(id: String): List<VnCredit> = pages(3) { p ->
    api.query("vn", Query(filters = Filters.staff(id), fields = "id,title,released,staff.id,staff.role,staff.note", sort = "released", results = 100, page = p), VnCredit.serializer())
  }.map { v -> v.copy(staff = v.staff.filter { it.id == id }) }

  suspend fun voiced(id: String): List<Character> = pages(3) { p ->
    api.query("character", Query(filters = Filters.seiyuu(id), fields = "id,name,original,vns.id,vns.title,vns.role,vns.spoiler", sort = "name", results = 100, page = p), Character.serializer())
  }

  suspend fun producerVns(id: String, page: Int) =
    api.query("vn", Query(filters = Filters.developer(id), fields = Fields.VN_BRIEF, sort = "released", results = 50, page = page), Vn.serializer())

  suspend fun tagVns(id: String, page: Int) =
    api.query("vn", Query(filters = Filters.tag(id), fields = Fields.VN_BRIEF, sort = "rating", reverse = true, results = 50, page = page), Vn.serializer())

  suspend fun traitChars(id: String, page: Int) =
    api.query("character", Query(filters = Filters.trait(id), fields = Fields.CHARACTER_BRIEF, sort = "name", results = 50, page = page), Character.serializer())

  private suspend fun <T> one(type: String, id: String, fields: String, s: kotlinx.serialization.KSerializer<T>): T =
    api.query(type, Query(filters = Filters.id(id), fields = fields, results = 1), s).results.firstOrNull()
      ?: throw NoSuchElementException("VNDB has no entry $id")

  suspend fun labels(user: String?): List<UlistLabel> = api.ulistLabels(user)

  suspend fun ulist(user: String, label: Int?, sort: String, reverse: Boolean, page: Int): QueryResult<UlistEntry> =
    api.query(
      "ulist",
      Query(user = user, filters = label?.let(Filters::label), fields = Fields.ULIST, sort = sort, reverse = reverse, results = 50, page = page),
      UlistEntry.serializer(),
    )

  suspend fun ulistEntry(user: String, vnId: String): UlistEntry? =
    api.query("ulist", Query(user = user, filters = Filters.id(vnId), fields = Fields.ULIST, results = 1), UlistEntry.serializer())
      .results.firstOrNull()

  /** Applies [edit] as a minimal PATCH relative to [orig]; no request when nothing changed (unless it's a new entry). */
  suspend fun saveEntry(vnId: String, edit: ListEdit, orig: UlistEntry?) {
    val patch = edit.toPatch(orig)
    if (patch.isNotEmpty() || orig == null) api.patchUlist(vnId, patch)
  }
  suspend fun removeEntry(vnId: String) = api.deleteUlist(vnId)
  suspend fun setReleaseStatus(rid: String, status: Int?) =
    if (status == null) api.deleteRlist(rid) else api.patchRlist(rid, status)

  private suspend fun <T> pages(max: Int = 5, fetch: suspend (Int) -> QueryResult<T>): List<T> {
    val out = mutableListOf<T>()
    for (p in 1..max) {
      val r = fetch(p)
      out += r.results
      if (!r.more) break
    }
    return out
  }
}

/**
 * The editable parts of a list entry. [toPatch] sends only what changed relative to [UlistEntry] `orig`
 * (null = not on the list yet), because Kana:
 *  - can't clear `started`/`finished` (null is rejected), so removing a date is an error here;
 *  - recomputes label 7 (Voted) only on vote changes, and a full `labels` overwrite would drop it,
 *    so labels go out as `labels_set`/`labels_unset`. Labels 0 and 7 are never sent.
 */
data class ListEdit(
  val vote: Int?,
  val notes: String,
  val started: String?,
  val finished: String?,
  val labels: Set<Int>,
) {
  class CantClearDate(which: String) : IllegalArgumentException(
    "VNDB's API can't remove a $which date. Put it back, or clear it on vndb.org.",
  )

  fun toPatch(orig: UlistEntry?): JsonObject {
    val started = started?.trim().orEmpty()
    val finished = finished?.trim().orEmpty()
    if (started.isEmpty() && !orig?.started.isNullOrEmpty()) throw CantClearDate("start")
    if (finished.isEmpty() && !orig?.finished.isNullOrEmpty()) throw CantClearDate("finish")
    val before = orig?.labels?.map { it.id }?.toSet().orEmpty() - MANAGED
    val after = labels - MANAGED
    return buildJsonObject {
      if (orig == null || vote != orig.vote) put("vote", vote?.let(::JsonPrimitive) ?: JsonNull)
      if (notes.trim() != orig?.notes.orEmpty().trim()) put("notes", notes.trim())
      if (started.isNotEmpty() && started != orig?.started) put("started", started)
      if (finished.isNotEmpty() && finished != orig?.finished) put("finished", finished)
      (after - before).sorted().takeIf { it.isNotEmpty() }?.let { l -> put("labels_set", buildJsonArray { l.forEach { add(JsonPrimitive(it)) } }) }
      (before - after).sorted().takeIf { it.isNotEmpty() }?.let { l -> put("labels_unset", buildJsonArray { l.forEach { add(JsonPrimitive(it)) } }) }
    }
  }

  companion object {
    /** Labels VNDB manages itself: 0 (none) and 7 (Voted). */
    val MANAGED = setOf(0, 7)
  }
}

/** Non-VN search targets: Kana endpoint, field list, and how a hit is summarised in one grey line. */
object SearchTypes {
  data class T(val key: String, val label: String, val fields: String, val sub: (kotlinx.serialization.json.JsonObject) -> String)
  private fun kotlinx.serialization.json.JsonObject.str(k: String) =
    (this[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content
  private fun kotlinx.serialization.json.JsonObject.num(k: String) =
    (this[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
  private fun kotlinx.serialization.json.JsonObject.strs(k: String) =
    (this[k] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }.orEmpty()

  val all = listOf(
    T("release", "releases", "id,title,alttitle,released,platforms") { o ->
      listOfNotNull(o.str("released"), o.strs("platforms").joinToString(", ") { dev.stagefuel.vndb.ui.common.Names.platform(it) }.ifBlank { null }).joinToString(" · ") },
    T("producer", "producers", "id,name,original,type,lang") { o ->
      listOfNotNull(mapOf("co" to "Company", "in" to "Individual", "ng" to "Amateur group")[o.str("type")], o.str("lang")?.let { dev.stagefuel.vndb.ui.common.Names.lang(it) }).joinToString(" · ") },
    T("character", "characters", "id,name,original") { "" },
    T("staff", "staff", "id,name,original,lang") { o -> o.str("lang")?.let { dev.stagefuel.vndb.ui.common.Names.lang(it) }.orEmpty() },
    T("tag", "tags", "id,name,category,vn_count") { o ->
      listOfNotNull(dev.stagefuel.vndb.ui.common.Names.tagCats.toMap()[o.str("category")], o.num("vn_count")?.let { "$it VNs" }).joinToString(" · ") },
    T("trait", "traits", "id,name,group_name,char_count") { o ->
      listOfNotNull(o.str("group_name"), o.num("char_count")?.let { "$it characters" }).joinToString(" · ") },
  )
  val byKey = all.associateBy { it.key }
  fun title(o: kotlinx.serialization.json.JsonObject) = o.str("title") ?: o.str("name") ?: o.str("id").orEmpty()
  fun original(o: kotlinx.serialization.json.JsonObject) = o.str("alttitle") ?: o.str("original")
  fun id(o: kotlinx.serialization.json.JsonObject) = o.str("id").orEmpty()
}

/**
 * Filters picked in the filter sheet. Values within a group are OR'd (any of these languages), groups are AND'd.
 * VN search uses all of them; the release feeds only languages and platforms (`lang`/`platform` work on both).
 */
data class SearchFilter(
  val langs: Set<String> = emptySet(),
  val platforms: Set<String> = emptySet(),
  val lengths: Set<Int> = emptySet(),
  val fromYear: Int? = null,
  val toYear: Int? = null,
) {
  val count get() = langs.size + platforms.size + lengths.size + listOfNotNull(fromYear, toYear).size
  val isEmpty get() = count == 0

  fun parts(): List<kotlinx.serialization.json.JsonElement> = buildList {
    if (langs.isNotEmpty()) add(anyOf(langs.sorted().map { Filters.f("lang", "=", it) }))
    if (platforms.isNotEmpty()) add(anyOf(platforms.sorted().map { Filters.f("platform", "=", it) }))
    if (lengths.isNotEmpty()) add(anyOf(lengths.sorted().map { Filters.f("length", "=", it) }))
    fromYear?.let { add(Filters.f("released", ">=", "$it-01-01")) }
    toYear?.let { add(Filters.f("released", "<=", "$it-12-31")) }
  }

  companion object {
    private fun combine(op: String, l: List<kotlinx.serialization.json.JsonElement>) = when (l.size) {
      0 -> null
      1 -> l[0]
      else -> kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive(op)) + l)
    }
    private fun anyOf(l: List<kotlinx.serialization.json.JsonElement>) = combine("or", l)!!
    fun and(l: List<kotlinx.serialization.json.JsonElement>) = combine("and", l)
  }
}
