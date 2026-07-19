package com.dejongdevelopment.golfps.tools

import android.app.Activity
import android.content.pm.ActivityInfo

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
}
