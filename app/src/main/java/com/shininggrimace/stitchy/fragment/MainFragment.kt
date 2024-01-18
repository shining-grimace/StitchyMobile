package com.shininggrimace.stitchy.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.shininggrimace.stitchy.R
import com.shininggrimace.stitchy.adapters.ImageAdapter
import com.shininggrimace.stitchy.databinding.FragmentMainBinding
import com.shininggrimace.stitchy.viewmodel.ImagesViewModel
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateOutputState(ImagesViewModel.OutputState.Empty, Unit)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showImageSelections(images: List<Uri>) {
        val context = context ?: return
        if (images.size > 0) {
            binding.selectedFiles.visibility = View.VISIBLE
            binding.selectedFiles.layoutManager = GridLayoutManager(context, 2)
            binding.selectedFiles.adapter = ImageAdapter(images)
        } else {
            binding.selectedFiles.visibility = View.INVISIBLE
        }
    }

    private fun updateOutputState(state: ImagesViewModel.OutputState, payload: Any) {
        when (state) {
            ImagesViewModel.OutputState.Empty -> {
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.VISIBLE
                binding.outputLabel.setText(R.string.no_images_selected)
            }
            ImagesViewModel.OutputState.Loading -> {
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.VISIBLE
                binding.outputLabel.setText(R.string.no_images_selected)
            }
            ImagesViewModel.OutputState.Completed -> {
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.VISIBLE
                binding.outputLabel.text = (payload as? String)
                    ?: getString(R.string.no_message_generated)
            }
            ImagesViewModel.OutputState.Failed -> {
                binding.outputLoading.visibility = View.GONE
                binding.outputLabel.visibility = View.VISIBLE
                binding.outputLabel.text = (payload as? Exception)?.message
                    ?: getString(R.string.no_message_generated)
            }
        }
    }
}