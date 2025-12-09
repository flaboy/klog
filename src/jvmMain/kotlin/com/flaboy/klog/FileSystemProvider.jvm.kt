package com.flaboy.klog

import okio.FileSystem

internal actual fun getDefaultFileSystem(): FileSystem = FileSystem.SYSTEM

