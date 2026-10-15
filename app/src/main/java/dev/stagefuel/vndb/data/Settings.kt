package dev.stagefuel.vndb.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("settings")

/** Signed-in account as reported by `GET /authinfo`. */
data class Account(val id: String, val username: String, val permissions: List<String>)

data class Prefs(
  val skin: String = "angel",
  /** 0 safe only, 1 up to suggestive, 2 everything — same scale as VNDB's image settings. */
  val maxSexual: Int = 0,
  val maxViolence: Int = 0,
  /** 0 hide spoilers, 1 minor, 2 all — VNDB's spoiler levels. */
  val spoilers: Int = 0,
  val account: Account? = null,
  /** Release tag the user chose to skip in the update prompt. */
  val skippedUpdate: String? = null,
  /** Put tab rows (VN tabs, list labels, site page tabs) at the bottom of the screen, near the thumb. */
  val tabsBottom: Boolean = false,
)

class Settings(private val context: Context) {
  private object K {
    val skin = stringPreferencesKey("skin")
    val sexual = intPreferencesKey("max_sexual")
    val violence = intPreferencesKey("max_violence")
    val spoilers = intPreferencesKey("spoilers")
    val token = stringPreferencesKey("token_enc")
    val uid = stringPreferencesKey("uid")
    val username = stringPreferencesKey("username")
    val perms = stringPreferencesKey("perms")
    val skippedUpdate = stringPreferencesKey("skipped_update")
    val tabsBottom = androidx.datastore.preferences.core.booleanPreferencesKey("tabs_bottom")
  }

  val prefs: Flow<Prefs> = context.store.data.map { p -> p.toPrefs() }

  private fun Preferences.toPrefs() = Prefs(
    skin = this[K.skin] ?: "angel",
    maxSexual = this[K.sexual] ?: 0,
    maxViolence = this[K.violence] ?: 0,
    spoilers = this[K.spoilers] ?: 0,
    skippedUpdate = this[K.skippedUpdate],
    tabsBottom = this[K.tabsBottom] ?: false,
    account = this[K.uid]?.let { Account(it, this[K.username].orEmpty(), this[K.perms].orEmpty().split(',').filter(String::isNotBlank)) },
  )

  suspend fun token(): String? = context.store.data.first()[K.token]?.let(TokenCrypto::decrypt)

  suspend fun signIn(token: String, account: Account) = context.store.edit {
    it[K.token] = TokenCrypto.encrypt(token)
    it[K.uid] = account.id
    it[K.username] = account.username
    it[K.perms] = account.permissions.joinToString(",")
  }

  suspend fun signOut() = context.store.edit {
    it.remove(K.token); it.remove(K.uid); it.remove(K.username); it.remove(K.perms)
  }

  suspend fun setSkin(id: String) = context.store.edit { it[K.skin] = id }
  suspend fun setMaxSexual(v: Int) = context.store.edit { it[K.sexual] = v }
  suspend fun setMaxViolence(v: Int) = context.store.edit { it[K.violence] = v }
  suspend fun setSpoilers(v: Int) = context.store.edit { it[K.spoilers] = v }
  suspend fun setTabsBottom(v: Boolean) = context.store.edit { it[K.tabsBottom] = v }
  suspend fun skipUpdate(tag: String) = context.store.edit { it[K.skippedUpdate] = tag }
}
