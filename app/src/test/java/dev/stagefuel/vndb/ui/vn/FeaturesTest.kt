package dev.stagefuel.vndb.ui.vn

import dev.stagefuel.vndb.api.FeedRelease
import dev.stagefuel.vndb.api.FeedVn
import dev.stagefuel.vndb.data.SearchFilter
import dev.stagefuel.vndb.ui.feed.group
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeaturesTest {
  @Test fun filterJson() {
    assertNull(SearchFilter.and(SearchFilter().parts()))
    val f = SearchFilter(langs = setOf("ja"), platforms = setOf("win", "lin"), lengths = setOf(3), fromYear = 2020)
    assertEquals(
      """["and",["lang","=","ja"],["or",["platform","=","lin"],["platform","=","win"]],["length","=",3],["released",">=","2020-01-01"]]""",
      SearchFilter.and(f.parts()).toString(),
    )
    assertEquals(5, f.count)
  }

  @Test fun lengthBuckets() {
    assertEquals(1, lengthBucket(90))
    assertEquals(2, lengthBucket(120))
    assertEquals(3, lengthBucket(10 * 60))
    assertEquals(4, lengthBucket(33 * 60 + 7))
    assertEquals(5, lengthBucket(50 * 60))
  }

  @Test fun feedGroupsByVn() {
    val a1 = FeedRelease("r1", "A regular", "2026-10-20", vns = listOf(FeedVn("v1", "A")))
    val a2 = FeedRelease("r2", "A limited", "2026-10-20", vns = listOf(FeedVn("v1", "A")))
    val b = FeedRelease("r3", "B", "2026-10-21", vns = listOf(FeedVn("v2", "B")))
    val g = group(listOf(a1, a2, b, FeedRelease("r4", "no vn")))
    assertEquals(listOf("v1", "v2"), g.map { it.vn.id })
    assertEquals(2, g[0].editions)
    assertEquals("r1", g[0].release.id)
  }
}
