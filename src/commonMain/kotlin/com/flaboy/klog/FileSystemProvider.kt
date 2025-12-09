package com.flaboy.klog

import okio.FileSystem

/**
 * Platform-specific FileSystem provider.
 * Uses expect/actual to provide the correct FileSystem for each platform.
 */
internal expect fun getDefaultFileSystem(): FileSystem

