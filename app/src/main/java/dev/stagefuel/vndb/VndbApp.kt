package dev.stagefuel.vndb

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dev.stagefuel.vndb.api.KanaClient
import dev.stagefuel.vndb.data.Settings
import dev.stagefuel.vndb.data.Updater
import dev.stagefuel.vndb.data.VndbRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class VndbApp : Application(), SingletonImageLoader.Factory {
  companion object {
    /** Overridden by screenshot tests to point at a local fixture server. */
    @JvmStatic var apiBase = "https://api.vndb.org/kana"
    /** Turned off by tests so they never reach GitHub. */
    @JvmStatic var checkUpdates = true
  }

  val http: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .build()
  }
  val settings: Settings by lazy { Settings(this) }
  val api: KanaClient by lazy { KanaClient(http, token = { settings.token() }, baseUrl = apiBase) }
  val repo: VndbRepository by lazy { VndbRepository(api) }
  val updater: Updater by lazy { Updater(http) }

  override fun newImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
      .components { add(OkHttpNetworkFetcherFactory(callFactory = { http })) }
      .build()
}
