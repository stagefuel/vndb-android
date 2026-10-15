package dev.stagefuel.vndb.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Real Kana responses (v17, saved 2026-10-15) decode with the models and field lists we ship. */
class LiveFixtureTest {
  private fun <T> decode(name: String, s: kotlinx.serialization.KSerializer<T>) =
    KanaJson.decodeFromString(QueryResult.serializer(s), javaClass.getResource("/kana/$name")!!.readText())

  @Test fun vn() {
    val v = decode("live_vn_v17.json", Vn.serializer()).results.single()
    assertEquals("v17", v.id)
    assertTrue(v.tags.size > 50)
    assertTrue(v.staff.any { it.role == "scenario" })
    assertTrue(v.titles.any { it.main })
  }

  @Test fun releases() {
    val r = decode("live_release_v17.json", Release.serializer()).results
    assertTrue(r.size > 20)
    assertTrue(r.all { it.vns.any { v -> v.id == "v17" } })
  }

  @Test fun characters() {
    val c = decode("live_character_v17.json", Character.serializer()).results
    assertTrue(c.any { ch -> ch.vns.any { it.id == "v17" && it.role == "main" } })
  }
}
