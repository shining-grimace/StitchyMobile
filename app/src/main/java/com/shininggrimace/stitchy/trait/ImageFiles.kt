package com.shininggrimace.stitchy.trait

import android.net.Uri
import android.os.ParcelFileDescriptor
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

interface ImageFiles {

    private fun activity(): FragmentActivity = this as FragmentActivity

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

        // Get output file including MIME type
        val outputFile = getTempOutputFile()
        val outputFd = getRawFileDescriptor(outputFile)
            .onFailure {
                viewModel.outputState.tryEmit(Pair(
                    ImagesViewModel.OutputState.Failed,
                    it))
                return@launch
            }
            .getOrThrow()

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

        // Run Stitchy
        val errorMessage = MainActivity.runStitchy(
            inputOptionsJson,
            inputFds.fileFds,
            inputFds.mimeTypes,
            outputFd,
            "image/png"
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

    fun saveStitchOutput(): Result<Unit> {
        val file = getTempOutputFile()
        TODO()
    }

    private fun getTempOutputFile(): File =
        File.createTempFile("stitch_preview", ".png", activity().cacheDir)

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
}
