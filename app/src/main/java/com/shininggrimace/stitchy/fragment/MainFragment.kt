package com.shininggrimace.stitchy.fragment

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.shininggrimace.stitchy.adapters.ImageAdapter
import com.shininggrimace.stitchy.databinding.FragmentMainBinding
import com.shininggrimace.stitchy.viewmodel.ImagesViewModel
import kotlinx.coroutines.launch

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
            viewModel.imageSelections.collect {
                refreshView(it)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshView(images: List<Uri>) {
        val context = context ?: return
        if (images.size > 0) {
            binding.noImageWarning.visibility = View.GONE
            binding.selectedFiles.visibility = View.VISIBLE
            binding.selectedFiles.layoutManager = GridLayoutManager(context, 2)
            binding.selectedFiles.adapter = ImageAdapter(images)
        } else {
            binding.noImageWarning.visibility = View.VISIBLE
            binding.selectedFiles.visibility = View.INVISIBLE
        }
    }
}