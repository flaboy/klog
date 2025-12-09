package com.flaboy.klog

/**
 * Report result
 */
data class ReportResult(
    val success: Boolean,
    val reportId: String? = null,
    val s3Key: String? = null,
    val lineCount: Int,
    val sizeBytes: Int,
    val compressedSize: Int,
    val error: String? = null,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
)

