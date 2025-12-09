package com.flaboy.klog

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
        platformLogger.log(tag, message)
        ringLogger.append("[$tag] $message", LEVEL_INFO)
    }
    
    /**
     * Log warning level message.
     */
    fun logW(tag: String, message: String) {
        platformLogger.logW(tag, message)
        ringLogger.append("WARNING [$tag] $message", LEVEL_WARNING)
    }
    
    /**
     * Log error level message.
     */
    fun logE(tag: String, message: String, throwable: Throwable? = null) {
        platformLogger.logE(tag, message, throwable)
        val errorMsg = if (throwable != null) {
            "$message: ${throwable.message}"
        } else {
            message
        }
        ringLogger.append("ERROR [$tag] $errorMsg", LEVEL_ERROR)
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

