package com.flaboy.klog

/**
 * UUID generator interface.
 * Projects can register custom generator before initialization.
 */
interface UUIDGenerator {
    /**
     * Generate device UUID.
     * Called once when log file is created.
     */
    fun generate(): String
}

/**
 * Default UUID generator (UUID v4)
 * Uses platform-specific UUID generation
 */
internal expect class DefaultUUIDGenerator() : UUIDGenerator {
    override fun generate(): String
}

