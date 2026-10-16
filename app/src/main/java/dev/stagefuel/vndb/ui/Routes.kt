package dev.stagefuel.vndb.ui

/** Maps vndb.org URLs onto in-app screens; anything without a native screen opens in the web view. */
object Routes {
  const val SEARCH = "search"
  const val LIST = "list"
  const val FORUMS = "forums"
  const val SETTINGS = "settings"
  const val VN = "vn/{id}"
  const val ITEM = "item/{id}"
  const val USER_LIST = "ulist/{uid}?name={name}"
  const val WEB = "web?url={url}"

  fun vn(id: String) = "vn/$id"
  fun item(id: String) = "item/$id"
  fun userList(uid: String, name: String) = "ulist/$uid?name=" + encode(name)
  fun web(url: String) = "web?url=" + encode(url)

  /** Percent-encodes everything but RFC 3986 unreserved chars (what Uri.encode does, minus the Android dependency). */
  fun encode(s: String): String = buildString {
    for (b in s.toByteArray(Charsets.UTF_8)) {
      val c = b.toInt() and 0xff
      if (c in 'a'.code..'z'.code || c in 'A'.code..'Z'.code || c in '0'.code..'9'.code || c.toChar() in "-._~") append(c.toChar())
      else append('%').append("%02X".format(c))
    }
  }

  private val vnUrl = Regex("""^https?://(?:www\.)?vndb\.org/(v\d+)(?:/(?:chars|releases|staff|tags)?)?(?:[#?].*)?$""", RegexOption.IGNORE_CASE)
  /** Characters, staff, producers, tags, traits and releases have native pages (not their sub-pages). */
  private val itemUrl = Regex("""^https?://(?:www\.)?vndb\.org/([cspgir]\d+)/?(?:#.*)?$""", RegexOption.IGNORE_CASE)
  private val siteUrl = Regex("""^https?://(?:www\.)?vndb\.org(?:/.*)?$""", RegexOption.IGNORE_CASE)

  sealed interface Target
  data class InApp(val route: String) : Target
  data class External(val url: String) : Target

  fun resolve(url: String): Target {
    vnUrl.find(url)?.let { return InApp(vn(it.groupValues[1].lowercase())) }
    itemUrl.find(url)?.let { return InApp(item(it.groupValues[1].lowercase())) }
    if (siteUrl.matches(url)) return InApp(web(url))
    return External(url)
  }
}
