package dev.stagefuel.vndb.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour variables of every vndb.org skin, from css/skins/<id>.sass in yorhel/vndb (2026-03).
 * Column order: bodybg boxbg maintext grayedout standout link statok statnok footer maintitle boxtitle
 * alttitle tabbg secbg secborder border diffadd diffdel warnbg warnborder noticebg noticeborder.
 */
private const val SKIN_TABLE = """
air|AIR (sky blue)|#ffffff|#ccddeebc|#222222|#77a9dd|#bb1511|#226588|#33ff38|#ff3833|#226588|#226588|#77a9dd|#000000|#ddeeff|#bed8f2|#5677cc|#77a9dd|#aaccbc|#ccaabb|#ccaabb|#ff3833|#aaccbc|#33ff38
angel|Angelic Serenade (dark blue)|#000|#071c30bc|#ddd|#37a|#e44|#7bd|#0c0|#c00|#247|#135|#258|#fff|#012|#0d2741|#35A|#258|#354|#534|#534|#c00|#052d2e|#227688
aselia_01|Eien no Aselia (falu red)|#8a3c35|#ac759595|#ffffff|#eee388|#e96e73|#4a2b33|#ffcbee|#e96e73|#f4f1e6|#fce5e5|#f4e1b5|#ed9f92|#ac7595|#8a3c35|#e86a76|#f1c0b2|#833d71|#6c3a55|#6b3b51|#e37192|#cc8487|#975352
carnevale|Gekkou no Carnevale (black)|#030708|#12162290|#b9c7ae|#6f7a68|#f5aaaa|#ffffff|#dab0fc|#768b78|#b9c7ae|#b9c7ae|#c7d3bf|#6f8578|#1f272a|#121622|#ffffff|#b9c7ae|#76a3b8|#354554|#5d182b|#829076|#263a45|#21343a
eiel|ELEL (peach-orange)|#fdd298|#36214299|#f8cb8a|#f26a7e|#ff435f|#ffffff|#ffcbee|#ff435f|#362142|#676082|#ffcbee|#f26a7e|#5a3a49|#584563|#c42b5a|#362142|#2bc88b|#ca4a4d|#c42b5a|#f8cb8a|#b48ab2|#f8cb8a
ever17_01|Ever17 (bondi blue)|#00879b|#d1ebee77|#3c363f|#785a5b|#966932|#013f7a|#9c4b00|#eb0e4c|#e2cfa6|#f0a260|#f0a260|#ff9013|#81d5ea|#bcd1e4|#00627e|#016c80|#f1a361|#9a7071|#c43f5c|#951924|#fefbf6|#f1a360
fate_01|Fate/stay night (seal brown)|#200b0c|#4d3c3abc|#ab928d|#916858|#72322b|#eee0da|#efdab9|#b07a6b|#642924|#200b0c|#d56243|#d7926e|#130504|#7c362f|#33261d|#9e4a47|#4d2c25|#6f5347|#882f27|#fbf1e0|#882f27|#fbf1e0
fate_02|Fate/stay night (pale carmine)|#ac4b47|#c6093366|#fcfbfb|#ff9d82|#ffc98f|#48b2c1|#ff7682|#9f72ff|#fffed5|#ffffff|#ffffff|#c4a9a7|#723033|#792447|#a68483|#452d2c|#3d3231|#01023f|#772446|#35152d|#5c151f|#460015
grey|Touhou (grey)|#fff|#ddddddcc|#222|#666|#500|#005|#050|#500|#999|#ccc|#444|#000|#ddd|#ccc|#000|#999|#cfc|#fcc|#fcc|#c00|#cfc|#0c0
higanbana|Higanbana no Saku Yoru ni (maroon)|#000|#331111bc|#ddd|#822|#ec4|#d77|#0c0|#c00|#722|#511|#822|#fff|#200|#410d0d|#a33|#822|#354|#534|#534|#c00|#354|#0c0
higu|Higurashi no Naku Koro ni (orange)|#f89e7e|#f7c7bb80|#2c1a18|#8c5b3b|#c24857|#3c549c|#8c6290|#824e52|#2e2536|#e5d3e1|#1b1b51|#35346d|#9b8587|#f7c7bb|#3c549c|#2c1a18|#c2bcc6|#8c5b3b|#ae6866|#612028|#9b8587|#dc9b7f
lb_02|Little Busters! (lemon chiffon)|#fff4d4|#d1ebee99|#3c363f|#785a5b|#eb0e4c|#04529b|#d87417|#eb0e4c|#eb0e4c|#fffeff|#ff9013|#f0a260|#9aa4d7|#bcd1e4|#966932|#016c80|#3689b5|#b7adc6|#e97a9a|#f1a360|#fefbf6|#951924
lb|Little Busters! (pink)|#fff|#f7b6edcc|#408|#670159|#e44|#a2d|#0c0|#c00|#f76ee2|#f78de7|#670159|#5328a7|#f78de7|#f78de7|#670159|#f76ee2|#cfc|#fcc|#fff|#c00|#f7b6ed|#670159
moeai|Moe AI Shit (pink)|#f7b6ed|#ffffffcc|#2e005d|#b894dc|#e44|#8a37b4|#0c0|#c00|#f76ee2|#8a37b4|#670159|#5328a7|#ffd1fc|#ffdffd|#670159|#f76ee2|#cfc|#fcc|#f7b6ed|#670159|#fff|#c00
phorni|Symphonic Rain (ivory)|#fff|#faf8efaa|#000|#a19089|#ad4114|#2a2e80|#325800|#a63d6a|#247|#694954|#30192b|#824227|#fce4cc|#fceedf|#5a2d1b|#5a2d1b|#cbdcb4|#edccd5|#d9b7c0|#553e46|#f8dfc0|#b8a094
primitive|Primitive Link (pale chestnut)|#ddac9b|#935a4099|#f8eacd|#ff9d8a|#642a12|#ffda63|#edc176|#d63f21|#935a40|#f8eacd|#f8eacd|#f19d20|#935a40|#be8f9f|#642a12|#edc176|#bd7f98|#c6562d|#7a3313|#ff392a|#d4aab4|#edc176
saya|Saya no Uta (dark scarlet)|#25010f|#437f6388|#ffffff|#ecbc93|#d75f25|#ffcb3a|#a55a3d|#281e14|#e07340|#ebb48b|#de9670|#ebb48b|#575c51|#437f63|#ffcb3a|#ebb48b|#f59731|#f2c5a3|#d45628|#fbab34|#c5af88|#56714e
seinarukana|Seinarukana (white)|#ffffff|#fde9e688|#131838|#fc8e77|#e93d71|#5a5fc7|#424d81|#a43462|#324978|#99c9dd|#e93d71|#983666|#bfd2e3|#bcd1e4|#7a88a5|#324978|#3689b5|#b7adc6|#ee3970|#451f4b|#fdf1e8|#d4aba2
taka|Sora no Iro, Mizu no Iro (turquoise)|#4bb3ae|#48878c92|#fefefc|#4b2427|#ffaa88|#f4d926|#f8a022|#4b2427|#f6ffff|#f6ffff|#943048|#a93f56|#3c6d69|#48878c|#f4d926|#48878c|#b2f68f|#5ed8e5|#008278|#fcfefd|#5bdebe|#a9fdc1
teal|Teal (teal)|#000|#002525bc|#ddd|#007070|#e44|#00c9c9|#0c0|#c00|#004040|#003535|#007070|#fff|#001010|#003434|#008080|#007070|#354|#534|#534|#c00|#354|#0c0
term|Neon (black)|#000|#000|#0f0|#aaa|#f00|#ff0|#0c0|#c00|#fff|#0f0|#0f0|#0f0|#000|#000|#0f0|#fff|#cfc|#fcc|#000|#c00|#000|#0f0
tsukihime_02|Tsukihime (black)|#000000|#35020990|#fa4347|#54459a|#fd3fa9|#b768aa|#d79a7e|#412651|#fa4347|#fa4347|#d79a7e|#c17e61|#2e0106|#2e0106|#b768aa|#000000|#d79a7e|#412651|#46285a|#c0959f|#4f3246|#94769a
tsukihime|Tsukihime (midnight blue)|#29345f|#6a4668bb|#ffffff|#abcdff|#ffffff|#0be0e9|#55dfaa|#e30b47|#0cacf3|#a9bbfb|#e3ecff|#c6d7ff|#5a3a63|#9b8494|#605567|#b791f3|#87705c|#374d77|#76a1cd|#decdcd|#b50439|#decdcd
"""

