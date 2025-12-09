package com.flaboy.klog

import kotlin.random.Random

internal actual class DefaultUUIDGenerator actual constructor() : UUIDGenerator {
    actual override fun generate(): String {
        // Generate UUID v4 using random
        val randomBytes = Random.Default.nextBytes(16)
        
        // Set version (4) and variant bits
        randomBytes[6] = ((randomBytes[6].toInt() and 0x0F) or 0x40).toByte()
        randomBytes[8] = ((randomBytes[8].toInt() and 0x3F) or 0x80).toByte()
        
        return buildString {
            for (i in randomBytes.indices) {
                if (i == 4 || i == 6 || i == 8 || i == 10) append('-')
                append(randomBytes[i].toInt().and(0xFF).toString(16).padStart(2, '0'))
            }
        }
    }
}

