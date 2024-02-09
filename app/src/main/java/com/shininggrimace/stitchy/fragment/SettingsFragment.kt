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
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.shininggrimace.stitchy.R
import com.shininggrimace.stitchy.databinding.FragmentSettingsBinding
import com.shininggrimace.stitchy.util.Options
import com.shininggrimace.stitchy.util.OptionsRepository
import timber.log.Timber
import java.lang.NumberFormatException

class SettingsFragment : Fragment(), MenuProvider {

    private data class ValidatedNumber(
        val matchesUserInput: Boolean,
        val resultingNumber: Int
    )

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
        applyListeners()
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
                    findNavController().navigateUp()
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
                Timber.e(it)
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

    private fun applyListeners() {
        binding.settingFormatInput.setOnItemClickListener { _, _, position, _ ->
            binding.settingQualityLayout.isEnabled = position == 0
            updateValidationMessage()
        }
        binding.settingQualityInput.doOnTextChanged { _, _, _, _ ->
            updateValidationMessage()
        }
        binding.settingSizeLimitInput.setOnItemClickListener { _, _, position, _ ->
            binding.settingPixelsLayout.isEnabled = position != 0
            updateValidationMessage()
        }
        binding.settingPixelsInput.doOnTextChanged { _, _, _, _ ->
            updateValidationMessage()
        }
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
            Timber.e("Cannot find selected format")
            return Result.failure(Exception(
                getString(R.string.error_parsing_setting)))
        }

        val qualityNumber = validateQuality().resultingNumber

        val arrangementText = binding.settingArrangementInput.text.toString()
        val arrangementStrings = resources.getStringArray(R.array.setting_arrangement_items)
        val arrangementPosition = arrangementStrings.indexOf(arrangementText)
        if (arrangementPosition < 0) {
            Timber.e("Cannot find selected arrangement")
            return Result.failure(Exception(
                getString(R.string.error_parsing_setting)))
        }

        val sizeLimitText = binding.settingSizeLimitInput.text.toString()
        val sizeLimitStrings = resources.getStringArray(R.array.setting_size_limit_items)
        val sizeLimitPosition = sizeLimitStrings.indexOf(sizeLimitText)
        if (sizeLimitPosition < 0) {
            Timber.e("Cannot find selected size limit")
            return Result.failure(Exception(
                getString(R.string.error_parsing_setting)))
        }

        val pixelsNumber = validateDimension().resultingNumber

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

    private fun updateValidationMessage() {

        val isQualitySensible = validateQuality().matchesUserInput
        val isDimensionSensible = validateDimension().matchesUserInput

        if (isQualitySensible && isDimensionSensible) {
            binding.validationMessage.visibility = View.GONE
            return
        }

        binding.validationMessage.visibility = View.VISIBLE
        if (isQualitySensible) {
            binding.validationMessage.setText(R.string.validation_dimension_not_sensible)
        } else if (isDimensionSensible) {
            binding.validationMessage.setText(R.string.validation_quality_not_sensible)
        } else {
            val qualityMessage = getString(R.string.validation_quality_not_sensible)
            val dimensionMessage = getString(R.string.validation_dimension_not_sensible)
            binding.validationMessage.text =
                getString(R.string.text_concat_newline, qualityMessage, dimensionMessage)
        }
    }

    private fun validateQuality(): ValidatedNumber {

        if (!binding.settingQualityLayout.isEnabled) {
            return ValidatedNumber(true, 0)
        }

        val textInput = binding.settingQualityInput.text?.toString()?.takeIf { it.isNotEmpty() }
            ?: return ValidatedNumber(false, Options.DEFAULT_JPEG_QUALITY)

        val number = try {
            textInput.toInt()
        } catch (e: NumberFormatException) {
            return ValidatedNumber(false, Options.DEFAULT_JPEG_QUALITY)
        }

        return when (number in 0..100) {
            true -> ValidatedNumber(true, number)
            false -> ValidatedNumber(false, Options.DEFAULT_JPEG_QUALITY)
        }
    }

    private fun validateDimension(): ValidatedNumber {

        if (!binding.settingPixelsLayout.isEnabled) {
            return ValidatedNumber(true, 0)
        }

        val textInput = binding.settingPixelsInput.text?.toString()?.takeIf { it.isNotEmpty() }
            ?: return ValidatedNumber(false, 0)

        val number = try {
            textInput.toInt()
        } catch (e: NumberFormatException) {
            return ValidatedNumber(false, 0)
        }

        return when (number) {
            0 -> ValidatedNumber(false, 0)
            else -> ValidatedNumber(true, number)
        }
    }
}