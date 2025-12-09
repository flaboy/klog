package com.flaboy.klog

import okio.FileHandle
import okio.FileSystem
import okio.Path

/**
 * Binary ring-buffer logger with fixed file size.
 * Append writes `[len][timestamp][level][utf8][len]` and tail reads back from the current end.
 * 
 * Usage:
 * ```
 * // Platform layer provides path (e.g., Android: context.filesDir, iOS: NSFileManager)
 * val path = "/path/to/log.bin".toPath()
 * val config = LogConfig(maxBytes = 1024 * 1024) // 1MB
 * val logger = RingLogger(path, config)
 * ```
 */
class RingLogger(
    private val path: Path,
    private val config: LogConfig,
    private val fileSystem: FileSystem = getDefaultFileSystem()
) {
    private val handle: FileHandle

    private data class Header(
        val magic: Int,
        val formatVersion: Int,
        val bodySize: Int,
        val lastEnd: Int
    )

    init {
        val parent = path.parent!!
        if (!fileSystem.exists(parent)) {
            fileSystem.createDirectories(parent)
        }
        if (!fileSystem.exists(path)) {
            fileSystem.write(path, mustCreate = true) {}
        }
        handle = fileSystem.openReadWrite(path)
        if (handle.size() < config.maxBytes.toLong()) {
            handle.resize(config.maxBytes.toLong())
        }
        val header = ByteArray(HEADER_SIZE)
        handle.read(0, header, 0, HEADER_SIZE)
        val magic = readInt(header, 0)
        if (magic != MAGIC) {
            val bodySize = config.maxBytes - HEADER_SIZE
            writeHeader(bodySize, 0, config.formatVersion)
        }
    }

    private fun currentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }

    fun append(message: String, level: Byte): Int {
        val timestampMillis = currentTimeMillis()
        val header = readHeader()
        val bodySize = header.bodySize
        val messageBytes = message.encodeToByteArray()
        val payloadLen = TIMESTAMP_BYTES + LEVEL_BYTES + messageBytes.size
        if (payloadLen > bodySize) {
            return 0
        }
        val recordSize = payloadLen + LEN_BYTES + LEN_BYTES
        val buffer = ByteArray(recordSize)
        writeShort(buffer, 0, payloadLen)
        writeLong(buffer, LEN_BYTES, timestampMillis)
        buffer[LEN_BYTES + TIMESTAMP_BYTES] = level
        messageBytes.copyInto(
            buffer,
            LEN_BYTES + TIMESTAMP_BYTES + LEVEL_BYTES,
            0,
            messageBytes.size
        )
        writeShort(buffer, recordSize - LEN_BYTES, payloadLen)

        writeBody(header.lastEnd, buffer, recordSize, bodySize)
        val newEnd = wrap(header.lastEnd + recordSize, bodySize)
        writeHeader(bodySize, newEnd, config.formatVersion)
        return recordSize
    }

    fun tail(count: Int): List<LogRecord> {
        val header = readHeader()
        val bodySize = header.bodySize
        var cursor = header.lastEnd
        val result = mutableListOf<LogRecord>()
        while (result.size < count) {
            val payloadLen = readTailLength(cursor, bodySize)
            if (payloadLen < MIN_PAYLOAD || payloadLen > bodySize) {
                break
            }
            val recordStart = rewind(cursor, payloadLen + LEN_BYTES + LEN_BYTES, bodySize)
            val record = readRecord(recordStart, payloadLen, bodySize) ?: break
            cursor = recordStart
            result.add(record)
        }
        return result
    }

    fun since(cutoffMillis: Long, limit: Int): List<LogRecord> {
        val header = readHeader()
        val bodySize = header.bodySize
        var cursor = header.lastEnd
        val result = mutableListOf<LogRecord>()
        while (result.size < limit) {
            val payloadLen = readTailLength(cursor, bodySize)
            if (payloadLen < MIN_PAYLOAD || payloadLen > bodySize) {
                break
            }
            val recordStart = rewind(cursor, payloadLen + LEN_BYTES + LEN_BYTES, bodySize)
            val record = readRecord(recordStart, payloadLen, bodySize) ?: break
            cursor = recordStart
            if (record.timestampMillis < cutoffMillis) {
                break
            }
            result.add(record)
        }
        return result
    }

    private fun readRecord(start: Int, payloadLen: Int, bodySize: Int): LogRecord? {
        val totalSize = payloadLen + LEN_BYTES + LEN_BYTES
        val data = readBody(start, totalSize, bodySize)
        val headLen = readShort(data, 0)
        val tailLen = readShort(data, totalSize - LEN_BYTES)
        if (headLen != tailLen || headLen != payloadLen) {
            return null
        }
        val timestamp = readLong(data, LEN_BYTES)
        val level = data[LEN_BYTES + TIMESTAMP_BYTES]
        val msgLen = payloadLen - TIMESTAMP_BYTES - LEVEL_BYTES
        if (msgLen < 0) {
            return null
        }
        val message = data.decodeToString(
            LEN_BYTES + TIMESTAMP_BYTES + LEVEL_BYTES,
            LEN_BYTES + TIMESTAMP_BYTES + LEVEL_BYTES + msgLen
        )
        return LogRecord(timestamp, level, message)
    }

    private fun writeHeader(bodySize: Int, lastEnd: Int, version: Int) {
        val buffer = ByteArray(HEADER_SIZE)
        writeInt(buffer, 0, MAGIC)
        writeInt(buffer, 4, version)
        writeInt(buffer, 8, bodySize)
        writeInt(buffer, 12, lastEnd)
        handle.write(0, buffer, 0, HEADER_SIZE)
        handle.flush()
    }

    private fun readHeader(): Header {
        val buffer = ByteArray(HEADER_SIZE)
        handle.read(0, buffer, 0, HEADER_SIZE)
        val magic = readInt(buffer, 0)
        val version = readInt(buffer, 4)
        val bodySize = readInt(buffer, 8)
        val lastEnd = readInt(buffer, 12)
        return Header(magic, version, bodySize, lastEnd)
    }

    private fun readTailLength(cursor: Int, bodySize: Int): Int {
        val lenOffset = rewind(cursor, LEN_BYTES, bodySize)
        val bytes = readBody(lenOffset, LEN_BYTES, bodySize)
        return readShort(bytes, 0)
    }

    private fun writeBody(offset: Int, data: ByteArray, size: Int, bodySize: Int) {
        val absStart = bodyOffset(offset)
        val first = minOf(size, bodySize - offset)
        handle.write(absStart, data, 0, first)
        if (first < size) {
            val remaining = size - first
            handle.write(bodyOffset(0), data, first, remaining)
        }
        handle.flush()
    }

    private fun readBody(offset: Int, size: Int, bodySize: Int): ByteArray {
        val buffer = ByteArray(size)
        val absStart = bodyOffset(offset)
        val first = minOf(size, bodySize - offset)
        handle.read(absStart, buffer, 0, first)
        if (first < size) {
            val remaining = size - first
            handle.read(bodyOffset(0), buffer, first, remaining)
        }
        return buffer
    }

    private fun bodyOffset(offset: Int): Long {
        return HEADER_SIZE.toLong() + offset.toLong()
    }

    fun close() {
        handle.close()
    }

    private fun wrap(value: Int, mod: Int): Int {
        val r = value % mod
        return if (r < 0) r + mod else r
    }

    private fun rewind(pos: Int, delta: Int, mod: Int): Int {
        return wrap(pos - delta, mod)
    }

    private fun writeInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value ushr 24).toByte()
        buffer[offset + 1] = (value ushr 16).toByte()
        buffer[offset + 2] = (value ushr 8).toByte()
        buffer[offset + 3] = value.toByte()
    }

    private fun writeShort(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value ushr 8).toByte()
        buffer[offset + 1] = value.toByte()
    }

    private fun writeLong(buffer: ByteArray, offset: Int, value: Long) {
        buffer[offset] = (value ushr 56).toByte()
        buffer[offset + 1] = (value ushr 48).toByte()
        buffer[offset + 2] = (value ushr 40).toByte()
        buffer[offset + 3] = (value ushr 32).toByte()
        buffer[offset + 4] = (value ushr 24).toByte()
        buffer[offset + 5] = (value ushr 16).toByte()
        buffer[offset + 6] = (value ushr 8).toByte()
        buffer[offset + 7] = value.toByte()
    }

    private fun readInt(buffer: ByteArray, offset: Int): Int {
        val b0 = buffer[offset].toInt() and 0xFF
        val b1 = buffer[offset + 1].toInt() and 0xFF
        val b2 = buffer[offset + 2].toInt() and 0xFF
        val b3 = buffer[offset + 3].toInt() and 0xFF
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    private fun readShort(buffer: ByteArray, offset: Int): Int {
        val b0 = buffer[offset].toInt() and 0xFF
        val b1 = buffer[offset + 1].toInt() and 0xFF
        return (b0 shl 8) or b1
    }

    private fun readLong(buffer: ByteArray, offset: Int): Long {
        val b0 = buffer[offset].toLong() and 0xFF
        val b1 = buffer[offset + 1].toLong() and 0xFF
        val b2 = buffer[offset + 2].toLong() and 0xFF
        val b3 = buffer[offset + 3].toLong() and 0xFF
        val b4 = buffer[offset + 4].toLong() and 0xFF
        val b5 = buffer[offset + 5].toLong() and 0xFF
        val b6 = buffer[offset + 6].toLong() and 0xFF
        val b7 = buffer[offset + 7].toLong() and 0xFF
        return (b0 shl 56) or (b1 shl 48) or (b2 shl 40) or (b3 shl 32) or
            (b4 shl 24) or (b5 shl 16) or (b6 shl 8) or b7
    }

    private companion object {
        const val MAGIC = 0x4B4C4F47 // "KLOG"
        const val HEADER_SIZE = 16
        const val LEN_BYTES = 2
        const val TIMESTAMP_BYTES = 8
        const val LEVEL_BYTES = 1
        const val MIN_PAYLOAD = TIMESTAMP_BYTES + LEVEL_BYTES
    }
}

