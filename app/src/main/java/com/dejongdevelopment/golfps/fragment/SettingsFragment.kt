package com.dejongdevelopment.golfps.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.databinding.FragmentSettingsBinding
import com.dejongdevelopment.golfps.tools.AnalyticsLogger
import com.dejongdevelopment.golfps.tools.AppUtility
import com.dejongdevelopment.golfps.tools.DebugLogger
import com.dejongdevelopment.golfps.tools.LocationPermissionTools

class SettingsFragment: Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var isUpdatingSettings = false

    private val canShareBitmoji: Boolean
        get() = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureInitialState()
        configureActions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        updateLocationPermissionState()
    }

    private fun configureInitialState() {
        isUpdatingSettings = true
        binding.unitsChipGroup.check(if (GolfApplication.metric) R.id.meterChip else R.id.yardChip)
        binding.displayModeChipGroup.check(
            if (GolfApplication.cupholderMode) R.id.cupholderDisplayChip else R.id.defaultDisplayChip
        )
        binding.locationShareSwitch.isChecked = GolfApplication.me.shareLocation
        binding.bitmojiShareSwitch.isChecked = GolfApplication.me.shareBitmoji
        updateBitmojiShareState()
        isUpdatingSettings = false
    }

    private fun configureActions() {
        binding.unitsChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isUpdatingSettings) return@setOnCheckedStateChangeListener

            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val isMetric = checkedId == R.id.meterChip
            GolfApplication.metric = isMetric
            AnalyticsLogger.log("click_switch_units")
            AnalyticsLogger.setUnits(isMetric)
        }

        binding.displayModeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (isUpdatingSettings) return@setOnCheckedStateChangeListener

            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val isCupholderMode = checkedId == R.id.cupholderDisplayChip
            GolfApplication.cupholderMode = isCupholderMode
            AnalyticsLogger.log("click_display_mode")
            AnalyticsLogger.setDisplayMode(isDefault = !isCupholderMode)
        }

        binding.locationShareSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSettings) return@setOnCheckedChangeListener

            GolfApplication.me.shareLocation = isChecked
            AnalyticsLogger.log("click_map_share")

            if (!isChecked) {
                stopSharingPlayerLocation()
            }

            updateBitmojiShareState()
        }

        binding.bitmojiShareSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingSettings) return@setOnCheckedChangeListener

            if (!binding.bitmojiShareSwitch.isEnabled && isChecked) {
                setBitmojiShareSwitchChecked(false)
                return@setOnCheckedChangeListener
            }

            GolfApplication.me.shareBitmoji = isChecked
            AnalyticsLogger.log("click_bitmoji_share")
        }

        binding.locationShareInfoButton.setOnClickListener {
            showInfoDialog(
                title = getString(R.string.settings_location_share_info_title),
                message = getString(R.string.settings_location_share_info)
            )
        }

        binding.bitmojiShareInfoButton.setOnClickListener {
            showInfoDialog(
                title = getString(R.string.settings_bitmoji_share_info_title),
                message = getString(R.string.settings_bitmoji_share_info)
            )
        }

        binding.locationPermissionSettingsButton.setOnClickListener {
            AnalyticsLogger.log("click_location_permission_settings")
            if (!AppUtility.openAppSettings(requireContext())) {
                Toast.makeText(
                    requireContext(),
                    R.string.location_permission_unable_to_open_settings,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        binding.privacyButton.setOnClickListener {
            AnalyticsLogger.log("click_privacy_settings")
            openUrl(PRIVACY_URL)
        }

        binding.termsButton.setOnClickListener {
            AnalyticsLogger.log("click_terms_settings")
            openUrl(TERMS_URL)
        }
    }

    private fun stopSharingPlayerLocation() {
        GolfApplication.me.docReference?.delete()
            ?.addOnFailureListener { exception ->
                DebugLogger.report(exception, "Error removing shared player location")
            }

        GolfApplication.me.geoPoint = null
        GolfApplication.me.shareBitmoji = false
        setBitmojiShareSwitchChecked(false)
    }

    private fun updateBitmojiShareState() {
        val isEnabled = GolfApplication.me.shareLocation && canShareBitmoji

        binding.bitmojiShareSwitch.isEnabled = isEnabled
        binding.bitmojiShareLabel.alpha = if (isEnabled) 1f else 0.5f
        binding.bitmojiStatusLabel.alpha = if (isEnabled) 1f else 0.7f

        if (!isEnabled) {
            GolfApplication.me.shareBitmoji = false
            setBitmojiShareSwitchChecked(false)
        }
    }

    private fun setBitmojiShareSwitchChecked(isChecked: Boolean) {
        isUpdatingSettings = true
        binding.bitmojiShareSwitch.isChecked = isChecked
        isUpdatingSettings = false
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.settings_ok, null)
            .show()
    }

    private fun updateLocationPermissionState() {
        if (_binding == null) {
            return
        }

        val isAllowed = LocationPermissionTools.hasLocationPermission(context)
        binding.locationPermissionStatus.text = getString(
            if (isAllowed) {
                R.string.settings_location_permission_allowed
            } else {
                R.string.settings_location_permission_denied
            }
        )
        binding.locationPermissionSettingsButton.text = getString(
            if (isAllowed) {
                R.string.location_permission_manage_settings
            } else {
                R.string.location_permission_open_settings
            }
        )
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            DebugLogger.report(exception, "Unable to open settings link")
            Toast.makeText(
                requireContext(),
                R.string.settings_unable_to_open_link,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val PRIVACY_URL = "https://golfps-dejongdevelopment.firebaseapp.com/privacy_policy.html"
        private const val TERMS_URL = "https://golfps-dejongdevelopment.firebaseapp.com/terms_and_conditions.html"
    }
}
