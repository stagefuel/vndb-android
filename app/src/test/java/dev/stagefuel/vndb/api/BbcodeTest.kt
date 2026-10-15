package dev.stagefuel.vndb.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BbcodeTest {
  @Test fun plainText() {
    assertEquals(listOf(BbSpan("hello")), Bbcode.parse("hello"))
  }

  @Test fun nestedStyles() {
    val s = Bbcode.parse("a [b]bold [i]both[/i][/b] c")
    assertEquals(listOf("a ", "bold ", "both", " c"), s.map { it.text })
    assertTrue(s[1].bold && !s[1].italic)
    assertTrue(s[2].bold && s[2].italic)
  }

  @Test fun spoilerAndUrl() {
    val s = Bbcode.parse("[spoiler]x[/spoiler] [url=/v17]Ever17[/url] [url=https://example.com]e[/url]")
    assertTrue(s[0].spoiler)
    assertEquals("https://vndb.org/v17", s.first { it.text == "Ever17" }.href)
    assertEquals("https://example.com", s.first { it.text == "e" }.href)
  }

  @Test fun unsafeUrlDropped() {
    assertNull(Bbcode.parse("[url=javascript:alert(1)]x[/url]").single().href)
  }

  @Test fun autoLinksIdsAndUrls() {
    val s = Bbcode.parse("see v17 and t13.1 or https://a.b/c.")
    assertEquals("https://vndb.org/v17", s.first { it.text == "v17" }.href)
    assertEquals("https://vndb.org/t13.1", s.first { it.text == "t13.1" }.href)
    assertEquals("https://a.b/c.", s.first { it.text == "link" }.href)
  }

  @Test fun rawSuppressesTags() {
    val s = Bbcode.parse("[raw][b]v17[/b][/raw]")
    assertEquals("[b]v17[/b]", s.joinToString("") { it.text })
    assertTrue(s.none { it.bold || it.href != null })
  }

  @Test fun plainDropsSpoilers() {
    assertEquals("a  b", Bbcode.plain("a [spoiler]secret[/spoiler] b"))
  }

  @Test fun unbalancedClosersIgnored() {
    assertEquals("x", Bbcode.parse("[/b]x").single().text)
    assertTrue(!Bbcode.parse("[/b]x").single().bold)
  }
}
