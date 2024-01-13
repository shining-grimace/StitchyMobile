package com.shininggrimace.stitchy

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.shininggrimace.stitchy.databinding.FragmentMainBinding

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
        refreshView(imageCount = 0)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshView(imageCount: Int) {
        if (imageCount > 0) {
            binding.noImageWarning.visibility = View.GONE
            binding.selectedFiles.visibility = View.VISIBLE
        } else {
            binding.noImageWarning.visibility = View.VISIBLE
            binding.selectedFiles.visibility = View.INVISIBLE
        }
    }
}