data class Skin(
  val id: String,
  val name: String,
  val bodyBg: Color,
  val boxBg: Color,
  val mainText: Color,
  val grayedOut: Color,
  val standout: Color,
  val link: Color,
  val statOk: Color,
  val statNok: Color,
  val footer: Color,
  val mainTitle: Color,
  val boxTitle: Color,
  val altTitle: Color,
  val tabBg: Color,
  val secBg: Color,
  val secBorder: Color,
  val border: Color,
  val warnBg: Color,
  val warnBorder: Color,
  val noticeBg: Color,
  val noticeBorder: Color,
) {
  /** The site's --blendbg: boxbg alpha-blended over bodybg (active tab background). */
  val blendBg: Color get() = blend(boxBg, bodyBg)

  /** Opaque box colour, for surfaces that can't show the page through them (sheets, dialogs). */
  val solidBox: Color get() = blendBg

  /**
   * Second shade for striped lists (the site's table.stripe): a faint tint of the text colour, so it reads as
   * slightly lighter on dark skins and slightly darker on light ones, whatever the skin's palette.
   */
  val stripe: Color get() = mainText.copy(alpha = 0.07f)

  val isDark: Boolean get() = (0.299 * bodyBg.red + 0.587 * bodyBg.green + 0.114 * bodyBg.blue) < 0.5
}

/** `#rgb`, `#rrggbb` or `#rrggbbaa` → Color. */
fun cssColor(hex: String): Color {
  var h = hex.trim().removePrefix("#")
  if (h.length == 3 || h.length == 4) h = h.map { "$it$it" }.joinToString("")
  val rgb = h.substring(0, 6).toLong(16)
  val a = if (h.length == 8) h.substring(6, 8).toLong(16) else 0xFF
  return Color((a shl 24 or rgb).toInt())
}

private fun blend(top: Color, bottom: Color): Color {
  val a = top.alpha
  return Color(
    red = top.red * a + bottom.red * (1 - a),
    green = top.green * a + bottom.green * (1 - a),
    blue = top.blue * a + bottom.blue * (1 - a),
  )
}

object Skins {
  val all: List<Skin> = SKIN_TABLE.trim().lines().map { line ->
    val p = line.split('|')
    val c = p.drop(2).map(::cssColor)
    Skin(
      id = p[0], name = p[1], bodyBg = c[0], boxBg = c[1], mainText = c[2], grayedOut = c[3], standout = c[4],
      link = c[5], statOk = c[6], statNok = c[7], footer = c[8], mainTitle = c[9], boxTitle = c[10],
      altTitle = c[11], tabBg = c[12], secBg = c[13], secBorder = c[14], border = c[15],
      warnBg = c[18], warnBorder = c[19], noticeBg = c[20], noticeBorder = c[21],
    )
  }

  const val DEFAULT = "angel"

  fun byId(id: String?): Skin = all.firstOrNull { it.id == id } ?: all.first { it.id == DEFAULT }
}
