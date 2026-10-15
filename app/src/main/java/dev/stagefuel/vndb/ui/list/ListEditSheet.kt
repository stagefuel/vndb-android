package dev.stagefuel.vndb.ui.list

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stagefuel.vndb.VndbApp
import dev.stagefuel.vndb.api.Release
import dev.stagefuel.vndb.api.UlistLabel
import dev.stagefuel.vndb.data.ListEdit
import dev.stagefuel.vndb.data.VndbRepository
import dev.stagefuel.vndb.ui.common.Grey
import dev.stagefuel.vndb.ui.common.LinkText
import dev.stagefuel.vndb.ui.common.Loading
import dev.stagefuel.vndb.ui.common.Names
import dev.stagefuel.vndb.ui.common.SectionHeader
import dev.stagefuel.vndb.ui.common.WarningBox
import dev.stagefuel.vndb.ui.common.formatVote
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType
import kotlinx.coroutines.launch

/** Vote input "8", "7.5", "10" → 80, 75, 100; blank → null; anything else → error. */
fun parseVote(s: String): Result<Int?> {
  val t = s.trim().replace(',', '.')
  if (t.isEmpty()) return Result.success(null)
  val d = t.toDoubleOrNull() ?: return Result.failure(IllegalArgumentException("Vote must be a number from 1 to 10"))
  val v = Math.round(d * 10).toInt()
  return if (v in 10..100) Result.success(v) else Result.failure(IllegalArgumentException("Vote must be from 1 to 10"))
}

private val DATE = Regex("""^\d{4}-\d{2}-\d{2}$""")

class ListEditVm(private val repo: VndbRepository, private val uid: String, val vnId: String) : ViewModel() {
  var loading by mutableStateOf(true); private set
  var error by mutableStateOf<String?>(null)
  var saving by mutableStateOf(false); private set
  var onList by mutableStateOf(false); private set
  private var orig: dev.stagefuel.vndb.api.UlistEntry? = null
  var labels by mutableStateOf<List<UlistLabel>>(emptyList()); private set
  var releases by mutableStateOf<List<Release>>(emptyList()); private set
  var releaseStatus by mutableStateOf<Map<String, Int>>(emptyMap()); private set

  var selected by mutableStateOf<Set<Int>>(emptySet())
  var vote by mutableStateOf("")
  var started by mutableStateOf("")
  var finished by mutableStateOf("")
  var notes by mutableStateOf("")

  init { load() }

  fun load() = viewModelScope.launch {
    loading = true; error = null
    runCatching {
      labels = repo.labels(uid).filter { it.id !in ListEdit.MANAGED }
      val e = repo.ulistEntry(uid, vnId)
      orig = e
      onList = e != null
      selected = e?.labels?.map { it.id }?.filterNot { it in ListEdit.MANAGED }?.toSet().orEmpty()
      vote = e?.vote?.let(::formatVote).orEmpty()
      started = e?.started.orEmpty()
      finished = e?.finished.orEmpty()
      notes = e?.notes.orEmpty()
      releaseStatus = e?.releases?.associate { it.id to it.listStatus }.orEmpty()
      releases = repo.releases(vnId)
    }.onFailure { error = it.message ?: "Failed to load list entry" }
    loading = false
  }

  fun save(done: () -> Unit) {
    val v = parseVote(vote).getOrElse { error = it.message; return }
    for (d in listOf(started, finished)) if (d.isNotBlank() && !DATE.matches(d.trim())) { error = "Dates must be YYYY-MM-DD"; return }
    viewModelScope.launch {
      saving = true; error = null
      runCatching { repo.saveEntry(vnId, ListEdit(v, notes, started.trim(), finished.trim(), selected), orig) }
        .onSuccess { done() }
        .onFailure { error = it.message ?: "Save failed" }
      saving = false
    }
  }

  fun remove(done: () -> Unit) = viewModelScope.launch {
    saving = true; error = null
    runCatching { repo.removeEntry(vnId) }.onSuccess { done() }.onFailure { error = it.message ?: "Remove failed" }
    saving = false
  }

