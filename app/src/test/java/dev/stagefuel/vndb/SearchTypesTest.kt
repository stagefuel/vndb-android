package dev.stagefuel.vndb

import dev.stagefuel.vndb.data.SearchTypes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTypesTest {
  private fun o(s: String) = Json.parseToJsonElement(s).jsonObject

  @Test fun summaries() {
    val rel = o("""{"id":"r1","title":"Saberface","alttitle":"セイバー","released":"2022-12-13","platforms":["win"]}""")
    assertEquals("Saberface", SearchTypes.title(rel))
    assertEquals("セイバー", SearchTypes.original(rel))
    assertEquals("2022-12-13 · Windows", SearchTypes.byKey.getValue("release").sub(rel))
    val prod = o("""{"id":"p1","name":"SABER FISH","original":"セイバーフィッシュ","type":"ng","lang":"ja"}""")
    assertEquals("Amateur group · Japanese", SearchTypes.byKey.getValue("producer").sub(prod))
    val trait = o("""{"id":"i70","name":"Dao","group_name":"Items","char_count":26}""")
    assertEquals("Items · 26 characters", SearchTypes.byKey.getValue("trait").sub(trait))
    val staff = o("""{"id":"s1","name":"Saber","original":null,"lang":"ru"}""")
    assertEquals(null, SearchTypes.original(staff))
  }
}
