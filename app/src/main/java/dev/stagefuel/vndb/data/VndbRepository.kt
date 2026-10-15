package dev.stagefuel.vndb.data

import dev.stagefuel.vndb.api.Character
import dev.stagefuel.vndb.api.CharacterFull
import dev.stagefuel.vndb.api.Producer
import dev.stagefuel.vndb.api.Quote
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

  suspend fun characters(vnId: String): List<Character> = pages { page ->
    api.query("character", Query(filters = Filters.vn(vnId), fields = Fields.CHARACTER_BRIEF, sort = "name", results = 100, page = page), Character.serializer())
  }

  /** The "search" sort is VNDB's search rank; anything else needs a query (or browses everything). */
  suspend fun browseVns(q: String, sort: String, reverse: Boolean, page: Int): QueryResult<Vn> {
    val id = Filters.vnId(q)
    val filter = when {
      id != null -> Filters.id(id)
      q.isNotBlank() -> Filters.search(q)
      else -> null
    }
    val s = if (sort == "searchrank" && (id != null || q.isBlank())) "rating" else sort
    return api.query("vn", Query(filters = filter, fields = Fields.VN_BRIEF, sort = s, reverse = reverse, results = 25, page = page), Vn.serializer())
  }

  suspend fun stats(): Stats = api.stats()
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
