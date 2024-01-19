package com.shininggrimace.stitchy

import android.net.Uri
import android.os.Bundle
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
import com.shininggrimace.stitchy.util.TypedFileDescriptors
import com.shininggrimace.stitchy.viewmodel.ImagesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        external fun runStitchy(inputFds: IntArray, inputMimeTypes: Array<String>): String
    }

    private val onInputsSelected = ActivityResultCallback<List<Uri>> { uris ->
        val viewModel: ImagesViewModel by viewModels()
        viewModel.imageSelections.tryEmit(uris)
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.outputState.tryEmit(Pair(
                ImagesViewModel.OutputState.Loading,
                Unit))
            val inputFilesResult = TypedFileDescriptors.fromPaths(this@MainActivity, uris)
            inputFilesResult.getOrNull()?.let { typedFiles ->
                val message = runStitchy(typedFiles.fileFds, typedFiles.mimeTypes)
                viewModel.outputState.tryEmit(Pair(
                    ImagesViewModel.OutputState.Completed,
                    message))
            } ?: run {
                viewModel.outputState.tryEmit(Pair(
                    ImagesViewModel.OutputState.Failed,
                    inputFilesResult.exceptionOrNull() ?: Unit))
            }
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