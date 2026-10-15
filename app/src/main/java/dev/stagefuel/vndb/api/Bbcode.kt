package dev.stagefuel.vndb.api

/** A run of text with the BBCode styles in effect. [href] is absolute (vndb.org ids are expanded). */
data class BbSpan(
  val text: String,
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val strike: Boolean = false,
  val spoiler: Boolean = false,
  val quote: Boolean = false,
  val code: Boolean = false,
  val href: String? = null,
)

/**
 * VNDB's description/post BBCode: [b] [i] [u] [s] [spoiler] [quote] [code] [raw] [url=..], plus bare
 * DB ids (v17, c123, t13.1) and bare URLs, which the site auto-links. Never produces HTML.
 */
object Bbcode {
  private val tagRe = Regex("""\[(/?)(url|spoiler|b|i|u|s|quote|raw|code)(?:=([^\]]*))?]""", RegexOption.IGNORE_CASE)
  private val autoRe = Regex("""\b([vrpcsgitwud][1-9]\d*(?:\.[1-9]\d*)?)\b|(https?://[^\s<>\[\]"]+)""")

  fun parse(src: String?): List<BbSpan> {
    val s = src.orEmpty()
    val out = mutableListOf<BbSpan>()
    var b = 0; var i = 0; var u = 0; var st = 0; var sp = 0; var q = 0; var code = 0
    var href: String? = null
    var raw = false
    var last = 0

    fun style(text: String, link: String? = href) = BbSpan(
      text, bold = b > 0, italic = i > 0, underline = u > 0, strike = st > 0, spoiler = sp > 0,
      quote = q > 0, code = code > 0, href = link,
    )

    fun push(text: String) {
      if (text.isEmpty()) return
      // Inside a link, raw or code block the text stays as-is; elsewhere ids and URLs are linked.
      if (href != null || raw || code > 0) { out += style(text); return }
      var pos = 0
      for (m in autoRe.findAll(text)) {
        if (m.range.first > pos) out += style(text.substring(pos, m.range.first))
        val id = m.groups[1]?.value
        out += if (id != null) style(id, "https://vndb.org/$id") else style("link", m.value)
        pos = m.range.last + 1
      }
      if (pos < text.length) out += style(text.substring(pos))
    }

    for (m in tagRe.findAll(s)) {
      val closing = m.groupValues[1] == "/"
      val tag = m.groupValues[2].lowercase()
      if (raw && !(closing && tag == "raw")) continue
      push(s.substring(last, m.range.first))
      last = m.range.last + 1
      val d = if (closing) -1 else 1
      when (tag) {
        "raw" -> raw = !closing
        "b" -> b = maxOf(0, b + d)
        "i" -> i = maxOf(0, i + d)
        "u" -> u = maxOf(0, u + d)
        "s" -> st = maxOf(0, st + d)
        "spoiler" -> sp = maxOf(0, sp + d)
        "quote" -> q = maxOf(0, q + d)
        "code" -> code = maxOf(0, code + d)
        "url" -> href = if (closing) null else normaliseUrl(m.groupValues[3])
      }
    }
    push(s.substring(last))
    return out
  }

  fun normaliseUrl(u: String?): String? = when {
    u.isNullOrBlank() -> null
    u.startsWith("/") -> "https://vndb.org$u"
    Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(u) -> u
    else -> null
  }

  /** Plain text with markup removed and spoilers dropped (for list previews). */
  fun plain(src: String?): String = parse(src).filterNot { it.spoiler }.joinToString("") { it.text }
}
