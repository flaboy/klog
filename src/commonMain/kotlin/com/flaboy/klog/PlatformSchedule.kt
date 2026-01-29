package com.flaboy.klog

/**
 * Cancellable scheduled work. Call [cancel] to prevent the block from running.
 */
interface Cancellable {
    fun cancel()
}

/**
 * Run [block] after [delayMs] milliseconds. Returns a [Cancellable] that cancels the scheduled run.
 * Platform-specific: Android uses Handler, JVM uses ScheduledExecutorService, iOS/Native use thread+sleep.
 */
expect fun runAfterDelay(delayMs: Long, block: () -> Unit): Cancellable
