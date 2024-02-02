package com.shininggrimace.stitchy

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.shininggrimace.stitchy.databinding.ActivityMainBinding
import com.shininggrimace.stitchy.trait.ImageFiles
import com.shininggrimace.stitchy.viewmodel.ImagesViewModel

class MainActivity : AppCompatActivity(), ImageFiles {

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

        fun logException(e: Throwable) {
            Log.e("StitchyMobile", e.message ?: "(Unknown exception)")
        }
    }

    private val onInputsSelected = ActivityResultCallback<List<Uri>> { uris ->
        val viewModel: ImagesViewModel by viewModels()
        viewModel.imageSelections.tryEmit(uris)
        processImageFiles(viewModel, uris)
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

        binding.selectImagesFab.setOnClickListener {
            pickImages.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}