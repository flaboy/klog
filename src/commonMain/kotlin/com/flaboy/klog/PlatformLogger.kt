package com.flaboy.klog

/**
 * Platform-specific console logger interface.
 * All projects using klog must implement this interface to maintain consistency.
 * 
 * 团队日志规范：所有项目必须实现此接口以保持日志输出的一致性。
 * 
 * 注意：message 参数已包含完整的格式化信息（时间戳、tag、level），平台实现只需原样输出。
 */
interface PlatformLogger {
    /**
     * Output formatted message to console.
     * @param tag 已废弃，保留用于兼容性
     * @param message 完整格式化的日志消息，格式: [TIMESTAMP] [LEVEL] [TAG] content
     */
    fun log(tag: String, message: String)
    
    /**
     * Output formatted warning to console.
     * @param tag 已废弃，保留用于兼容性
     * @param message 完整格式化的日志消息
     */
    fun logW(tag: String, message: String)
    
    /**
     * Output formatted error to console.
     * @param tag 已废弃，保留用于兼容性
     * @param message 完整格式化的日志消息
     * @param throwable 异常信息（已包含在 message 中）
     */
    fun logE(tag: String, message: String, throwable: Throwable? = null)
}

