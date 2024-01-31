package com.shininggrimace.stitchy.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.navigation.fragment.findNavController
import com.shininggrimace.stitchy.MainActivity
import com.shininggrimace.stitchy.R
import com.shininggrimace.stitchy.databinding.FragmentSettingsBinding
import com.shininggrimace.stitchy.util.Options
import com.shininggrimace.stitchy.util.OptionsRepository
import java.lang.NumberFormatException

class SettingsFragment : Fragment(), MenuProvider {

    private var _binding: FragmentSettingsBinding? = null

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as? MenuHost)?.addMenuProvider(this)
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.settingFormatInput.setOnItemClickListener { _, _, position, _ ->
            binding.settingQualityLayout.isEnabled = position == 0
        }
        binding.settingSizeLimitInput.setOnItemClickListener { _, _, position, _ ->
            binding.settingPixelsLayout.isEnabled = position != 0
        }
        prefillOptions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MenuHost)?.removeMenuProvider(this)
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_save -> {
                val saved = saveOptions()
                if (saved) {
                    findNavController().navigate(R.id.action_SettingsFragment_to_MainFragment)
                }
                return saved
            }
            else -> false
        }
    }

    private fun toast(@StringRes message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun saveOptions(): Boolean {
        val options = retrieveOptions()
            .onFailure {
                MainActivity.logException(it)
                toast(R.string.error_saving_settings)
                return false
            }
            .getOrThrow()
        OptionsRepository.saveOptions(requireContext(), options)
            .onFailure {
                toast(R.string.error_saving_settings)
                return false
            }
        return true
    }

    private fun prefillOptions() {

        val options = OptionsRepository.getOptions(requireContext()) ?: Options.default()

        val formatPosition = when {
            options.jpeg -> {
                binding.settingQualityLayout.isEnabled = true
                binding.settingQualityInput.setText(options.quality.toString())
                0
            }
            options.png -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingQualityInput.setText(0.toString())
                1
            }
            options.gif -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingQualityInput.setText(0.toString())
                2
            }
            options.bmp -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingQualityInput.setText(0.toString())
                3
            }
            else -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingQualityInput.setText(0.toString())
                1
            }
        }
        val formatText = binding.settingFormatInput.adapter
            .getItem(formatPosition).toString()
        binding.settingFormatInput.setText(formatText, false)

        val arrangementPosition = when {
            options.horizontal -> 1
            options.vertical -> 2
            else -> 0
        }
        val arrangementText = binding.settingArrangementInput.adapter
            .getItem(arrangementPosition).toString()
        binding.settingArrangementInput.setText(arrangementText, false)

        val sizeLimitPosition = when {
            options.maxd > 0 -> {
                binding.settingPixelsLayout.isEnabled = true
                binding.settingPixelsInput.setText(options.maxd.toString())
                1
            }
            options.maxw > 0 -> {
                binding.settingPixelsLayout.isEnabled = true
                binding.settingPixelsInput.setText(options.maxw.toString())
                2
            }
            options.maxh > 0 -> {
                binding.settingPixelsLayout.isEnabled = true
                binding.settingPixelsInput.setText(options.maxh.toString())
                3
            }
            else -> {
                binding.settingPixelsLayout.isEnabled = false
                binding.settingPixelsInput.setText(0.toString())
                0
            }
        }
        val sizeLimitText = binding.settingSizeLimitInput.adapter
            .getItem(sizeLimitPosition).toString()
        binding.settingSizeLimitInput.setText(sizeLimitText, false)
    }

    private fun retrieveOptions(): Result<Options> {

        val formatText = binding.settingFormatInput.text.toString()
        val formatStrings = resources.getStringArray(R.array.setting_format_items)
        val formatPosition = formatStrings.indexOf(formatText)
        if (formatPosition < 0) {
            return Result.failure(Exception("Cannot find selected format"))
        }

        val qualityNumber = if (binding.settingQualityLayout.isEnabled) {
            try {
                binding.settingQualityInput.text?.toString()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toInt() ?: 0
            } catch (e: NumberFormatException) {
                0
            }
        } else {
            0
        }

        val arrangementText = binding.settingArrangementInput.text.toString()
        val arrangementStrings = resources.getStringArray(R.array.setting_arrangement_items)
        val arrangementPosition = arrangementStrings.indexOf(arrangementText)
        if (arrangementPosition < 0) {
            return Result.failure(Exception("Cannot find selected arrangement"))
        }

        val sizeLimitText = binding.settingSizeLimitInput.text.toString()
        val sizeLimitStrings = resources.getStringArray(R.array.setting_size_limit_items)
        val sizeLimitPosition = sizeLimitStrings.indexOf(sizeLimitText)
        if (sizeLimitPosition < 0) {
            return Result.failure(Exception("Cannot find selected size limit"))
        }

        val pixelsNumber = if (binding.settingPixelsLayout.isEnabled) {
            try {
                binding.settingPixelsInput.text?.toString()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toInt() ?: 0
            } catch (e: NumberFormatException) {
                0
            }
        } else {
            0
        }

        val options = Options(
            horizontal = arrangementPosition == 1,
            vertical = arrangementPosition == 2,
            quality = qualityNumber,
            maxd = pixelsNumber.takeIf { sizeLimitPosition == 1 } ?: 0,
            maxw = pixelsNumber.takeIf { sizeLimitPosition == 2 } ?: 0,
            maxh = pixelsNumber.takeIf { sizeLimitPosition == 3 } ?: 0,
            jpeg = formatPosition == 0,
            png = formatPosition == 1,
            gif = formatPosition == 2,
            bmp = formatPosition == 3
        )
        return Result.success(options)
    }
}