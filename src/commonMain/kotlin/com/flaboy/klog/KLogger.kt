package com.flaboy.klog

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

import okio.Path.Companion.toPath

/**
 * Unified logger that writes to both console and ring buffer file.
 * Consecutive identical (tag, message, level) logs are deduplicated and output as one line with " (repeat N times)" when flushed.
 * Flush happens when a different log arrives or after 20s silence (timestamp used is the last occurrence).
 * 
 * Usage:
 * ```
 * // Initialize once in your app
 * val platformLogger = MyPlatformLogger()
 * val klogger = KLogger.initialize(logPath, platformLogger)
 * 
 * // Use throughout your app
 * KLogger.log("Tag", "message")
 * KLogger.logW("Tag", "warning")
 * KLogger.logE("Tag", "error", exception)
 * ```
 */
class KLogger private constructor(
    private val ringLogger: RingLogger,
    private val platformLogger: PlatformLogger,
    private val dedupBuffer: DedupBuffer,
    private val dedupEnabled: Boolean
) {
    /**
     * Log info level message.
     */
    fun log(tag: String, message: String) {
        if (!dedupEnabled) {
            emitImmediate(tag, message, LEVEL_INFO)
            return
        }
        dedupBuffer.add(tag, message, LEVEL_INFO)
    }

    /**
     * Log warning level message.
     */
    fun logW(tag: String, message: String) {
        if (!dedupEnabled) {
            emitImmediate(tag, message, LEVEL_WARNING)
            return
        }
        dedupBuffer.add(tag, message, LEVEL_WARNING)
    }

    /**
     * Log error level message.
     */
    fun logE(tag: String, message: String, throwable: Throwable? = null) {
        val errorMsg = if (throwable != null) {
            "$message: ${throwable.message}"
        } else {
            message
        }
        if (!dedupEnabled) {
            emitImmediate(tag, errorMsg, LEVEL_ERROR)
            return
        }
        dedupBuffer.add(tag, errorMsg, LEVEL_ERROR)
    }

    private fun emitImmediate(tag: String, message: String, level: Byte) {
        val now = Clock.System.now().toEpochMilliseconds()
        val formatted = KLogger.formatLogLine(tag, message, level, now)
        when (level) {
            LEVEL_WARNING -> platformLogger.logW(tag, formatted)
            LEVEL_ERROR -> platformLogger.logE(tag, formatted, null)
            else -> platformLogger.log(tag, formatted)
        }
        ringLogger.append(formatted, level)
    }

    /**
     * Get recent logs from file (last N records).
     */
    fun getRecentLogs(count: Int): List<String> {
        return ringLogger.tail(count).map { it.message }
    }
    
    /**
     * Get logs since timestamp (milliseconds).
     */
    fun getLogsSince(timestampMillis: Long, limit: Int = 1000): List<String> {
        return ringLogger.since(timestampMillis, limit).map { it.message }
    }
    
    companion object {
        private const val LEVEL_INFO: Byte = 1
        private const val LEVEL_WARNING: Byte = 2
        private const val LEVEL_ERROR: Byte = 3
        
        private var instance: KLogger? = null
        
        /**
         * Initialize KLogger with okio.Path. Must be called before first use.
         */
        fun initialize(
            logPath: okio.Path,
            platformLogger: PlatformLogger,
            maxBytes: Int = 2 * 1024 * 1024
        ): KLogger {
            val config = LogConfig(maxBytes = maxBytes)
            val ringLogger = RingLogger(logPath, config)
            val dedupBuffer = DedupBuffer(::runAfterDelay) { tag, message, level, count, lastTs ->
                val displayMessage = if (count > 1) "$message (repeat $count times)" else message
                val formatted = formatLogMessageWithTimestamp(tag, displayMessage, level, lastTs)
                when (level) {
                    LEVEL_WARNING -> platformLogger.logW(tag, formatted)
                    LEVEL_ERROR -> platformLogger.logE(tag, formatted, null)
                    else -> platformLogger.log(tag, formatted)
                }
                ringLogger.append(formatted, level)
            }
            val logger = KLogger(ringLogger, platformLogger, dedupBuffer, config.dedupEnabled)
            instance = logger
            return logger
        }

        internal fun formatLogLine(tag: String, message: String, level: Byte, timestampMillis: Long): String =
            formatLogMessageWithTimestamp(tag, message, level, timestampMillis)

        private fun formatLogMessageWithTimestamp(tag: String, message: String, level: Byte, timestampMillis: Long): String {
            val instant = Instant.fromEpochMilliseconds(timestampMillis)
            val timestamp = formatTimestamp(instant)
            val levelStr = when (level) {
                LEVEL_WARNING -> "WARNING"
                LEVEL_ERROR -> "ERROR"
                else -> ""
            }
            return if (levelStr.isEmpty()) {
                "[$timestamp] [$tag] $message"
            } else {
                "[$timestamp] $levelStr [$tag] $message"
            }
        }

        private fun formatTimestamp(instant: Instant): String {
            val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val year = dateTime.date.year
            val month = dateTime.date.monthNumber.toString().padStart(2, '0')
            val day = dateTime.date.dayOfMonth.toString().padStart(2, '0')
            val hour = dateTime.time.hour.toString().padStart(2, '0')
            val minute = dateTime.time.minute.toString().padStart(2, '0')
            val second = dateTime.time.second.toString().padStart(2, '0')
            val nanosecond = dateTime.time.nanosecond.toString().padStart(9, '0').take(3)
            return "$year-$month-$day $hour:$minute:$second.$nanosecond"
        }
        
        /**
         * Initialize KLogger with String path. Must be called before first use.
         */
        fun initialize(
            logPathString: String,
            platformLogger: PlatformLogger,
            maxBytes: Int = 2 * 1024 * 1024
        ): KLogger {
            val logPath = logPathString.toPath()
            return initialize(logPath, platformLogger, maxBytes)
        }
        
        /**
         * Get the singleton instance. Must call initialize() first.
         */
        fun getInstance(): KLogger {
            return instance ?: error("KLogger not initialized. Call initialize() first.")
        }
        
        // Convenient static methods
        fun log(tag: String, message: String) = getInstance().log(tag, message)
        fun logW(tag: String, message: String) = getInstance().logW(tag, message)
        fun logE(tag: String, message: String, throwable: Throwable? = null) = getInstance().logE(tag, message, throwable)
    }
}

