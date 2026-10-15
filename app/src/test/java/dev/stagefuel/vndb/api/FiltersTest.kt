package dev.stagefuel.vndb.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FiltersTest {
  @Test fun vnIds() {
    assertEquals("v17", Filters.vnId("v17"))
    assertEquals("v17", Filters.vnId("V17"))
    assertEquals("v17", Filters.vnId("https://vndb.org/v17"))
    assertEquals("v17", Filters.vnId("vndb.org/v17/chars"))
    assertNull(Filters.vnId("ever17"))
    assertNull(Filters.vnId("v17x"))
  }

  @Test fun nestedFilterJson() {
    assertEquals("""["vn","=",["id","=","v17"]]""", Filters.vn("v17").toString())
    assertEquals("""["label","=",5]""", Filters.label(5).toString())
  }
}
