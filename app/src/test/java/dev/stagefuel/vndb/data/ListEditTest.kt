package dev.stagefuel.vndb.data

import dev.stagefuel.vndb.api.UlistEntry
import dev.stagefuel.vndb.api.UlistLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class ListEditTest {
  private val orig = UlistEntry(
    id = "v17", vote = 80, notes = "hi", started = "2025-01-01", finished = null,
    labels = listOf(UlistLabel(2, "Finished"), UlistLabel(7, "Voted"), UlistLabel(10, "Priority")),
  )
  private fun same() = ListEdit(80, "hi", "2025-01-01", null, setOf(2, 10))

  @Test fun unchangedSendsNothing() = assertEquals("{}", same().toPatch(orig).toString())

  @Test fun onlyChangedFields() {
    val p = same().copy(vote = null, finished = "2025-02-01", labels = setOf(1, 10)).toPatch(orig)
    assertEquals("""{"vote":null,"finished":"2025-02-01","labels_set":[1],"labels_unset":[2]}""", p.toString())
  }

  @Test fun managedLabelsNeverSent() {
    assertEquals("{}", same().copy(labels = setOf(0, 2, 7, 10)).toPatch(orig).toString())
  }

  @Test(expected = ListEdit.CantClearDate::class) fun clearingStartedIsRefused() {
    same().copy(started = "").toPatch(orig)
  }

  @Test fun newEntryWithNothingSetIsEmpty() {
    assertEquals("""{"vote":null}""", ListEdit(null, "", null, null, emptySet()).toPatch(null).toString())
  }
}
