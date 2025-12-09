package com.flaboy.klog

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.Path.Companion.toPath

/**
 * Unified logger that writes to both console and ring buffer file.
 * 
 * 团队统一日志接口：所有项目必须通过此类记录日志。
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
    private val platformLogger: PlatformLogger
) {
    /**
     * Log info level message.
     */
    fun log(tag: String, message: String) {
        val formattedMessage = formatLogMessage(tag, message)
        platformLogger.log(tag, formattedMessage)
        ringLogger.append(formattedMessage, LEVEL_INFO)
    }
    
    /**
     * Log warning level message.
     */
    fun logW(tag: String, message: String) {
        val formattedMessage = formatLogMessage(tag, message, "WARNING")
        platformLogger.logW(tag, formattedMessage)
        ringLogger.append(formattedMessage, LEVEL_WARNING)
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
        val formattedMessage = formatLogMessage(tag, errorMsg, "ERROR")
        platformLogger.logE(tag, formattedMessage, throwable)
        ringLogger.append(formattedMessage, LEVEL_ERROR)
    }
    
    private fun formatLogMessage(tag: String, message: String, level: String = ""): String {
        val timestamp = formatTimestamp(Clock.System.now())
        return if (level.isEmpty()) {
            "[$timestamp] [$tag] $message"
        } else {
            "[$timestamp] $level [$tag] $message"
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
            val logger = KLogger(ringLogger, platformLogger)
            instance = logger
            return logger
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

