package com.flaboy.klog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okio.fakefilesystem.FakeFileSystem
import okio.Path.Companion.toPath

class RingLoggerTest {
    @Test
    fun appendAndTailReadsNewestFirst() {
        val fs = FakeFileSystem()
        val path = "/tmp/klog.bin".toPath()
        val config = LogConfig(maxBytes = 1024, formatVersion = 1)
        val logger = RingLogger(path, config, fs)
        logger.append("a", 1)
        logger.append("b", 1)
        logger.append("c", 1)
        val records = logger.tail(2)
        assertEquals(2, records.size)
        assertEquals("c", records[0].message)
        assertEquals("b", records[1].message)
    }

    @Test
    fun wrapsWhenExceedingBody() {
        val fs = FakeFileSystem()
        val path = "/tmp/klog.bin".toPath()
        val config = LogConfig(maxBytes = 1024, formatVersion = 1)
        val logger = RingLogger(path, config, fs)
        repeat(50) {
            logger.append("msg-$it", 1)
        }
        val records = logger.tail(5)
        assertEquals(5, records.size)
        assertTrue(records.first().message.startsWith("msg-"))
    }

    @Test
    fun readsSinceCutoff() {
        val fs = FakeFileSystem()
        val path = "/tmp/klog.bin".toPath()
        val config = LogConfig(maxBytes = 1024, formatVersion = 1)
        val logger = RingLogger(path, config, fs)
        logger.append("old", 1)
        Thread.sleep(50)
        val cutoff = System.currentTimeMillis()
        Thread.sleep(50)
        logger.append("new", 1)
        val records = logger.since(cutoff, 5)
        assertEquals(1, records.size)
        assertEquals("new", records[0].message)
    }

    @Test
    fun stopsOnCorruptLength() {
        val fs = FakeFileSystem()
        val path = "/tmp/klog.bin".toPath()
        val config = LogConfig(maxBytes = 1024, formatVersion = 1)
        val writer = RingLogger(path, config, fs)
        writer.append("good", 1)
        writer.append("bad", 1)
        writer.close()
        val headerSize = 16
        val corruptOffset = headerSize + 2
        val bytes = fs.read(path) { readByteArray() }
        bytes[corruptOffset] = 0x7F.toByte()
        bytes[corruptOffset + 1] = 0x7F.toByte()
        fs.write(path) { write(bytes) }
        val reader = RingLogger(path, config, fs)
        val records = reader.tail(5)
        assertTrue(records.isNotEmpty())
    }
}
