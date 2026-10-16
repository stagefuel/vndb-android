package dev.stagefuel.vndb.shots

import android.graphics.Bitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.stagefuel.vndb.MainActivity
import dev.stagefuel.vndb.VndbApp
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.AfterClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders real screens against fixture data and writes PNGs to build/shots/ for eyeballing
 * against vndb.org. Not a pixel-diff test; it fails only if a screen crashes or text is missing.
 */
/** Same fixtures as [ScreenshotTest], but a light skin (AIR) on a narrow 320dp phone: catches overflow/contrast. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w320dp-h680dp-xhdpi")
@OptIn(ExperimentalTestApi::class)
class ScreenshotNarrowTest {
  companion object {
    private val server = MockWebServer().apply {
      dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          val body = request.body.readUtf8()
          if (request.path == "/site/v17") return MockResponse().setBody(ScreenshotNarrowTest::class.java.getResource("/site/v17_stats.html")!!.readText())
          val file = when {
            request.path!!.endsWith("/vn") -> "live_vn_v17.json"
            request.path!!.endsWith("/release") -> "live_release_v17.json"
            request.path!!.endsWith("/character") -> "live_character_v17.json"
            else -> return MockResponse().setResponseCode(404)
          }
          return MockResponse().setBody(ScreenshotNarrowTest::class.java.getResource("/kana/$file")!!.readText())
        }
      }
      start()
      VndbApp.apiBase = url("/kana").toString().trimEnd('/')
      VndbApp.checkUpdates = false
      VndbApp.siteBase = url("/site").toString().trimEnd('/')
      VndbApp.testSkin = "air"
    }

    @AfterClass @JvmStatic fun stop() { server.shutdown(); VndbApp.testSkin = null }
  }

  @get:Rule val rule = createAndroidComposeRule<MainActivity>()

  private val out = File("build/shots").apply { mkdirs() }

  private fun shot(name: String) {
    rule.waitForIdle()
    val view = rule.activity.window.decorView
    val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    rule.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
    File(out, "n-$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
  }

  @Test fun narrowLight() {
    shot("01-start")
    rule.onNode(hasSetTextAction()).performTextInput("ever17")
    rule.onNode(hasSetTextAction()).performImeAction()
    rule.waitUntilAtLeastOneExists(androidx.compose.ui.test.hasText("Ever17 -the out of infinity-"), 10_000)
    shot("02-results")
    rule.onNodeWithText("Ever17 -the out of infinity-").performClick()
    rule.waitUntilAtLeastOneExists(androidx.compose.ui.test.hasText("Length"), 10_000)
    shot("03-vn-main")
    rule.onAllNodes(androidx.compose.ui.test.hasScrollToNodeAction())[0].performScrollToNode(androidx.compose.ui.test.hasText("Relations"))
    shot("03b-vn-main-lower")
    rule.onAllNodes(androidx.compose.ui.test.hasScrollToNodeAction())[0].performScrollToIndex(0)
    for ((tab, wait) in listOf("tags" to "summary", "releases" to "Japanese (", "characters" to "Protagonist", "staff" to "Scenario")) {
      rule.onNodeWithText(tab).performClick()
      rule.waitUntilAtLeastOneExists(androidx.compose.ui.test.hasText(wait, substring = true), 10_000)
      shot("04-$tab")
    }
    rule.onNodeWithText("characters").performClick()
    rule.waitUntilAtLeastOneExists(androidx.compose.ui.test.hasText("Kuranari Takeshi"), 10_000)
    rule.onAllNodes(androidx.compose.ui.test.hasText("Kuranari Takeshi"))[0].performClick()
    rule.waitUntilAtLeastOneExists(androidx.compose.ui.test.hasText("open on vndb.org"), 10_000)
    shot("05-character")
    rule.onNodeWithText("Me").performClick()
    rule.waitUntilAtLeastOneExists(androidx.compose.ui.test.hasText("Display"), 5_000)
    shot("06-me")
    rule.onNodeWithText("My list").performClick()
    shot("07-list-signed-out")
  }
}
