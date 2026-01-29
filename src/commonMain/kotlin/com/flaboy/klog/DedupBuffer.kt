package com.flaboy.klog

import kotlinx.datetime.Clock

private const val SILENCE_FLUSH_MS = 20_000L

/**
 * Buffers consecutive identical (tag, message, level) logs and flushes:
 * - When a different log arrives (then outputs previous with "repeat N times" if N > 1).
 * - When 20s pass with no new log (then outputs buffered line with last occurrence timestamp).
 */
internal class DedupBuffer(
    private val runAfterDelay: (Long, () -> Unit) -> Cancellable,
    private val onFlush: (tag: String, message: String, level: Byte, count: Int, lastTimestampMillis: Long) -> Unit
) {
    private data class Pending(
        val tag: String,
        val message: String,
        val level: Byte,
        var count: Int,
        var lastTimestampMillis: Long
    )

    private var pending: Pending? = null
    private var scheduled: Cancellable? = null

    fun add(tag: String, message: String, level: Byte) {
        val now = Clock.System.now().toEpochMilliseconds()
        val current = pending
        if (current != null && current.tag == tag && current.message == message && current.level == level) {
            current.count++
            current.lastTimestampMillis = now
            reschedule()
            return
        }
        flushPending()
        pending = Pending(tag, message, level, 1, now)
        reschedule()
    }

    private fun reschedule() {
        scheduled?.cancel()
        scheduled = runAfterDelay(SILENCE_FLUSH_MS) {
            flushPending()
        }
    }

    private fun flushPending() {
        val p = pending ?: return
        pending = null
        scheduled?.cancel()
        scheduled = null
        onFlush(p.tag, p.message, p.level, p.count, p.lastTimestampMillis)
    }
}
