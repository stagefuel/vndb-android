package dev.stagefuel.vndb

import dev.stagefuel.vndb.web.VoteStatsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoteStatsTest {
  private fun fixture(name: String) = javaClass.getResource("/site/$name")!!.readText()

  @Test fun parsesGraphSummaryAndRecentVotes() {
    val s = VoteStatsParser.parse(fixture("v17_stats.html"))!!
    assertEquals((10 downTo 1).toList(), s.buckets.map { it.first })
    assertEquals(3021, s.buckets.first().second)
    assertEquals(50, s.buckets.last().second)
    assertEquals(8792, s.total)
    assertEquals(listOf("8792 votes (rank 120)", "8.45 average (8.45 weighted, rank 18)"), s.summary)
    assertEquals(2, s.recent.size)
    assertEquals(5, s.reviews)
    assertEquals("Some One", s.recent[0].user)
    assertEquals("u42", s.recent[0].userId)
    assertEquals("8.5", s.recent[0].vote)
    assertEquals("2026-10-14", s.recent[0].date)
    assertNull(s.recent[1].userId)
    assertEquals("hidden", s.recent[1].user ?: "hidden")
  }

  @Test fun noVotesYet() {
    val s = VoteStatsParser.parse("""<article id="stats"><h1>User stats</h1><p>Nobody has voted on this visual novel yet...</p></article>""")!!
    assertEquals(0, s.total)
  }

  @Test fun noStatsBox() = assertNull(VoteStatsParser.parse("<html><body>hidden</body></html>"))
}
