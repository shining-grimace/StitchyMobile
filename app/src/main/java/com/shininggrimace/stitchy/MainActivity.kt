package com.shininggrimace.stitchy

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.shininggrimace.stitchy.databinding.ActivityMainBinding
import com.shininggrimace.stitchy.util.Options
import com.shininggrimace.stitchy.util.OptionsRepository
import com.shininggrimace.stitchy.util.TypedFileDescriptors
import com.shininggrimace.stitchy.viewmodel.ImagesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var pickImages: ActivityResultLauncher<PickVisualMediaRequest>

    init {
        // Load librust.so
        System.loadLibrary("rust")
    }

    companion object {
        @JvmStatic
        external fun runStitchy(
            config: String,
            inputFds: IntArray,
            inputMimeTypes: Array<String>,
            outputFd: Int,
            outputMimeType: String
        ): String?
    }

    private val onInputsSelected = ActivityResultCallback<List<Uri>> { uris ->
        val viewModel: ImagesViewModel by viewModels()
        viewModel.imageSelections.tryEmit(uris)
        lifecycleScope.launch(Dispatchers.IO) {

            // Mark loading
            viewModel.outputState.tryEmit(Pair(
                ImagesViewModel.OutputState.Loading,
                Unit))

            // Open input files
            val inputFds = TypedFileDescriptors.fromPaths(this@MainActivity, uris)
                .onFailure {
                    viewModel.outputState.tryEmit(Pair(
                        ImagesViewModel.OutputState.Failed,
                        it))
                    return@launch
                }
                .getOrThrow()

            // Get output file including MIME type
            val outputFile = File.createTempFile("stitch_preview", ".png", cacheDir)
            val outputFd = getRawFileDescriptor(outputFile)
                .onFailure {
                    viewModel.outputState.tryEmit(Pair(
                        ImagesViewModel.OutputState.Failed,
                        Exception("Cannot open output file")))
                    return@launch
                }
                .getOrThrow()

            val config = OptionsRepository.getOptions(this@MainActivity)
                ?: Options.default()
            val inputOptionsJson = config.toJson()
                .onFailure {
                    viewModel.outputState.tryEmit(Pair(
                        ImagesViewModel.OutputState.Failed,
                        Exception("Cannot convert options to JSON")))
                    return@launch
                }
                .getOrThrow()

            // Run Stitchy
            val errorMessage = runStitchy(
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
    }

    private fun getRawFileDescriptor(file: File): Result<Int> {
        return try {
            file.createNewFile()
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
            Result.success(pfd.detachFd())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pickImages = registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(),
            onInputsSelected)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            pickImages.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController(R.id.nav_host_fragment_content_main)
                    .navigate(R.id.action_MainFragment_to_SettingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}