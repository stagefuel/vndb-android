package dev.stagefuel.vndb.ui.web

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.stagefuel.vndb.ui.Nav
import dev.stagefuel.vndb.ui.Routes
import dev.stagefuel.vndb.ui.theme.LocalSkin

/**
 * vndb.org itself, for everything without a native screen (forums, profile edits, reviews…).
 * Signing in here sets the site's `vndb_auth` cookie, which CookieManager keeps across launches.
 * VN links jump to the native VN page; off-site links open in a custom tab.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(url: String, nav: Nav) {
  val skin = LocalSkin.current
  var loading by remember { mutableStateOf(true) }
  var canGoBack by remember { mutableStateOf(false) }
  var view by remember { mutableStateOf<WebView?>(null) }

  BackHandler(enabled = canGoBack) { view?.goBack() }

  Column(Modifier.fillMaxSize().background(skin.bodyBg)) {
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = skin.link, trackColor = skin.tabBg)
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        WebView(ctx).apply {
          settings.javaScriptEnabled = true
          settings.domStorageEnabled = true
          setBackgroundColor(android.graphics.Color.BLACK)
          CookieManager.getInstance().setAcceptCookie(true)
          webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView, req: WebResourceRequest): Boolean {
              if (!req.isForMainFrame) return false
              return when (val t = Routes.resolve(req.url.toString())) {
                is Routes.External -> { nav.open(t.url); true }
                is Routes.InApp -> if (t.route.startsWith("vn/") || t.route.startsWith("item/")) { nav.open(req.url.toString()); true } else false
              }
            }
            override fun onPageStarted(v: WebView, u: String?, favicon: Bitmap?) { loading = true }
            override fun onPageFinished(v: WebView, u: String?) {
              loading = false
              canGoBack = v.canGoBack()
              CookieManager.getInstance().flush()
            }
          }
          loadUrl(url)
          view = this
        }
      },
    )
  }
}
