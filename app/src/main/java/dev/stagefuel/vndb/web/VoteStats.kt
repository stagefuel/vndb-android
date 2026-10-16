package dev.stagefuel.vndb.web

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * The "User stats" box from a VN's page on vndb.org (vote distribution + recent votes). The Kana API has no
 * per-VN vote data, so this reads the public /vNNN page (robots.txt allows /v; only /t and /u are disallowed).
 * Markup per yorhel/vndb lib/VNWeb/VN/Page.pm stats_(): article#stats > div.votestats > table.votegraph,
 * table.recentvotes.
 */
data class VoteStats(
  /** Counts for scores 10 down to 1. */
  val buckets: List<Pair<Int, Int>>,
  val summary: List<String>,
  val recent: List<RecentVote>,
  /** From the recent-votes footer ("5 reviews »"); 0 when the VN has none. */
  val reviews: Int = 0,
) {
  data class RecentVote(val user: String?, val userId: String?, val vote: String, val date: String)
  val total get() = buckets.sumOf { it.second }
}

object VoteStatsParser {
  /** Null when the page has no stats box; empty buckets when nobody has voted yet. */
  fun parse(html: String): VoteStats? {
    val stats = Jsoup.parse(html).selectFirst("article#stats") ?: return null
    val graph = stats.selectFirst("table.votegraph") ?: return VoteStats(emptyList(), emptyList(), emptyList())
    val buckets = graph.select("tbody > tr, tr").filter { it.selectFirst("td.number") != null }.map { tr ->
      val n = tr.selectFirst("td.number")!!.text().trim().toIntOrNull() ?: 0
      val count = tr.selectFirst("td.graph")?.ownText()?.trim()?.toIntOrNull() ?: 0
      n to count
    }.distinctBy { it.first }
    val summary = graph.selectFirst("tfoot td")?.let { td ->
      td.html().split(Regex("<br\\s*/?>")).map { Jsoup.parse(it).text().trim() }.filter(String::isNotEmpty)
    }.orEmpty()
    val recent = stats.selectFirst("table.recentvotes")?.select("tr")
      ?.filter { it.parent()?.tagName() != "thead" && it.parent()?.tagName() != "tfoot" && it.select("td").size >= 3 }
      ?.map { tr -> recentRow(tr) }.orEmpty()
    val reviews = stats.selectFirst("table.recentvotes tfoot a[href*=/reviews]")?.text()
      ?.let { Regex("""(\d+)\s+reviews?""").find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: 0
    return VoteStats(buckets, summary, recent, reviews)
  }

  private fun recentRow(tr: Element): VoteStats.RecentVote {
    val td = tr.select("td")
    val a = td[0].selectFirst("a[href^=/u]")
    return VoteStats.RecentVote(
      user = a?.text() ?: td[0].text().trim().ifEmpty { null },
      userId = a?.attr("href")?.removePrefix("/")?.takeIf { Regex("u\\d+").matches(it) },
      vote = td[1].text().trim(),
      date = td[2].text().trim(),
    )
  }
}

class VoteStatsSource(private val http: OkHttpClient, private val base: String = "https://vndb.org") {
  suspend fun get(vnId: String): VoteStats? = withContext(Dispatchers.IO) {
    http.newCall(Request.Builder().url("$base/$vnId").build()).execute().use { r ->
      if (!r.isSuccessful) error("vndb.org returned ${r.code}")
      VoteStatsParser.parse(r.body!!.string())
    }
  }
}
