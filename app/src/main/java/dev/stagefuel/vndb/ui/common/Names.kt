package dev.stagefuel.vndb.ui.common

/** Display names for VNDB's enum codes (from the site's lib/VNDB/Types.pm, trimmed to common ones). */
object Names {
  val languages = mapOf(
    "ja" to "Japanese", "en" to "English", "zh" to "Chinese", "zh-Hans" to "Chinese (simplified)",
    "zh-Hant" to "Chinese (traditional)", "ko" to "Korean", "ru" to "Russian", "es" to "Spanish",
    "de" to "German", "fr" to "French", "it" to "Italian", "pt-br" to "Portuguese (Brazil)",
    "pt-pt" to "Portuguese (Portugal)", "pl" to "Polish", "vi" to "Vietnamese", "uk" to "Ukrainian",
    "id" to "Indonesian", "th" to "Thai", "tr" to "Turkish", "ar" to "Arabic", "cs" to "Czech",
    "hu" to "Hungarian", "nl" to "Dutch", "sv" to "Swedish", "fi" to "Finnish", "da" to "Danish",
    "no" to "Norwegian", "el" to "Greek", "he" to "Hebrew", "ro" to "Romanian", "ca" to "Catalan",
    "ms" to "Malay", "tl" to "Tagalog", "fa" to "Persian", "bg" to "Bulgarian", "hr" to "Croatian",
    "lt" to "Lithuanian", "lv" to "Latvian", "sk" to "Slovak", "sl" to "Slovene", "ta" to "Tamil",
    "hi" to "Hindi", "eo" to "Esperanto", "la" to "Latin", "eu" to "Basque", "be" to "Belarusian",
    "bs" to "Bosnian", "ck" to "Cherokee", "et" to "Estonian", "gl" to "Galician", "ga" to "Irish",
    "iu" to "Inuktitut", "kk" to "Kazakh", "mk" to "Macedonian", "ne" to "Nepali", "gd" to "Scottish Gaelic",
    "sr" to "Serbian", "ur" to "Urdu",
  )
  /** Release media (Kana `medium` enum). */
  val media = mapOf(
    "blr" to "Blu-ray disc", "mrt" to "Cartridge", "cas" to "Cassette tape", "cd" to "CD", "dc" to "Download card",
    "dvd" to "DVD", "flp" to "Floppy", "gdr" to "GD-ROM", "in" to "Internet download", "mem" to "Memory card",
    "nod" to "Nintendo optical disc", "umd" to "UMD", "otc" to "Other",
  )
  val platforms = mapOf(
    "win" to "Windows", "lin" to "Linux", "mac" to "Mac OS", "web" to "Website", "and" to "Android",
    "ios" to "iOS", "swi" to "Nintendo Switch", "sw2" to "Nintendo Switch 2", "ps4" to "PlayStation 4", "ps5" to "PlayStation 5",
    "psv" to "PlayStation Vita", "psp" to "PlayStation Portable", "ps3" to "PlayStation 3",
    "ps2" to "PlayStation 2", "ps1" to "PlayStation", "xb1" to "Xbox One", "xxs" to "Xbox X/S",
    "xb3" to "Xbox 360", "xbo" to "Xbox", "n3d" to "Nintendo 3DS", "nds" to "Nintendo DS",
    "wii" to "Nintendo Wii", "wiu" to "Wii U", "dvd" to "DVD player", "dos" to "DOS", "sat" to "Sega Saturn",
    "drc" to "Dreamcast", "pce" to "PC Engine", "pcf" to "PC-FX", "p98" to "PC-98", "p88" to "PC-88",
    "x68" to "X68000", "fmt" to "FM Towns", "msx" to "MSX", "mob" to "Other mobile", "vnd" to "VNDS", "bdp" to "Blu-ray player", "gba" to "Game Boy Advance", "gbc" to "Game Boy Color", "smd" to "Sega Mega Drive", "scd" to "Sega Mega-CD", "sfc" to "Super Famicom", "nes" to "Famicom", "tdo" to "3DO", "fm7" to "FM-7", "fm8" to "FM-8", "x1s" to "Sharp X1", "pc6" to "PC-6001", "mtx" to "MTX", "oth" to "Other",
  )
  val relations = mapOf(
    "seq" to "Sequel", "preq" to "Prequel", "set" to "Same setting", "alt" to "Alternative version",
    "char" to "Shares characters", "side" to "Side story", "par" to "Parent story", "ser" to "Same series",
    "fan" to "Fandisc", "orig" to "Original game",
  )
  val relationOrder = listOf("preq", "seq", "fan", "side", "par", "ser", "set", "alt", "char", "orig")
  val lengths = mapOf(1 to "Very short (< 2 hours)", 2 to "Short (2 - 10 hours)", 3 to "Medium (10 - 30 hours)", 4 to "Long (30 - 50 hours)", 5 to "Very long (> 50 hours)")
  val devstatus = mapOf(0 to "Finished", 1 to "In development", 2 to "Cancelled")
  val staffRoles = mapOf(
    "scenario" to "Scenario", "director" to "Director", "chardesign" to "Character design", "art" to "Artist",
    "music" to "Composer", "songs" to "Vocals", "translator" to "Translator", "editor" to "Editor",
    "qa" to "Quality assurance", "staff" to "Staff",
  )
  val staffRoleOrder = listOf("director", "scenario", "chardesign", "art", "music", "songs", "translator", "editor", "qa", "staff")
  val charRoles = mapOf("main" to "Protagonist", "primary" to "Main characters", "side" to "Side characters", "appears" to "Makes an appearance")
  val charRoleOrder = listOf("main", "primary", "side", "appears")
  val tagCats = mapOf("cont" to "content", "ero" to "sexual", "tech" to "technical")
  val rtypes = mapOf("trial" to "trial", "partial" to "partial", "complete" to "")
  val voiced = mapOf(1 to "Not voiced", 2 to "Only ero scenes voiced", 3 to "Partially voiced", 4 to "Fully voiced")
  /** rlist status codes. */
  val releaseStatus = listOf("Unknown", "Pending", "Obtained", "On loan", "Deleted")

  fun lang(code: String) = languages[code] ?: code
  fun platform(code: String) = platforms[code] ?: code

  fun minutes(m: Int): String = if (m >= 60) "${m / 60}h${if (m % 60 != 0) "%02dm".format(m % 60) else ""}" else "${m}m"
}
