package dev.stagefuel.vndb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.ui.AppNav
import dev.stagefuel.vndb.ui.theme.VndbTheme

class MainActivity : ComponentActivity() {
  /** vndb.org link the app was opened with (deep link); consumed by AppNav. */
  private val deepLink = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val app = application as VndbApp
    if (savedInstanceState == null) deepLink.value = intent?.data?.toString()
    setContent {
      val prefs by app.settings.prefs.collectAsStateWithLifecycle(initialValue = Prefs())
      VndbTheme(prefs.skin) {
        androidx.compose.runtime.CompositionLocalProvider(dev.stagefuel.vndb.ui.common.LocalTabsBottom provides prefs.tabsBottom) {
        AppNav(app, prefs, startUrl = deepLink.value, onStartUrlUsed = { deepLink.value = null }, openExternal = ::openExternal)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    deepLink.value = intent.data?.toString()
  }

  private fun openExternal(url: String) {
    CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
  }
}
