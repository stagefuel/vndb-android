package dev.stagefuel.vndb.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import dev.stagefuel.vndb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** Checks GitHub releases for a newer APK; installing always goes through the system's confirm prompt. */
class Updater(private val http: OkHttpClient, private val repo: String = REPO) {
  companion object {
    const val REPO = "stagefuel/vndb-android"
    private val json = Json { ignoreUnknownKeys = true }

    /** `v0.2.0` > `0.1.10`? Compares dotted numbers; non-numeric suffixes are ignored. */
    fun isNewer(tag: String, current: String): Boolean {
      fun parts(v: String) = v.trimStart('v', 'V').substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
      val a = parts(tag); val b = parts(current)
      for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
      }
      return false
    }
  }

  @Serializable data class Asset(val name: String, val browser_download_url: String, val size: Long = 0)
  @Serializable data class Release(val tag_name: String, val name: String? = null, val body: String? = null,
                                   val html_url: String = "", val draft: Boolean = false, val prerelease: Boolean = false,
                                   val assets: List<Asset> = emptyList()) {
    val apk get() = assets.firstOrNull { it.name.endsWith(".apk") }
  }

  /** The latest release if it's newer than this build and has an APK, else null. Network errors → null. */
  suspend fun check(current: String = BuildConfig.VERSION_NAME): Release? = withContext(Dispatchers.IO) {
    runCatching {
      val req = Request.Builder().url("https://api.github.com/repos/$repo/releases/latest")
        .header("Accept", "application/vnd.github+json").build()
      http.newCall(req).execute().use { r ->
        if (!r.isSuccessful) return@use null
        json.decodeFromString(Release.serializer(), r.body!!.string())
      }
    }.getOrNull()?.takeIf { !it.draft && it.apk != null && isNewer(it.tag_name, current) }
  }

  /** Streams the APK into a PackageInstaller session and commits it; Android then asks the user to confirm. */
  suspend fun install(ctx: Context, rel: Release, progress: (Float) -> Unit) = withContext(Dispatchers.IO) {
    val asset = rel.apk ?: error("Release has no APK")
    val pi = ctx.packageManager.packageInstaller
    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
      setAppPackageName(ctx.packageName)
      if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
    }
    val id = pi.createSession(params)
    try {
      pi.openSession(id).use { s ->
        http.newCall(Request.Builder().url(asset.browser_download_url).build()).execute().use { r ->
          if (!r.isSuccessful) error("Download failed (${r.code})")
          val body = r.body!!
          val total = body.contentLength().takeIf { it > 0 } ?: asset.size
          s.openWrite("base.apk", 0, total.takeIf { it > 0 } ?: -1).use { out ->
            body.byteStream().use { inp ->
              val buf = ByteArray(64 * 1024); var done = 0L
              while (true) {
                val n = inp.read(buf); if (n < 0) break
                out.write(buf, 0, n); done += n
                if (total > 0) progress(done.toFloat() / total)
              }
            }
            s.fsync(out)
          }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val cb = PendingIntent.getBroadcast(ctx, id, Intent(ctx, InstallResult::class.java), flags)
        s.commit(cb.intentSender)
      }
    } catch (e: Throwable) {
      runCatching { pi.abandonSession(id) }
      throw e
    }
  }
}

/** Receives the session status; on PENDING_USER_ACTION it shows the system install prompt. */
class InstallResult : BroadcastReceiver() {
  override fun onReceive(ctx: Context, intent: Intent) {
    when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
      PackageInstaller.STATUS_PENDING_USER_ACTION -> {
        @Suppress("DEPRECATION")
        val confirm = (if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                       else intent.getParcelableExtra(Intent.EXTRA_INTENT)) ?: return
        ctx.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      }
      PackageInstaller.STATUS_SUCCESS -> Unit
      else -> android.widget.Toast.makeText(ctx, "Update failed: " +
        (intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "unknown error"), android.widget.Toast.LENGTH_LONG).show()
    }
  }
}
