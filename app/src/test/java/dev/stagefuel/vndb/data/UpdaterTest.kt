package dev.stagefuel.vndb.data

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterTest {
  @Test fun versionCompare() {
    assertTrue(Updater.isNewer("v0.2.0", "0.1.0"))
    assertTrue(Updater.isNewer("v0.1.10", "0.1.9"))
    assertTrue(Updater.isNewer("1.0", "0.9.9"))
    assertFalse(Updater.isNewer("v0.1.0", "0.1.0"))
    assertFalse(Updater.isNewer("v0.1.0", "0.2.0"))
    assertFalse(Updater.isNewer("v0.2.0-beta", "0.2.0"))
  }

  @Test fun releaseJsonDecodes() {
    val r = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(
      Updater.Release.serializer(),
      """{"tag_name":"v0.3.0","body":"notes","html_url":"x","assets":[{"name":"src.zip","browser_download_url":"a"},
         {"name":"vndb-0.3.0.apk","browser_download_url":"https://example/b.apk","size":5}],"extra":1}""",
    )
    assertEquals("https://example/b.apk", r.apk?.browser_download_url)
  }

  @Test fun missingRepoIsSilent() = runTest {
    val server = MockWebServer().apply { enqueue(MockResponse().setResponseCode(404)); start() }
    val http = OkHttpClient.Builder().addInterceptor { chain ->
      chain.proceed(chain.request().newBuilder().url(server.url(chain.request().url.encodedPath)).build())
    }.build()
    assertNull(Updater(http).check("0.1.0"))
    server.shutdown()
  }
}
