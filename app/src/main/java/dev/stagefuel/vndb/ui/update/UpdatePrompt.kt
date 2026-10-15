package dev.stagefuel.vndb.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.stagefuel.vndb.data.Settings
import dev.stagefuel.vndb.data.Updater
import dev.stagefuel.vndb.ui.common.Grey
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.VndbType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Holds the update prompt's state; one per activity so the launch check runs once. */
class UpdateVm(private val updater: Updater, private val settings: Settings) : ViewModel() {
  var release by mutableStateOf<Updater.Release?>(null); private set
  var progress by mutableStateOf<Float?>(null); private set
  var error by mutableStateOf<String?>(null); private set
  /** Result line for a manual check ("Up to date."), shown in About. */
  var status by mutableStateOf<String?>(null); private set
  private var checking = false

  fun check(manual: Boolean) {
    if (checking) return
    checking = true
    if (manual) status = "Checking…"
    viewModelScope.launch {
      val r = updater.check()
      val skipped = settings.prefs.first().skippedUpdate
      checking = false
      if (manual) status = if (r == null) "Up to date." else null
      if (r != null && (manual || r.tag_name != skipped)) release = r
    }
  }

  fun dismiss() { release = null; progress = null; error = null }
  fun skip() { release?.let { r -> viewModelScope.launch { settings.skipUpdate(r.tag_name) } }; dismiss() }

  fun install(ctx: Context) {
    val r = release ?: return
    if (!ctx.packageManager.canRequestPackageInstalls()) {
      error = "Allow VNDB to install apps, then tap Update again."
      ctx.startActivity(Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
      return
    }
    error = null; progress = 0f
    viewModelScope.launch {
      runCatching { updater.install(ctx.applicationContext, r) { progress = it } }
        .onSuccess { dismiss() }
        .onFailure { progress = null; error = it.message ?: "Update failed" }
    }
  }
}

@Composable
fun UpdateDialog(vm: UpdateVm) {
  val r = vm.release ?: return
  val ctx = LocalContext.current
  val skin = LocalSkin.current
  val busy = vm.progress != null
  AlertDialog(
    onDismissRequest = { if (!busy) vm.dismiss() },
    containerColor = skin.boxBg,
    title = { Text("Update to ${r.tag_name}?", style = VndbType.boxH1, color = skin.boxTitle) },
    text = {
      Column {
        r.body?.takeIf(String::isNotBlank)?.let {
          Text(it.trim(), style = VndbType.small, color = skin.mainText,
            modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()))
        }
        vm.progress?.let { LinearProgressIndicator(progress = { it }, color = skin.link, modifier = Modifier.padding(vertical = 8.dp)) }
        vm.error?.let { Grey(it, style = VndbType.small) }
      }
    },
    confirmButton = { TextButton(onClick = { vm.install(ctx) }, enabled = !busy) { Text("Update", color = skin.link) } },
    dismissButton = {
      if (!busy) Row {
        TextButton(onClick = vm::dismiss) { Text("Later", color = skin.link) }
        TextButton(onClick = vm::skip) { Text("Skip this version", color = skin.grayedOut) }
      }
    },
  )
}
