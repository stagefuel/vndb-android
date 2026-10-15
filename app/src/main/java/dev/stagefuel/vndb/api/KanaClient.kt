package dev.stagefuel.vndb.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

class KanaException(val status: Int, message: String) : IOException(message)

/** Client for https://api.vndb.org/kana. [token] is read on every call so sign-in/out applies at once. */
class KanaClient(
  private val http: OkHttpClient,
  private val token: suspend () -> String?,
  private val baseUrl: String = "https://api.vndb.org/kana",
  private val limiter: RateLimiter = RateLimiter(),
) {
  private val jsonType = "application/json".toMediaType()

  suspend fun <T> query(endpoint: String, q: Query, item: KSerializer<T>): QueryResult<T> {
    val body = KanaJson.encodeToString(Query.serializer(), q)
    val text = send("POST", endpoint, body)
    return KanaJson.decodeFromString(QueryResult.serializer(item), text)
  }

  /** Checks [candidate] (or the stored token when null) and returns who it belongs to. */
  suspend fun authInfo(candidate: String? = null): AuthInfo =
    KanaJson.decodeFromString(AuthInfo.serializer(), sendUrl("GET", "$baseUrl/authinfo", null, candidate))

  suspend fun stats(): Stats = KanaJson.decodeFromString(Stats.serializer(), send("GET", "stats", null))

  suspend fun ulistLabels(user: String?): List<UlistLabel> {
    val path = "ulist_labels?fields=count" + (user?.let { "&user=$it" } ?: "")
    return KanaJson.decodeFromString(UlistLabels.serializer(), send("GET", path, null)).labels
  }

  /** `PATCH /ulist/<vid>`; only the keys present in [patch] change. Adds the VN if it's not on the list. */
  suspend fun patchUlist(vid: String, patch: JsonObject) {
    send("PATCH", "ulist/$vid", patch.toString())
  }

  suspend fun deleteUlist(vid: String) {
    send("DELETE", "ulist/$vid", null)
  }

  /** Release status: 0 unknown, 1 pending, 2 obtained, 3 on loan, 4 deleted. */
  suspend fun patchRlist(rid: String, status: Int) {
    send("PATCH", "rlist/$rid", """{"status":$status}""")
  }

  suspend fun deleteRlist(rid: String) {
    send("DELETE", "rlist/$rid", null)
  }

  /** `GET /user?q=` by name or id; null if no such user. */
  suspend fun user(q: String): UserInfo? {
    val url = "$baseUrl/user".toHttpUrl().newBuilder()
      .addQueryParameter("q", q)
      .addQueryParameter("fields", "lengthvotes,lengthvotes_sum")
      .build()
    val obj = KanaJson.parseToJsonElement(sendUrl("GET", url.toString(), null)) as JsonObject
    val u = obj[q] ?: return null
    if (u is kotlinx.serialization.json.JsonNull) return null
    return KanaJson.decodeFromJsonElement(UserInfo.serializer(), u)
  }

  private suspend fun send(method: String, path: String, body: String?): String =
    sendUrl(method, "$baseUrl/$path", body)

  private suspend fun sendUrl(method: String, url: String, body: String?, tokenOverride: String? = null): String {
    val tok = tokenOverride ?: token()
    var backoff = 2_000L
    repeat(4) { attempt ->
      limiter.acquire()
      val req = Request.Builder().url(url)
        .header("User-Agent", USER_AGENT)
        .apply { tok?.let { header("Authorization", "Token $it") } }
        .method(method, body?.toRequestBody(jsonType) ?: if (method == "PATCH") "{}".toRequestBody(jsonType) else null)
        .build()
      http.newCall(req).await().use { res ->
        val text = withContext(Dispatchers.IO) { res.body?.string().orEmpty() }
        when {
          res.isSuccessful -> return text
          res.code == 429 && attempt < 3 -> { delay(backoff); backoff *= 2 }
          else -> throw KanaException(res.code, errorMessage(res.code, text))
        }
      }
    }
    throw KanaException(429, "VNDB is rate-limiting requests; try again in a minute.")
  }

  private fun errorMessage(code: Int, text: String): String = when (code) {
    401 -> "VNDB rejected the API token."
    429 -> "VNDB is rate-limiting requests; try again in a minute."
    else -> "VNDB request failed ($code)" + text.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }?.let { ": ${it.take(200)}" }.orEmpty()
  }

  companion object {
    const val USER_AGENT = "vndb-android (github.com/stagefuel/vndb-android)"
  }
}

val KanaJson = Json {
  ignoreUnknownKeys = true
  explicitNulls = false
  encodeDefaults = false
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
  cont.invokeOnCancellation { cancel() }
  enqueue(object : Callback {
    override fun onResponse(call: Call, response: Response) = cont.resume(response) { _, _, _ -> response.close() }
    override fun onFailure(call: Call, e: IOException) = cont.resumeWithException(e)
  })
}
