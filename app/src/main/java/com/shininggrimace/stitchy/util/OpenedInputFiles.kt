package com.shininggrimace.stitchy.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.shininggrimace.stitchy.R
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class OpenedInputFiles private constructor(
    private val resources: OpenResources,
    val mimeTypes: Array<String>
) {
    private class OpenResources(
        val streams: MutableList<FileInputStream> = mutableListOf(),
        val channels: MutableList<FileChannel> = mutableListOf(),
        val buffers: MutableList<ByteBuffer> = mutableListOf()
    ) {
        fun close() {
            for (channel in channels) {
                channel.close()
            }
            for (stream in streams) {
                stream.close()
            }
        }
    }

    companion object {

        @SuppressLint("Recycle")
        fun fromPaths(context: Context, uris: List<Uri>): Result<OpenedInputFiles> {

            val openResources = OpenResources()
            val mimes = mutableListOf<String>()

            val resolver = context.contentResolver
            try {
                for (uri in uris) {
                    val mime = resolver.getType(uri)
                        ?: throw Exception("Error checking MIME type")
                    val fd = resolver.openFileDescriptor(uri, "r")?.fileDescriptor
                        ?: throw Exception("Error opening an input file")
                    val buffer = FileInputStream(fd).use { inputStream ->
                        streamToDirectBuffer(inputStream)
                    }
                    mimes.add(mime)
                    openResources.buffers.add(buffer)
                }
            } catch (e: Exception) {
                Timber.e(e)
                openResources.close()
                return Result.failure(Exception(
                    context.getString(R.string.error_accessing_files)))
            }

            return Result.success(
                OpenedInputFiles(
                    openResources,
                    mimes.toTypedArray()
                ))
        }

        private fun streamToDirectBuffer(inputStream: FileInputStream): ByteBuffer {
            val size = inputStream.channel.size().toInt()
            val bytes = ByteArrayOutputStream(size).use { bufferStream ->
                inputStream.copyTo(bufferStream).toInt()
                bufferStream.toByteArray()
            }
            val buffer = ByteBuffer.allocateDirect(bytes.size)
            buffer.order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.rewind()
            return buffer
        }
    }

    fun getBuffers(): Array<ByteBuffer> {
        return resources.buffers.toTypedArray()
    }

    fun closeAll() {
        resources.close()
    }
}