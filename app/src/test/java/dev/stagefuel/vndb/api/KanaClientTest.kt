package dev.stagefuel.vndb.api

import dev.stagefuel.vndb.data.ListEdit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class KanaClientTest {
  private val server = MockWebServer()
  private var token: String? = null
  private lateinit var api: KanaClient

  private fun fixture(name: String) = javaClass.getResource("/kana/$name")!!.readText()

  @Before fun setUp() {
    server.start()
    api = KanaClient(OkHttpClient(), token = { token }, baseUrl = server.url("/kana").toString().trimEnd('/'))
  }

  @After fun tearDown() = server.shutdown()

  @Test fun vnQueryDecodesAndSendsFields() = runTest {
    server.enqueue(MockResponse().setBody(fixture("vn_v17.json")))
    val r = api.query("vn", Query(filters = Filters.id("v17"), fields = Fields.VN_FULL, results = 1), Vn.serializer())
    val v = r.results.single()
    assertEquals("Ever17 -the out of infinity-", v.title)
    assertEquals(2, v.tags.size)
    assertEquals("seq", v.relations.single().relation)
    assertEquals(1.0, v.screenshots.single().sexual, 0.0)
    val req = server.takeRequest()
    assertEquals("/kana/vn", req.path)
    assertEquals("POST", req.method)
    val body = req.body.readUtf8()
    assertTrue(body.contains(""""filters":["id","=","v17"]"""))
    assertTrue(body.contains(""""fields":"id,title"""))
    assertNull(req.getHeader("Authorization"))
  }

  @Test fun tokenSentWhenSignedIn() = runTest {
    token = "abc-def"
    server.enqueue(MockResponse().setBody("""{"id":"u1","username":"yorhel","permissions":["listread","listwrite"]}"""))
    val a = api.authInfo()
    assertEquals("u1", a.id)
    assertEquals(listOf("listread", "listwrite"), a.permissions)
    assertEquals("Token abc-def", server.takeRequest().getHeader("Authorization"))
  }

  @Test fun candidateTokenOverridesStored() = runTest {
    token = "stored"
    server.enqueue(MockResponse().setBody("""{"id":"u2","username":"x"}"""))
    api.authInfo("candidate")
    assertEquals("Token candidate", server.takeRequest().getHeader("Authorization"))
  }

  @Test fun retriesOn429() = runTest {
    server.enqueue(MockResponse().setResponseCode(429))
    server.enqueue(MockResponse().setBody("""{"results":[],"more":false}"""))
    val r = api.query("vn", Query(fields = "id"), Vn.serializer())
    assertEquals(0, r.results.size)
    assertEquals(2, server.requestCount)
  }

  @Test fun errorsCarryStatus() = runTest {
    server.enqueue(MockResponse().setResponseCode(401).setBody("Unauthorized"))
    try {
      api.authInfo("bad"); fail()
    } catch (e: KanaException) {
      assertEquals(401, e.status)
    }
  }

  @Test fun patchUlistBody() = runTest {
    server.enqueue(MockResponse().setResponseCode(204))
    api.patchUlist("v17", ListEdit(vote = 85, notes = "n", started = "2026-01-02", finished = "", labels = setOf(2, 7, 0, 10)).toPatch(null))
    val req = server.takeRequest()
    assertEquals("PATCH", req.method)
    assertEquals("/kana/ulist/v17", req.path)
    assertEquals("""{"vote":85,"notes":"n","started":"2026-01-02","labels_set":[2,10]}""", req.body.readUtf8())
  }

  @Test fun ulistDecodes() = runTest {
    server.enqueue(MockResponse().setBody(fixture("ulist.json")))
    val r = api.query("ulist", Query(user = "u1", fields = Fields.ULIST), UlistEntry.serializer())
    val e = r.results.single()
    assertEquals(90, e.vote)
    assertEquals(listOf(2, 7), e.labels.map { it.id })
    assertEquals(2, e.releases.single().listStatus)
    assertTrue(r.more)
    assertTrue(server.takeRequest().body.readUtf8().contains(""""user":"u1""""))
  }

  @Test fun labelsWithCounts() = runTest {
    server.enqueue(MockResponse().setBody("""{"labels":[{"id":1,"label":"Playing","private":false,"count":3},{"id":10,"label":"Mine","private":true}]}"""))
    val l = api.ulistLabels("u1")
    assertEquals(3, l[0].count)
    assertTrue(l[1].private)
    assertEquals("/kana/ulist_labels?fields=count&user=u1", server.takeRequest().path)
  }
}
