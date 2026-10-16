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
import dev.stagefuel.vndb.web.VoteStats
import dev.stagefuel.vndb.web.VoteStatsSource
import kotlinx.coroutines.launch

sealed interface Load<out T> {
  data object Idle : Load<Nothing>
  data object Loading : Load<Nothing>
  data class Done<T>(val value: T) : Load<T>
  data class Failed(val message: String) : Load<Nothing>
}

class VnVm(private val repo: VndbRepository, val id: String, private val voteSrc: VoteStatsSource? = null) : ViewModel() {
  var votes by mutableStateOf<Load<VoteStats?>>(Load.Idle); private set
  var covers by mutableStateOf<Load<List<Pair<dev.stagefuel.vndb.api.ReleaseImages, dev.stagefuel.vndb.api.ReleaseImage>>>>(Load.Idle); private set

  fun loadCovers() {
    if (covers !is Load.Idle) return
    viewModelScope.launch {
      covers = Load.Loading
      covers = runCatching { repo.releaseImages(id) }.fold({ Load.Done(it) }, { Load.Failed(it.message ?: "Failed") })
    }
  }

  fun loadVotes(force: Boolean = false) {
    val src = voteSrc ?: return
    if (!force && votes !is Load.Idle) return
    viewModelScope.launch {
      votes = Load.Loading
      votes = runCatching { src.get(id) }.fold({ Load.Done(it) }, { Load.Failed(it.message ?: "Failed") })
    }
  }

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
