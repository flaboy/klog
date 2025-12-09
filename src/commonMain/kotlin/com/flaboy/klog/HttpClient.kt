package com.flaboy.klog

/**
 * HTTP client interface for klog report functionality.
 * Projects must implement this interface and provide it during initialization.
 * 
 * 团队规范：项目必须实现此接口以支持日志上报功能。
 */
interface HttpClient {
    /**
     * POST request
     * 
     * @param url Target URL
     * @param headers HTTP headers
     * @param body Request body (raw bytes)
     * @return HTTP response
     */
    suspend fun post(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray
    ): HttpResponse
    
    /**
     * PUT request (for S3 upload)
     * 
     * @param url Target URL
     * @param headers HTTP headers
     * @param body Request body (raw bytes)
     * @return HTTP response
     */
    suspend fun put(
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray
    ): HttpResponse
}

/**
 * HTTP response
 */
data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap()
) {
    val isSuccess: Boolean
        get() = statusCode in 200..299
}


