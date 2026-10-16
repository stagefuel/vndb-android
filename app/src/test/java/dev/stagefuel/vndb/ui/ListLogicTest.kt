package dev.stagefuel.vndb.ui

import dev.stagefuel.vndb.ui.common.formatVote
import dev.stagefuel.vndb.ui.list.parseVote
import dev.stagefuel.vndb.ui.theme.Skins
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListLogicTest {
  @Test fun votes() {
    assertEquals(80, parseVote("8").getOrNull())
    assertEquals(75, parseVote("7,5").getOrNull())
    assertEquals(100, parseVote("10").getOrNull())
    assertNull(parseVote("").getOrThrow())
    assertTrue(parseVote("0.5").isFailure)
    assertTrue(parseVote("11").isFailure)
    assertTrue(parseVote("abc").isFailure)
    assertEquals("8", formatVote(80))
    assertEquals("7.5", formatVote(75))
    assertEquals("-", formatVote(null))
  }

  @Test fun routes() {
    assertEquals(Routes.InApp("vn/v17"), Routes.resolve("https://vndb.org/v17"))
    assertEquals(Routes.InApp("vn/v17"), Routes.resolve("https://vndb.org/V17/chars#chars"))
    // Sub-pages without a native screen (votes, edit, history…) stay on the site.
    assertEquals(Routes.InApp("web?url=https%3A%2F%2Fvndb.org%2Fv17%2Fvotes"), Routes.resolve("https://vndb.org/v17/votes"))
    assertEquals(Routes.InApp("web?url=https%3A%2F%2Fvndb.org%2Ft%2Fdb"), Routes.resolve("https://vndb.org/t/db"))
    assertEquals(Routes.External("https://example.com/v17"), Routes.resolve("https://example.com/v17"))
    assertTrue(Routes.isImage("https://t.vndb.org/sf/12/34512.jpg"))
    assertTrue(Routes.isImage("https://i.imgur.com/abc.PNG?x=1"))
    assertFalse(Routes.isImage("https://vndb.org/v17"))
    assertFalse(Routes.isImage("https://example.com/page.html"))
  }

  @Test fun skinsTableComplete() {
    assertEquals(23, Skins.all.size)
    assertEquals(Skins.all.size, Skins.all.map { it.id }.toSet().size)
    assertEquals("angel", Skins.byId("nope").id)
    val angel = Skins.byId("angel")
    assertTrue(angel.isDark)
    assertEquals(0xbc / 255f, angel.boxBg.alpha, 0.01f)
    assertTrue(!Skins.byId("air").isDark)
  }
}
