package com.flaboy.klog

/**
 * Platform-agnostic configuration for ring buffer logger.
 * Path is provided separately by platform layer.
 */
data class LogConfig(
    val maxBytes: Int,
    val formatVersion: Int = 1
)

