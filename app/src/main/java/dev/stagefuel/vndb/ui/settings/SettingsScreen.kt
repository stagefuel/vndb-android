package dev.stagefuel.vndb.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.stagefuel.vndb.BuildConfig
import dev.stagefuel.vndb.VndbApp
import dev.stagefuel.vndb.data.Account
import dev.stagefuel.vndb.data.Prefs
import dev.stagefuel.vndb.ui.Nav
import dev.stagefuel.vndb.ui.common.Grey
import dev.stagefuel.vndb.ui.common.LinkText
import dev.stagefuel.vndb.ui.common.SectionHeader
import dev.stagefuel.vndb.ui.common.VndbBox
import dev.stagefuel.vndb.ui.common.WarningBox
import dev.stagefuel.vndb.ui.theme.LocalSkin
import dev.stagefuel.vndb.ui.theme.Skins
import dev.stagefuel.vndb.ui.theme.VndbType
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: VndbApp, prefs: Prefs, nav: Nav, updates: dev.stagefuel.vndb.ui.update.UpdateVm) {
  val skin = LocalSkin.current
  val scope = rememberCoroutineScope()
  var token by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
    dev.stagefuel.vndb.ui.common.SiteTitle()
    Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
    prefs.account?.let { acc ->
      VndbBox {
        SectionHeader(acc.username)
        val u = "https://vndb.org/${acc.id}"
        // The web view keeps its own vndb.org login (cookie), separate from the API token; it is lost on reinstall.
        // Logged-out visits to your own notifications/edit pages get the site's "page does not exist".
        val siteLogin = remember { siteLoggedIn() }
        val own = { url: String -> if (siteLogin) nav.web(url) else nav.web("https://vndb.org/u/login") }
        if (!siteLogin) Row {
          Grey("Not logged in on vndb.org. ", style = VndbType.small)
          LinkText("log in", style = VndbType.small) { nav.web("https://vndb.org/u/login") }
        }
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
          LinkText("profile") { nav.web(u) }
          LinkText("notifications") { own("$u/notifies") }
          LinkText("edit profile") { own("$u/edit") }
          LinkText("reviews") { nav.web("https://vndb.org/w?u=${acc.id}") }
          LinkText("posts") { nav.web("$u/posts") }
          LinkText("history") { nav.web("$u/hist") }
        }
      }
      Spacer(Modifier.height(10.dp))
    }
    UserLookup(app, nav)
    Spacer(Modifier.height(10.dp))
    VndbBox {
      SectionHeader("Account")
      val acc = prefs.account
      if (acc != null) {
        Text("Signed in as ${acc.username} (${acc.id})", style = VndbType.body, color = skin.mainText)
        Grey("Permissions: " + acc.permissions.joinToString(", ").ifEmpty { "none" })
        if ("listwrite" !in acc.permissions) Grey("This token can't edit your list; make one with “listwrite”.")
        Spacer(Modifier.height(6.dp))
        LinkText("Sign out") { scope.launch { app.settings.signOut() } }
      } else {
        Text("Paste an API token to see and edit your list.", style = VndbType.body, color = skin.mainText)
        Row {
          Grey("Make one at ", style = VndbType.body)
          LinkText("vndb.org/u/tokens") { nav.web("https://vndb.org/u/tokens") }
          Grey(" with listread + listwrite.", style = VndbType.body)
        }
        Spacer(Modifier.height(6.dp))
        BasicTextField(
          value = token,
          onValueChange = { token = it.trim() },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          textStyle = VndbType.body.copy(color = skin.mainText),
          cursorBrush = SolidColor(skin.mainText),
          modifier = Modifier.fillMaxWidth().border(1.dp, skin.secBorder).padding(8.dp),
          decorationBox = { inner -> if (token.isEmpty()) Grey("token", style = VndbType.body); inner() },
        )
        error?.let { WarningBox(it) }
        Spacer(Modifier.height(6.dp))
        if (busy) Grey("checking…", style = VndbType.body)
        else LinkText("Sign in") {
          if (token.isEmpty()) return@LinkText
          busy = true; error = null
          scope.launch {
            runCatching { app.api.authInfo(token) }
              .onSuccess { a -> app.settings.signIn(token, Account(a.id, a.username, a.permissions)); token = "" }
              .onFailure { error = it.message ?: "Sign-in failed" }
            busy = false
          }
        }
      }
      Spacer(Modifier.height(4.dp))
      Grey("Forums and other site pages use the vndb.org login in the Forums tab; that's separate from the token.")
    }
    Spacer(Modifier.height(10.dp))

    VndbBox {
      SectionHeader("Display")
      SkinPicker(prefs.skin) { id -> scope.launch { app.settings.setSkin(id) } }
      Choice("Sexual images", listOf("safe", "suggestive", "explicit"), prefs.maxSexual) { scope.launch { app.settings.setMaxSexual(it) } }
      Choice("Violent images", listOf("tame", "violent", "brutal"), prefs.maxViolence) { scope.launch { app.settings.setMaxViolence(it) } }
      Choice("Spoilers", listOf("hide", "minor", "all"), prefs.spoilers) { scope.launch { app.settings.setSpoilers(it) } }
      Choice("Tabs", listOf("top", "bottom"), if (prefs.tabsBottom) 1 else 0) { scope.launch { app.settings.setTabsBottom(it == 1) } }
    }
    Spacer(Modifier.height(10.dp))

    VndbBox {
      SectionHeader("About")
      Row {
        Text("vndb-android ${BuildConfig.VERSION_NAME}  ", style = VndbType.body, color = skin.mainText)
        updates.status?.let { Grey(it, style = VndbType.body) } ?: LinkText("check for updates") { updates.check(manual = true) }
      }
      Grey("An unofficial client for the visual novel database.", style = VndbType.body)
      Row {
        Grey("Data from ", style = VndbType.body)
        LinkText("vndb.org") { nav.web("https://vndb.org/") }
        Grey(", under the ", style = VndbType.body)
        LinkText("VNDB Data License") { nav.web("https://vndb.org/d17") }
        Grey(".", style = VndbType.body)
      }
    }
    }
  }
}

