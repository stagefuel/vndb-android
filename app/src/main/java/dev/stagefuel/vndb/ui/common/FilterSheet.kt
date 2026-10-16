package dev.stagefuel.vndb.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.stagefuel.vndb.data.SearchFilter
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType

private val COMMON_LANGS = listOf("ja", "en", "zh-Hans", "zh-Hant", "ko", "ru", "es", "de", "fr", "it", "pt-br", "vi")
private val COMMON_PLATFORMS = listOf("win", "lin", "mac", "and", "ios", "web", "swi", "sw2", "ps4", "ps5", "psv", "xxs")
private val SHORT_LENGTHS = mapOf(1 to "very short", 2 to "short", 3 to "medium", 4 to "long", 5 to "very long")

/**
 * Language / platform (and, for VN search, length and release years) filters. Within a group any choice
 * matches; groups combine. [full] = false shows only languages and platforms (release lists).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(initial: SearchFilter, full: Boolean, onDismiss: () -> Unit, onApply: (SearchFilter) -> Unit) {
  val skin = LocalSkin.current
  var f by remember { mutableStateOf(initial) }
  var allLangs by remember { mutableStateOf(initial.langs.any { it !in COMMON_LANGS }) }
  var allPlatforms by remember { mutableStateOf(initial.platforms.any { it !in COMMON_PLATFORMS }) }
  var from by remember { mutableStateOf(initial.fromYear?.toString().orEmpty()) }
  var to by remember { mutableStateOf(initial.toYear?.toString().orEmpty()) }

  @Composable
  fun chip(label: String, on: Boolean, click: () -> Unit) = Text(
    label, style = VndbType.small, color = if (on) skin.mainText else skin.link,
    modifier = Modifier.border(1.dp, if (on) skin.link else skin.border).background(if (on) skin.blendBg else skin.tabBg)
      .clickable(onClick = click).padding(horizontal = 8.dp, vertical = 5.dp),
  )

  @Composable
  fun group(title: String, codes: List<String>, all: Boolean, onAll: () -> Unit, selected: Set<String>, name: (String) -> String, toggle: (String) -> Unit) {
    SectionHeader(title + if (selected.isNotEmpty()) " (${selected.size})" else "")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      codes.forEach { c -> chip(name(c), c in selected) { toggle(c) } }
      Text(if (all) "fewer" else "more…", style = VndbType.small, color = skin.link,
        modifier = Modifier.clickable(onClick = onAll).padding(horizontal = 6.dp, vertical = 5.dp))
    }
  }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = skin.solidBox) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
      Text("Filters", style = VndbType.boxH1, color = skin.boxTitle)
      val langs = if (allLangs) COMMON_LANGS + (Names.languages.keys - COMMON_LANGS.toSet()).sortedBy { Names.lang(it) } else COMMON_LANGS
      group("Language", langs, allLangs, { allLangs = !allLangs }, f.langs, Names::lang) { c ->
        f = f.copy(langs = if (c in f.langs) f.langs - c else f.langs + c)
      }
      val plats = if (allPlatforms) COMMON_PLATFORMS + (Names.platforms.keys - COMMON_PLATFORMS.toSet()).sortedBy { Names.platform(it) } else COMMON_PLATFORMS
      group("Platform", plats, allPlatforms, { allPlatforms = !allPlatforms }, f.platforms, Names::platform) { c ->
        f = f.copy(platforms = if (c in f.platforms) f.platforms - c else f.platforms + c)
      }
      if (full) {
        SectionHeader("Length" + if (f.lengths.isNotEmpty()) " (${f.lengths.size})" else "")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          SHORT_LENGTHS.forEach { (k, label) -> chip(label, k in f.lengths) { f = f.copy(lengths = if (k in f.lengths) f.lengths - k else f.lengths + k) } }
        }
        SectionHeader("Released")
        Row(verticalAlignment = Alignment.CenterVertically) {
          YearField(from, "from") { from = it }
          Grey("  –  ", style = VndbType.body)
          YearField(to, "to") { to = it }
        }
      }
      Spacer(Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        LinkText("Apply") {
          onApply(f.copy(fromYear = from.toIntOrNull()?.takeIf { it in 1970..2100 }, toYear = to.toIntOrNull()?.takeIf { it in 1970..2100 }))
        }
        LinkText("Clear") { onApply(SearchFilter()) }
        LinkText("Cancel") { onDismiss() }
      }
    }
  }
}

@Composable
private fun YearField(value: String, hint: String, onChange: (String) -> Unit) {
  val skin = LocalSkin.current
  BasicTextField(
    value = value,
    onValueChange = { v -> onChange(v.filter(Char::isDigit).take(4)) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    textStyle = VndbType.body.copy(color = skin.mainText),
    cursorBrush = SolidColor(skin.mainText),
    modifier = Modifier.width(80.dp).border(1.dp, skin.secBorder).padding(6.dp),
    decorationBox = { inner -> if (value.isEmpty()) Grey(hint, style = VndbType.body); inner() },
  )
}
