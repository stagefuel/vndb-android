package dev.stagefuel.vndb.ui.vn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.stagefuel.vndb.api.Character
import dev.stagefuel.vndb.api.Release
import dev.stagefuel.vndb.api.Vn
import dev.stagefuel.vndb.data.VndbRepository
import kotlinx.coroutines.launch

sealed interface Load<out T> {
  data object Idle : Load<Nothing>
  data object Loading : Load<Nothing>
  data class Done<T>(val value: T) : Load<T>
  data class Failed(val message: String) : Load<Nothing>
}

class VnVm(private val repo: VndbRepository, val id: String) : ViewModel() {
  var vn by mutableStateOf<Load<Vn>>(Load.Idle); private set
  var releases by mutableStateOf<Load<List<Release>>>(Load.Idle); private set
  var characters by mutableStateOf<Load<List<Character>>>(Load.Idle); private set

  init { loadVn() }

  fun loadVn(refresh: Boolean = false) = viewModelScope.launch {
    vn = Load.Loading
    vn = runCatching { repo.vn(id, refresh) }.fold({ Load.Done(it) }, { Load.Failed(it.message ?: "Failed to load $id") })
  }

  fun loadReleases(force: Boolean = false) {
    if (!force && releases !is Load.Idle) return
    viewModelScope.launch {
      releases = Load.Loading
      releases = runCatching { repo.releases(id) }.fold({ Load.Done(it) }, { Load.Failed(it.message ?: "Failed") })
    }
  }

  fun loadCharacters(force: Boolean = false) {
    if (!force && characters !is Load.Idle) return
    viewModelScope.launch {
      characters = Load.Loading
      characters = runCatching { repo.characters(id) }.fold({ Load.Done(it) }, { Load.Failed(it.message ?: "Failed") })
    }
  }
}
