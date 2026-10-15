package dev.stagefuel.vndb.ui.item

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.stagefuel.vndb.api.QueryResult
import dev.stagefuel.vndb.ui.vn.Load
import kotlinx.coroutines.launch

/** Loads one thing once; [reload] retries. */
class LoaderVm<T>(private val fetch: suspend () -> T) : ViewModel() {
  var state by mutableStateOf<Load<T>>(Load.Loading); private set
  init { reload() }
  fun reload() = viewModelScope.launch {
    state = Load.Loading
    state = runCatching { fetch() }.fold({ Load.Done(it) }, { Load.Failed(it.message ?: "Failed to load") })
  }
}

/** Paged list (Kana `page`/`more`), loaded on demand. */
class PagedVm<T>(private val fetch: suspend (Int) -> QueryResult<T>) : ViewModel() {
  var items by mutableStateOf<List<T>>(emptyList()); private set
  var loading by mutableStateOf(false); private set
  var more by mutableStateOf(true); private set
  var error by mutableStateOf<String?>(null); private set
  private var page = 0

  fun loadMore() {
    if (loading || !more) return
    loading = true
    viewModelScope.launch {
      runCatching { fetch(page + 1) }
        .onSuccess { r -> page++; items = items + r.results; more = r.more; error = null }
        .onFailure { error = it.message ?: "Failed to load" }
      loading = false
    }
  }
}
