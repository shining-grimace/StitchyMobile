package com.shininggrimace.stitchy.trait

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.shininggrimace.stitchy.MainActivity
import com.shininggrimace.stitchy.util.Options
import com.shininggrimace.stitchy.util.OptionsRepository
import com.shininggrimace.stitchy.util.TypedFileDescriptors
import com.shininggrimace.stitchy.viewmodel.ImagesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface ImageFiles {

    private fun activity(): FragmentActivity = (this as Fragment).requireActivity()

    fun processImageFiles(
        viewModel: ImagesViewModel,
        uris: List<Uri>
    ) = activity().lifecycleScope.launch(Dispatchers.IO) {

        // Mark loading
        viewModel.outputState.tryEmit(Pair(
            ImagesViewModel.OutputState.Loading,
            Unit))

        // Open input files
        val inputFds = TypedFileDescriptors.fromPaths(activity(), uris)
            .onFailure {
                viewModel.outputState.tryEmit(Pair(
                    ImagesViewModel.OutputState.Failed,
                    it))
                return@launch
            }
            .getOrThrow()

        // Get configuration, or use defaults if there were none
        val config = OptionsRepository.getOptions(activity())
            ?: Options.default()
        val inputOptionsJson = config.toJson()
            .onFailure {
                viewModel.outputState.tryEmit(Pair(
                    ImagesViewModel.OutputState.Failed,
                    it))
                return@launch
            }
            .getOrThrow()

        // Get output file including MIME type
        val outputFile = getTempOutputFile(config.getFileExtension())
        val outputFd = getRawFileDescriptor(outputFile)
            .onFailure {
                viewModel.outputState.tryEmit(Pair(
                    ImagesViewModel.OutputState.Failed,
                    it))
                return@launch
            }
            .getOrThrow()

        // Run Stitchy
        val errorMessage = MainActivity.runStitchy(
            inputOptionsJson,
            inputFds.fileFds,
            inputFds.mimeTypes,
            outputFd,
            config.getMimeType()
        ) ?: run {
            viewModel.outputState.tryEmit(Pair(
                ImagesViewModel.OutputState.Completed,
                outputFile.absolutePath))
            return@launch
        }

        // Update UI as completed
        viewModel.outputState.tryEmit(Pair(
            ImagesViewModel.OutputState.Failed,
            Exception(errorMessage)))
    }

    fun saveStitchOutput(outputFileAbsolutePath: String): Result<Pair<String, Uri>> {

        val fileExtension: String = outputFileAbsolutePath.lastIndexOf('.')
            .takeIf { it > 0 && it < outputFileAbsolutePath.length - 1 }
            ?.let { outputFileAbsolutePath.substring(it + 1) }
            ?: return Result.failure(Exception("Cannot save output - failed reading extension"))

        val inputFile = File(outputFileAbsolutePath)
        if (!inputFile.isFile) {
            return Result.failure(Exception("Cannot save output - file does not exist"))
        }

        val outputFileName = getOutputFileName(fileExtension)

        val resolver = activity().contentResolver
        val contentUri = resolver
            .insert(
                getImageCollection(),
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, outputFileName)
                }
            )
            ?: return Result.failure(Exception("A problem occurred writing the gallery"))

        try {
            val inputStream = inputFile.inputStream()
            val outputStream = resolver.openOutputStream(contentUri, "w") ?: run {
                return Result.failure(Exception("A problem occurred opening the file"))
            }
            sinkStream(inputStream, outputStream)
            inputStream.close()
            outputStream.close()
        } catch (e: FileNotFoundException) {
            return Result.failure(Exception("A problem occurred accessing the file"))
        } catch (e: IOException) {
            return Result.failure(Exception("A problem occurred writing the file"))
        }
        return Result.success(
            Pair(outputFileName, contentUri))
    }

    private fun getTempOutputFile(fileExtension: String): File =
        File.createTempFile("stitch_preview", ".$fileExtension", activity().cacheDir)

    private fun getRawFileDescriptor(file: File): Result<Int> {
        return try {
            file.createNewFile()
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
            Result.success(pfd.detachFd())
        } catch (e: Exception) {
            MainActivity.logException(e)
            Result.failure(Exception("Cannot open output file"))
        }
    }

    private fun getImageCollection(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL_PRIMARY
            )
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun getOutputFileName(fileExtension: String): String {
        val formatter = SimpleDateFormat("yyyyMMdd_hhmmss", Locale.getDefault())
        val timestamp = formatter.format(Date())
        return "stitch_$timestamp.$fileExtension"
    }

    @Throws(IOException::class)
    private fun sinkStream(input: FileInputStream, output: OutputStream) {
        val buffer = ByteArray(4096)
        while (true) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) {
                return
            }
            output.write(buffer, 0, bytesRead)
        }
    }
}
