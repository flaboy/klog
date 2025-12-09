package com.flaboy.klog

data class LogRecord(
    val timestampMillis: Long,
    val level: Byte,
    val message: String
)


