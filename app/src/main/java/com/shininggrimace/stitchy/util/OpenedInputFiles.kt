package com.shininggrimace.stitchy.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.shininggrimace.stitchy.R
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class OpenedInputFiles private constructor(
    val buffers: Array<ByteBuffer>,
    val mimeTypes: Array<String>
) {
    companion object {

        @SuppressLint("Recycle")
        fun fromPaths(context: Context, uris: List<Uri>): Result<OpenedInputFiles> {

            val buffers = mutableListOf<ByteBuffer>()
            val mimes = mutableListOf<String>()

            val resolver = context.contentResolver
            try {
                for (uri in uris) {
                    val mime = resolver.getType(uri)
                        ?: throw Exception("Error checking MIME type")
                    val stream = resolver.openInputStream(uri)
                        ?: throw Exception("Error opening an input file")
                    val buffer = stream.use { inputStream ->
                        streamToDirectBuffer(inputStream)
                    }
                    mimes.add(mime)
                    buffers.add(buffer)
                }
            } catch (e: Exception) {
                Timber.e(e)
                return Result.failure(Exception(
                    context.getString(R.string.error_accessing_files)))
            }

            return Result.success(
                OpenedInputFiles(
                    buffers.toTypedArray(),
                    mimes.toTypedArray()
                ))
        }

        private fun streamToDirectBuffer(inputStream: InputStream): ByteBuffer {
            val bytes = ByteArrayOutputStream(0).use { bufferStream ->
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
}