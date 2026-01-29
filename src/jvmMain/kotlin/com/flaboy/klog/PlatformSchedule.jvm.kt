package com.flaboy.klog

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val scheduler = Executors.newSingleThreadScheduledExecutor()

actual fun runAfterDelay(delayMs: Long, block: () -> Unit): Cancellable {
    val future = scheduler.schedule(block, delayMs, TimeUnit.MILLISECONDS)
    return object : Cancellable {
        override fun cancel() {
            future.cancel(false)
        }
    }
}