@Composable
private fun SkinPicker(current: String, onPick: (String) -> Unit) {
  val skin = LocalSkin.current
  var open by remember { mutableStateOf(false) }
  Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("Skin", style = VndbType.body, color = skin.mainText, modifier = Modifier.width(110.dp))
    Box {
      LinkText(Skins.byId(current).name) { open = true }
      DropdownMenu(open, { open = false }, containerColor = skin.solidBox) {
        Skins.all.forEach { s ->
          DropdownMenuItem(
            leadingIcon = { Box(Modifier.size(16.dp).background(s.bodyBg).border(3.dp, s.link)) },
            text = { Text(s.name, color = if (s.id == current) skin.standout else skin.mainText) },
            onClick = { open = false; onPick(s.id) },
          )
        }
      }
    }
  }
}

@Composable
private fun Choice(label: String, options: List<String>, selected: Int, onPick: (Int) -> Unit) {
  val skin = LocalSkin.current
  Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, style = VndbType.body, color = skin.mainText, modifier = Modifier.width(110.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      options.forEachIndexed { i, o ->
        Text(o, style = VndbType.body, color = if (i == selected) skin.mainText else skin.link, modifier = Modifier.clickable { onPick(i) })
      }
    }
  }
}

/** Find another user by name or id and open their (public) list. */
@Composable
private fun UserLookup(app: VndbApp, nav: Nav) {
  val skin = LocalSkin.current
  val scope = rememberCoroutineScope()
  var q by remember { mutableStateOf("") }
  var msg by remember { mutableStateOf<String?>(null) }
  fun go() {
    val name = q.trim().takeIf { it.isNotEmpty() } ?: return
    scope.launch {
      runCatching { app.repo.user(name) }
        .onSuccess { u -> if (u == null) msg = "No such user." else { msg = null; nav.userList(u.id, u.username) } }
        .onFailure { msg = it.message }
    }
  }
  VndbBox {
    SectionHeader("Users")
    BasicTextField(
      value = q,
      onValueChange = { q = it },
      singleLine = true,
      textStyle = VndbType.body.copy(color = skin.mainText),
      cursorBrush = SolidColor(skin.mainText),
      keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
      keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { go() }),
      modifier = Modifier.fillMaxWidth().border(1.dp, skin.secBorder).padding(8.dp),
      decorationBox = { inner -> if (q.isEmpty()) Grey("username or u123", style = VndbType.body); inner() },
    )
    msg?.let { Grey(it, style = VndbType.body) }
    LinkText("view list", Modifier.padding(top = 6.dp)) { go() }
  }
}

/** True when the web view holds a vndb.org session cookie. */
private fun siteLoggedIn(): Boolean = runCatching {
  android.webkit.CookieManager.getInstance().getCookie("https://vndb.org").orEmpty().contains("vndb_auth=")
}.getOrDefault(false)
