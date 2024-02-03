package com.shininggrimace.stitchy.fragment

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.shininggrimace.stitchy.R
import com.shininggrimace.stitchy.adapters.ImageAdapter
import com.shininggrimace.stitchy.databinding.FragmentMainBinding
import com.shininggrimace.stitchy.trait.ImageFiles
import com.shininggrimace.stitchy.viewmodel.ImagesViewModel
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment(), MenuProvider, ImageFiles {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private lateinit var pickImages: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? MenuHost)?.addMenuProvider(this)
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val viewModel: ImagesViewModel by activityViewModels()
        lifecycleScope.launch {
            viewModel.imageSelections.collect { uris ->
                showImageSelections(uris)
            }
        }
        lifecycleScope.launch {
            viewModel.outputState.collect { (state, payload) ->
                updateOutputState(state, payload)
            }
        }
        binding.selectImagesFab.setOnClickListener {
            pickImages.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        pickImages = registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(),
            onInputsSelected)
        binding.exportOutputFab.setOnClickListener {
            val message = exportOutputFile()
                .run {
                    when (isSuccess) {
                        true -> getOrThrow()
                        false -> exceptionOrNull()?.message ?: "(No message)"
                    }
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateOutputState(ImagesViewModel.OutputState.Empty, Unit)
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
        val viewModel: ImagesViewModel by activityViewModels()
        viewModel.imageSelections.tryEmit(uris)
        processImageFiles(viewModel, uris)
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

    private fun exportOutputFile(): Result<String> {
        val viewModel: ImagesViewModel by activityViewModels()
        val outputState = viewModel.outputState.value
        if (outputState.first != ImagesViewModel.OutputState.Completed) {
            return Result.failure(Exception("The stitch output doesn't appear to be ready"))
        }
        return saveStitchOutput(outputState.second as String)
    }

    private fun updateOutputState(state: ImagesViewModel.OutputState, payload: Any) {
        when (state) {
            ImagesViewModel.OutputState.Empty -> {
                binding.exportOutputFab.visibility = View.GONE
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.VISIBLE
                binding.stitchPreview.visibility = View.INVISIBLE
                binding.outputLabel.setText(R.string.no_images_selected)
            }
            ImagesViewModel.OutputState.Loading -> {
                binding.exportOutputFab.visibility = View.GONE
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.VISIBLE
                binding.stitchPreview.visibility = View.INVISIBLE
                binding.outputLabel.setText(R.string.no_images_selected)
            }
            ImagesViewModel.OutputState.Completed -> {
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
            ImagesViewModel.OutputState.Failed -> {
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