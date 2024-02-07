package com.shininggrimace.stitchy.util

import android.content.Context
import android.net.Uri
import com.shininggrimace.stitchy.R
import timber.log.Timber

class TypedFileDescriptors private constructor(
    val fileFds: IntArray,
    val mimeTypes: Array<String>
) {
    companion object {
        fun fromPaths(context: Context, uris: List<Uri>): Result<TypedFileDescriptors> {

            val fds = mutableListOf<Int>()
            val mimes = mutableListOf<String>()

            val resolver = context.contentResolver
            try {
                for (uri in uris) {
                    val mime = resolver.getType(uri)
                        ?: throw Exception("Error checking MIME type")
                    val fd = resolver.openFileDescriptor(uri, "r")?.detachFd()
                        ?: throw Exception("Error opening an input file")
                    fds.add(fd)
                    mimes.add(mime)
                }
            } catch (e: Exception) {
                Timber.e(e)
                return Result.failure(Exception(
                    context.getString(R.string.error_accessing_files)))
            }

            return Result.success(
                TypedFileDescriptors(fds.toIntArray(), mimes.toTypedArray()))
        }
    }
}