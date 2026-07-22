package com.dejongdevelopment.golfps.tools

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.provider.Settings

object AppUtility {
    fun lockOrientation(activity: Activity, orientation: Int) {
        activity.requestedOrientation = orientation
    }

    fun lockPortrait(activity: Activity) {
        lockOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }

    fun lockLandscape(activity: Activity) {
        lockOrientation(activity, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    fun openAppSettings(context: Context): Boolean {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (exception: ActivityNotFoundException) {
            DebugLogger.report(exception, "Unable to open app settings.")
            false
        }
    }
}
