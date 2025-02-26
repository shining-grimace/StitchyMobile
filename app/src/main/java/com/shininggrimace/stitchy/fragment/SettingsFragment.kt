package com.shininggrimace.stitchy.fragment

import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.annotation.StringRes
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import com.shininggrimace.stitchy.BuildConfig
import com.shininggrimace.stitchy.R
import com.shininggrimace.stitchy.databinding.FragmentSettingsBinding
import com.shininggrimace.stitchy.util.Options
import com.shininggrimace.stitchy.util.OptionsRepository
import timber.log.Timber
import java.lang.NumberFormatException

private const val DEVELOPER_LINK_URL = "https://shininggrimace.com"

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
        configureLabels()
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

    private fun configureLabels() {
        binding.versionLabel.text = getString(R.string.app_with_version, BuildConfig.VERSION_NAME)
    }

    private fun applyListeners() {
        binding.settingFormatInput.setOnItemClickListener { _, _, position, _ ->
            binding.settingQualityLayout.isEnabled = position == 0
            binding.settingCompressionLayout.isEnabled = position == 1 || position == 2
            if (position == 1 || position == 2) {
                binding.settingCompressionLayout.isEnabled = true
            } else {
                binding.settingCompressionLayout.isEnabled = false
                binding.settingCompressionInput.setText(
                    resources.getStringArray(R.array.setting_compression_items)[0]
                )
            }
            updateValidationMessage()
        }
        binding.settingQualityInput.doOnTextChanged { _, _, _, _ ->
            updateValidationMessage()
        }
        binding.settingPixelsInput.doOnTextChanged { _, _, _, _ ->
            updateValidationMessage()
        }
        binding.settingCompressionInput.setOnItemClickListener { _, _, _, _ ->
            updateValidationMessage()
        }
        binding.settingFastInput.setOnItemClickListener { _, _, _, _ ->
            updateValidationMessage()
        }
        binding.openWebsite.setOnClickListener {
            openWebsite()
        }
    }

    private fun prefillOptions() {

        val options = OptionsRepository.getOptions(requireContext()) ?: Options.default()

        val formatPosition = when {
            options.jpeg -> {
                binding.settingQualityLayout.isEnabled = true
                binding.settingCompressionLayout.isEnabled = false
                0
            }
            options.png -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingCompressionLayout.isEnabled = true
                1
            }
            options.gif -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingCompressionLayout.isEnabled = true
                2
            }
            options.bmp -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingCompressionLayout.isEnabled = false
                3
            }
            options.webp -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingCompressionLayout.isEnabled = false
                4
            }
            else -> {
                binding.settingQualityLayout.isEnabled = false
                binding.settingCompressionLayout.isEnabled = true
                1
            }
        }
        val formatText = binding.settingFormatInput.adapter
            .getItem(formatPosition).toString()
        binding.settingFormatInput.setText(formatText, false)

        if (options.jpeg) {
            binding.settingQualityInput.setText(options.quality.toString())
        } else {
            binding.settingQualityInput.setText(Options.DEFAULT_JPEG_QUALITY.toString())
        }

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
                binding.settingPixelsInput.setText(options.maxd.toString())
                0
            }
            options.maxw > 0 -> {
                binding.settingPixelsInput.setText(options.maxw.toString())
                1
            }
            options.maxh > 0 -> {
                binding.settingPixelsInput.setText(options.maxh.toString())
                2
            }
            else -> {
                binding.settingPixelsInput.setText(Options.MAX_DIMENSION.toString())
                0
            }
        }
        val sizeLimitText = binding.settingSizeLimitInput.adapter
            .getItem(sizeLimitPosition).toString()
        binding.settingSizeLimitInput.setText(sizeLimitText, false)

        val compressionPosition = when ((options.png || options.gif) && options.small) {
            true -> 1
            false -> 0
        }
        val compressionText = binding.settingCompressionInput.adapter
            .getItem(compressionPosition).toString()
        binding.settingCompressionInput.setText(compressionText, false)

        val fastPosition = when (options.fast) {
            true -> 1
            false -> 0
        }
        val fastText = binding.settingFastInput.adapter
            .getItem(fastPosition).toString()
        binding.settingFastInput.setText(fastText, false)
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

        val compressionText = binding.settingCompressionInput.text.toString()
        val compressionStrings = resources.getStringArray(R.array.setting_compression_items)
        val compressionPosition = compressionStrings.indexOf(compressionText)
        if (compressionPosition < 0) {
            Timber.e("Cannot find selected compression option")
            return Result.failure(Exception(
                getString(R.string.error_parsing_setting)))
        }

        val fastText = binding.settingFastInput.text.toString()
        val fastStrings = resources.getStringArray(R.array.setting_fast_items)
        val fastPosition = fastStrings.indexOf(fastText)
        if (fastPosition < 0) {
            Timber.e("Cannot find selected fast option")
            return Result.failure(Exception(
                getString(R.string.error_parsing_setting)))
        }

        val options = Options(
            horizontal = arrangementPosition == 1,
            vertical = arrangementPosition == 2,
            quality = qualityNumber,
            small = compressionPosition == 1,
            fast = fastPosition == 1,
            maxd = pixelsNumber.takeIf { sizeLimitPosition == 0 } ?: 0,
            maxw = pixelsNumber.takeIf { sizeLimitPosition == 1 } ?: 0,
            maxh = pixelsNumber.takeIf { sizeLimitPosition == 2 } ?: 0,
            jpeg = formatPosition == 0,
            png = formatPosition == 1,
            gif = formatPosition == 2,
            bmp = formatPosition == 3,
            webp = formatPosition == 4
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

        val textInput = binding.settingPixelsInput.text?.toString()?.takeIf { it.isNotEmpty() }
            ?: return ValidatedNumber(false, Options.MAX_DIMENSION)

        val number = try {
            textInput.toInt()
        } catch (e: NumberFormatException) {
            return ValidatedNumber(false, Options.MAX_DIMENSION)
        }

        return when (number in 1..4096) {
            true -> ValidatedNumber(true, number)
            false -> ValidatedNumber(false, Options.MAX_DIMENSION)
        }
    }

    private fun openWebsite() {
        val activity = requireActivity()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_LINK_URL))
        val resolvedActivities = activity.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_ALL)
        if (resolvedActivities.isNotEmpty()) {
            activity.startActivity(intent)
        }
    }
}