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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.stagefuel.vndb.ui.Nav
import dev.stagefuel.vndb.ui.Routes
import dev.stagefuel.vndb.ui.common.LocalTabsBottom
import dev.stagefuel.vndb.ui.theme.LocalSkin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.JsonPrimitive

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
  val bottomTabs by rememberUpdatedState(LocalTabsBottom.current)
  val bodyBg by rememberUpdatedState(skin.bodyBg)
  // Re-style an open page when the setting changes.
  LaunchedEffect(bottomTabs) { view?.let { fitToPhone(it, bottomTabs, bodyBg) } }

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
            override fun onPageCommitVisible(v: WebView, u: String?) { fitToPhone(v, bottomTabs, bodyBg) }
            override fun onPageFinished(v: WebView, u: String?) {
              loading = false
              fitToPhone(v, bottomTabs, bodyBg)
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

private var fitCss: String? = null
private var bottomCss: String? = null

/**
 * vndb.org has no phone layout (fixed widths, a 2-column grid), so pages get a stylesheet that reflows
 * them to the screen width; with "Tabs: bottom" the page's main tabs are pinned above the nav bar.
 * Idempotent: the same <style> element is replaced on every call.
 */
internal fun fitToPhone(v: WebView, tabsBottom: Boolean, bodyBg: Color) {
  val assets = v.context.assets
  val fit = fitCss ?: assets.open("site-fit.css").bufferedReader().use { it.readText() }.also { fitCss = it }
  val bottom = bottomCss ?: assets.open("site-tabs-bottom.css").bufferedReader().use { it.readText() }.also { bottomCss = it }
  val bg = "#%06x".format(bodyBg.toArgb() and 0xffffff)
  val css = ":root{--app-bodybg:$bg}\n" + fit + if (tabsBottom) "\n" + bottom else ""
  v.evaluateJavascript(
    """(function(){
      var d=document, h=d.head||d.documentElement;
      if(!d.querySelector('meta[name=viewport]')){var m=d.createElement('meta');m.name='viewport';m.content='width=device-width, initial-scale=1';h.appendChild(m);}
      var s=d.getElementById('vndbapp-fit'); if(!s){s=d.createElement('style');s.id='vndbapp-fit';}
      s.textContent=${JsonPrimitive(css)}; h.appendChild(s);
      var n=d.querySelector('body > header > nav');
      if(d.body){ if(${tabsBottom} && n) d.body.style.setProperty('padding-bottom',(n.offsetHeight+12)+'px','important'); else d.body.style.removeProperty('padding-bottom'); }
    })()""",
    null,
  )
}
