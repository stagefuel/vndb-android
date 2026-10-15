package dev.stagefuel.vndb.data

import dev.stagefuel.vndb.api.KanaClient
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Round-trip against the real API, opt-in: VNDB_LIVE_TOKEN (listread+listwrite) and VNDB_LIVE_VN, a VN that is
 * NOT on the token owner's list. Adds it, edits it through the same code the app uses, then deletes it, so the
 * list ends as it started. Refuses to run if the VN is already listed (it would destroy that entry).
 */
class LiveListTest {
  private val token = System.getenv("VNDB_LIVE_TOKEN")
  private val vn = System.getenv("VNDB_LIVE_VN")

  @Test fun addEditRemove() = runBlocking {
    assumeTrue("live test disabled", !token.isNullOrBlank() && !vn.isNullOrBlank())
    val api = KanaClient(OkHttpClient(), token = { token })
    val repo = VndbRepository(api)
    val me = api.authInfo()
    check(repo.ulistEntry(me.id, vn!!) == null) { "$vn is already on the list; pick another VN" }
    try {
      repo.saveEntry(vn, ListEdit(vote = 75, notes = "live round-trip", started = "2026-01-02", finished = null, labels = setOf(1, 10)), null)
      val e = repo.ulistEntry(me.id, vn)!!
      assertEquals(75, e.vote)
      assertEquals("live round-trip", e.notes)
      assertEquals("2026-01-02", e.started)
      assertNull(e.finished)
      assertEquals(setOf(1, 7, 10), e.labels.map { it.id }.toSet())

      // Change labels without touching the vote: Voted (7) must survive.
      repo.saveEntry(vn, ListEdit(vote = 75, notes = "", started = "2026-01-02", finished = "2026-02-03", labels = setOf(2)), e)
      val e2 = repo.ulistEntry(me.id, vn)!!
      assertEquals("2026-02-03", e2.finished)
      assertEquals("", e2.notes.orEmpty())
      assertEquals(setOf(2, 7), e2.labels.map { it.id }.toSet())

      // Clearing the vote drops Voted.
      repo.saveEntry(vn, ListEdit(vote = null, notes = "", started = "2026-01-02", finished = "2026-02-03", labels = setOf(2)), e2)
      val e3 = repo.ulistEntry(me.id, vn)!!
      assertNull(e3.vote)
      assertEquals(setOf(2), e3.labels.map { it.id }.toSet())

      val rid = repo.releases(vn).first().id
      repo.setReleaseStatus(rid, 2)
      assertEquals(2, repo.ulistEntry(me.id, vn)!!.releases.first { it.id == rid }.listStatus)
      repo.setReleaseStatus(rid, null)
      assertEquals(emptyList<Any>(), repo.ulistEntry(me.id, vn)!!.releases)
    } finally {
      repo.removeEntry(vn)
    }
    assertNull(repo.ulistEntry(me.id, vn))
    println("LIVE OK for $vn")
  }
}
