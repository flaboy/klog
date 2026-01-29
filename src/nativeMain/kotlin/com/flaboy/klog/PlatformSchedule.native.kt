@file:OptIn(kotlin.concurrent.atomics.ExperimentalAtomicApi::class)
package com.flaboy.klog

import kotlin.concurrent.atomics.AtomicInt
import kotlin.native.concurrent.Worker

actual fun runAfterDelay(delayMs: Long, block: () -> Unit): Cancellable {
    val cancelled = AtomicInt(0)
    val worker = Worker.start()
    worker.executeAfter(delayMs * 1000L) {
        if (cancelled.load() == 0) block()
    }
    return object : Cancellable {
        override fun cancel() {
            cancelled.store(1)
        }
    }
}
