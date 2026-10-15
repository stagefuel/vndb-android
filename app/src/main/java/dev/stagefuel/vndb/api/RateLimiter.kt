package dev.stagefuel.vndb.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sliding-window limiter for Kana's 200 requests / 5 minutes. Stays a little under the cap so
 * other clients on the same IP (VN Shelf) don't tip us into 429s.
 */
class RateLimiter(
  private val max: Int = 180,
  private val windowMs: Long = 5 * 60_000L,
  private val now: () -> Long = System::currentTimeMillis,
) {
  private val mutex = Mutex()
  private val stamps = ArrayDeque<Long>()

  suspend fun acquire() {
    while (true) {
      val wait = mutex.withLock {
        val t = now()
        while (stamps.isNotEmpty() && t - stamps.first() >= windowMs) stamps.removeFirst()
        if (stamps.size < max) {
          stamps.addLast(t); 0L
        } else {
          windowMs - (t - stamps.first())
        }
      }
      if (wait <= 0) return
      delay(wait)
    }
  }
}
