package com.flaboy.klog

import platform.Foundation.NSUUID
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual class DefaultUUIDGenerator actual constructor() : UUIDGenerator {
    actual override fun generate(): String {
        return NSUUID().UUIDString()
    }
}

