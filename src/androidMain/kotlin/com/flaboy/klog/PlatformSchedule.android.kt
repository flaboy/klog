package com.flaboy.klog

import android.os.Handler
import android.os.Looper

actual fun runAfterDelay(delayMs: Long, block: () -> Unit): Cancellable {
    val handler = Handler(Looper.getMainLooper())
    val runnable = Runnable(block)
    handler.postDelayed(runnable, delayMs)
    return object : Cancellable {
        override fun cancel() {
            handler.removeCallbacks(runnable)
        }
    }
}
