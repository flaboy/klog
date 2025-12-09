package com.flaboy.klog

/**
 * Platform-specific console logger interface.
 * All projects using klog must implement this interface to maintain consistency.
 * 
 * 团队日志规范：所有项目必须实现此接口以保持日志输出的一致性。
 */
interface PlatformLogger {
    /**
     * Log info level message to console.
     */
    fun log(tag: String, message: String)
    
    /**
     * Log warning level message to console.
     */
    fun logW(tag: String, message: String)
    
    /**
     * Log error level message to console.
     */
    fun logE(tag: String, message: String, throwable: Throwable? = null)
}

