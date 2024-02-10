package com.shininggrimace.stitchy.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.shininggrimace.stitchy.R
import com.shininggrimace.stitchy.adapter.ImageAdapter
import com.shininggrimace.stitchy.databinding.FragmentMainBinding
import com.shininggrimace.stitchy.trait.ImageFiles
import com.shininggrimace.stitchy.util.ExportResult
import com.shininggrimace.stitchy.viewmodel.ImagesViewModel
import timber.log.Timber

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment(), MenuProvider, ImageFiles {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var pickImages: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private val viewModel: ImagesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? MenuHost)?.addMenuProvider(this)
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        viewModel.getImageSelections().observe(viewLifecycleOwner) { uris ->
            showImageSelections(uris)
        }
        viewModel.getOutputState().observe(viewLifecycleOwner) { (state, payload) ->
            updateOutputState(state, payload)
        }
        binding.clearImagesFab.setOnClickListener {
            clearInputs()
        }
        binding.selectImagesFab.setOnClickListener {
            pickImages.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        pickImages = registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(),
            onInputsSelected)
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    exportGivenPermission()
                }
            }
        binding.exportOutputFab.setOnClickListener {
            checkPermissionAndExport()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateOutputState(ImagesViewModel.ProcessingState.Empty, Unit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MenuHost)?.removeMenuProvider(this)
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_main, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_settings -> {
                findNavController()
                    .navigate(R.id.action_MainFragment_to_SettingsFragment)
                true
            }
            else -> false
        }
    }

    private val onInputsSelected = ActivityResultCallback<List<Uri>> { uris ->
        if (uris.isEmpty()) {
            return@ActivityResultCallback
        }
        val existingList = viewModel.getImageSelections().value ?: emptyList()
        val totalList = existingList + uris
        viewModel.postImageSelections(totalList)
        processImageFiles(requireActivity(), viewModel, totalList)
    }

    private fun clearInputs() {
        viewModel.postImageSelections(listOf())
    }

    private fun showImageSelections(images: List<Uri>) {
        val context = context ?: return
        if (images.isNotEmpty()) {
            binding.selectedFiles.visibility = View.VISIBLE
            binding.selectedFiles.layoutManager = GridLayoutManager(context, 2)
            binding.selectedFiles.adapter = ImageAdapter(images)
        } else {
            binding.selectedFiles.visibility = View.INVISIBLE
        }
    }

    private fun checkPermissionAndExport() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportGivenPermission()
            return
        }

        val permissionResult = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permissionResult == PackageManager.PERMISSION_GRANTED) {
            exportGivenPermission()
            return
        }

        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun exportGivenPermission() {
        processExportResult(
            exportOutputFile())
    }

    private fun exportOutputFile(): Result<ExportResult> {
        val outputState = viewModel.getOutputState().value
            ?: ImagesViewModel.OutputUpdate(ImagesViewModel.ProcessingState.Empty, Unit)
        if (outputState.state != ImagesViewModel.ProcessingState.Completed) {
            Timber.e("The stitch output doesn't appear to be ready")
            return Result.failure(Exception(
                getString(R.string.error_cannot_write_media)))
        }
        return saveStitchOutput(outputState.data as String)
    }

    private fun processExportResult(result: Result<ExportResult>) {

        if (result.isFailure) {
            val message = result.exceptionOrNull()?.message ?: "(No message)"
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                .show()
            return
        }

        val exportResult = result.getOrThrow()
        Snackbar.make(binding.root, exportResult.fileName, Snackbar.LENGTH_LONG)
            .setAction("Open") {
                openImageInGallery(exportResult.uri)
            }
            .show()
    }

    private fun openImageInGallery(contentUri: Uri) {
        val activity = activity ?: return
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(contentUri, "image/*")
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        }
    }

    private fun updateOutputState(state: ImagesViewModel.ProcessingState, payload: Any) {
        when (state) {
            ImagesViewModel.ProcessingState.Empty -> {
                binding.exportOutputFab.visibility = View.GONE
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.VISIBLE
                binding.stitchPreview.visibility = View.INVISIBLE
                binding.outputLabel.setText(R.string.no_images_selected)
            }
            ImagesViewModel.ProcessingState.Loading -> {
                binding.exportOutputFab.visibility = View.GONE
                binding.outputLoading.visibility = View.VISIBLE
                binding.outputLabel.visibility = View.GONE
                binding.stitchPreview.visibility = View.INVISIBLE
            }
            ImagesViewModel.ProcessingState.Completed -> {
                binding.exportOutputFab.visibility = View.VISIBLE
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.GONE
                binding.stitchPreview.visibility = View.VISIBLE
                (payload as? String)?.let { file ->
                    binding.stitchPreview.setImageBitmap(
                        BitmapFactory.decodeFile(file)
                    )
                }
            }
            ImagesViewModel.ProcessingState.Failed -> {
                binding.exportOutputFab.visibility = View.GONE
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.VISIBLE
                binding.stitchPreview.visibility = View.INVISIBLE
                binding.outputLabel.text = (payload as? Exception)?.message
                    ?: getString(R.string.no_message_generated)
            }
        }
    }
}