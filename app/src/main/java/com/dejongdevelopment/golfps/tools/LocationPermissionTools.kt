package com.dejongdevelopment.golfps.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.dejongdevelopment.golfps.GolfApplication

object LocationPermissionTools {
    private const val LOCATION_PERMISSION_REQUESTED_KEY = "location_permission_requested"

    val wasRequested: Boolean
        get() = GolfApplication.preferences?.getBoolean(LOCATION_PERMISSION_REQUESTED_KEY, false) ?: false

    fun markRequested() {
        GolfApplication.preferences
            ?.edit()
            ?.putBoolean(LOCATION_PERMISSION_REQUESTED_KEY, true)
            ?.apply()
    }

    fun hasLocationPermission(context: Context?): Boolean {
        context ?: return false

        val fineLocation = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocation = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        return fineLocation == PackageManager.PERMISSION_GRANTED ||
                coarseLocation == PackageManager.PERMISSION_GRANTED
    }
}