  /** null removes the release from the list. */
  fun setRelease(rid: String, status: Int?) = viewModelScope.launch {
    runCatching { repo.setReleaseStatus(rid, status) }
      .onSuccess {
        releaseStatus = if (status == null) releaseStatus - rid else releaseStatus + (rid to status)
        // Adding a release adds its VN to the list (an empty entry); removing it never removes the VN.
        if (!onList && status != null) { onList = true; orig = repo.ulistEntry(uid, vnId) }
      }
      .onFailure { error = it.message ?: "Failed to update release" }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListEditSheet(app: VndbApp, vnId: String, title: String, onDismiss: () -> Unit, onSaved: () -> Unit = {}) {
  val skin = LocalSkin.current
  val account = app.settings.prefs.collectAsStateWithLifecycle(initialValue = null).value?.account ?: return
  val vm = viewModel(key = "edit-$vnId-${account.id}") { ListEditVm(app.repo, account.id, vnId) }
  val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val close = { onSaved(); onDismiss() }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = skin.solidBox) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
      Text(title, style = VndbType.boxH1, color = skin.boxTitle)
      Grey(if (vm.onList) "On your list" else "Not on your list")
      vm.error?.let { WarningBox(it) }
      if (vm.loading) { Loading(); return@Column }

      SectionHeader("Labels")
      FlowRow {
        vm.labels.forEach { l ->
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { vm.selected = if (l.id in vm.selected) vm.selected - l.id else vm.selected + l.id }) {
            Checkbox(
              checked = l.id in vm.selected,
              onCheckedChange = { vm.selected = if (it) vm.selected + l.id else vm.selected - l.id },
              colors = CheckboxDefaults.colors(checkedColor = skin.link, uncheckedColor = skin.secBorder, checkmarkColor = skin.bodyBg),
            )
            Text(l.label + if (l.private) " (private)" else "", style = VndbType.body, color = skin.mainText, modifier = Modifier.padding(end = 10.dp))
          }
        }
      }

      SectionHeader("Vote, dates and notes")
      Field("Vote (1-10)", vm.vote, KeyboardType.Decimal) { vm.vote = it }
      Field("Started", vm.started, KeyboardType.Number, placeholder = "YYYY-MM-DD") { vm.started = it }
      Field("Finished", vm.finished, KeyboardType.Number, placeholder = "YYYY-MM-DD") { vm.finished = it }
      Field("Notes", vm.notes, KeyboardType.Text, singleLine = false) { vm.notes = it }

      if (vm.releases.isNotEmpty()) {
        SectionHeader("Releases")
        vm.releases.forEach { r -> ReleaseStatusRow(r, vm.releaseStatus[r.id]) { vm.setRelease(r.id, it) } }
      }

      Spacer(Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        if (vm.saving) Grey("saving…", style = VndbType.body)
        else {
          LinkText("Save") { vm.save(close) }
          if (vm.onList) Text("Remove from list", style = VndbType.body, color = skin.statNok, modifier = Modifier.clickable { vm.remove(close) })
          LinkText("Cancel") { onDismiss() }
        }
      }
    }
  }
}

@Composable
private fun Field(label: String, value: String, type: KeyboardType, placeholder: String = "", singleLine: Boolean = true, onChange: (String) -> Unit) {
  val skin = LocalSkin.current
  Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
    Text(label, style = VndbType.body, color = skin.mainText, modifier = Modifier.width(96.dp))
    BasicTextField(
      value = value,
      onValueChange = onChange,
      singleLine = singleLine,
      minLines = if (singleLine) 1 else 3,
      keyboardOptions = KeyboardOptions(keyboardType = type),
      textStyle = VndbType.body.copy(color = skin.mainText),
      cursorBrush = SolidColor(skin.mainText),
      modifier = Modifier.weight(1f).border(1.dp, skin.secBorder).padding(6.dp),
      decorationBox = { inner -> if (value.isEmpty() && placeholder.isNotEmpty()) Grey(placeholder, style = VndbType.body); inner() },
    )
  }
}

@Composable
private fun ReleaseStatusRow(r: Release, status: Int?, onSet: (Int?) -> Unit) {
  val skin = LocalSkin.current
  var open by remember { mutableStateOf(false) }
  Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f)) {
      Text(r.title, style = VndbType.body, color = skin.mainText)
      Grey(listOfNotNull(r.released, r.languages.joinToString(", ") { it.lang }).joinToString(" · "))
    }
    Column {
      Text(
        status?.let { Names.releaseStatus.getOrNull(it) } ?: "not on list",
        style = VndbType.small,
        color = if (status == null) skin.link else skin.mainText,
        modifier = Modifier.clickable { open = true }.border(1.dp, skin.secBorder).padding(horizontal = 8.dp, vertical = 4.dp),
      )
      DropdownMenu(open, onDismissRequest = { open = false }, containerColor = skin.solidBox) {
        Names.releaseStatus.forEachIndexed { i, label ->
          DropdownMenuItem(text = { Text(label, color = skin.mainText) }, onClick = { open = false; onSet(i) })
        }
        if (status != null) DropdownMenuItem(text = { Text("remove", color = skin.statNok) }, onClick = { open = false; onSet(null) })
      }
    }
  }
}
