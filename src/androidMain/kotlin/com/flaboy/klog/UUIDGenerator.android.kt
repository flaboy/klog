package com.flaboy.klog

import java.util.UUID

internal actual class DefaultUUIDGenerator actual constructor() : UUIDGenerator {
    actual override fun generate(): String {
        return UUID.randomUUID().toString()
    }
}


