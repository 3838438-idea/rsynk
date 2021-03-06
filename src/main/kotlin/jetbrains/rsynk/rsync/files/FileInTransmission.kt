/**
 * Copyright 2016 - 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.rsynk.rsync.files

import jetbrains.rsynk.rsync.exitvalues.InvalidFileException
import jetbrains.rsynk.rsync.extensions.use
import mu.KLogging
import java.io.InputStream
import java.nio.file.Files

internal class FileInTransmission(
        private val fileInfo: FileInfo,
        windowSize: Int,
        bufferSize: Int = 8 * 1024
) : AutoCloseable {

    companion object : KLogging()

    private val stream: InputStream
    val array: ByteArray

    private var remainingBytes = fileInfo.size
    private var windowLength: Int

    private var startOffset = 0
    private var endOffset = -1
    private var markOffset = -1
    private var readOffset = -1

    init {
        if (fileInfo.size > 0) {
            try {
                stream = Files.newInputStream(fileInfo.path)
                stream.skip(fileInfo.offset)
            } catch (t: Throwable) {
                val message = "Failed to open the file ${fileInfo.path}: ${t.message}"
                logger.error(t, { message })
                throw InvalidFileException(message, t)
            }
            windowLength = windowSize
            array = ByteArray(bufferSize)
            slide(0)
        } else {
            windowLength = 0
            array = ByteArray(0)
            stream = object : InputStream() {
                override fun read() = -1
            }
        }
    }

    fun setMarkOffsetRelativeToStart(relativeOffset: Int) {
        markOffset = startOffset + relativeOffset
    }

    fun getWindowLength(): Int {
        return endOffset - startOffset + 1
    }

    private fun getPrefetchedBytesCount(): Int {
        return readOffset - startOffset + 1
    }

    fun getStartOffset(): Int {
        return startOffset
    }

    fun getMarkOffset(): Int {
        return markOffset
    }

    fun getFirstOffset(): Int {
        if (markOffset >= 0) {
            return Math.min(startOffset, markOffset)
        }
        return startOffset
    }

    fun getEndOffset(): Int {
        return endOffset
    }


    private fun getMarkedBytesCount(): Int {
        return startOffset - getFirstOffset()
    }

    fun getTotalBytes(): Int {
        return endOffset - getFirstOffset() + 1
    }

    private fun getAvailableSpace(): Int {
        return array.size - 1 - readOffset
    }

    fun isFull(): Boolean {
        return getTotalBytes() == array.size
    }

    private fun readBetween(min: Int, max: Int) {
        var numBytesRead = 0
        while (numBytesRead < min) {
            val len = stream.read(array, readOffset + 1, max - numBytesRead)
            if (len <= 0) {
                throw InvalidFileException("File ended prematurely ($len of ${fileInfo.size} read)")
            }
            numBytesRead += len
            readOffset += len
            remainingBytes -= len.toLong()
        }
    }

    fun slide(shift: Int) {
        startOffset += shift
        val windowLength = Math.min(windowLength.toLong(), getPrefetchedBytesCount() + remainingBytes).toInt()
        val minBytesToRead = windowLength - getPrefetchedBytesCount()

        if (minBytesToRead > 0) {
            if (minBytesToRead > getAvailableSpace()) {
                realloc()
            }
            readBetween(minBytesToRead, Math.min(remainingBytes, getAvailableSpace().toLong()).toInt())
        }

        endOffset = startOffset + windowLength - 1
    }

    private fun realloc() {
        val shiftOffset = getFirstOffset()
        val numShifts = getMarkedBytesCount() + getPrefetchedBytesCount()
        System.arraycopy(array, shiftOffset, array, 0, numShifts)
        startOffset -= shiftOffset
        endOffset -= shiftOffset
        readOffset -= shiftOffset
        if (markOffset >= 0) {
            markOffset -= shiftOffset
        }
    }

    override fun close() {
        stream.close()
    }
}

internal object FilesTransmission {
    val defaultBlockSize = 8 * 1024

    fun <T> runWithOpenedFile(file: FileInfo,
                              windowLength: Int,
                              bufferSize: Int,
                              action: (FileInTransmission) -> T): T {
        FileInTransmission(file, windowLength, bufferSize).use { f ->
            return action(f)
        }
    }
}
