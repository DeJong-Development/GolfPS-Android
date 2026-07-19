package com.dejongdevelopment.golfps.tools

import android.util.Log
import com.dejongdevelopment.golfps.BuildConfig
import com.dejongdevelopment.golfps.models.BagType
import com.dejongdevelopment.golfps.models.Course
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsLogger {
    fun log(name: String, parameters: Map<String, Any>? = null) {
        if (BuildConfig.DEBUG) {
            Log.d("ANALYTICS", name)
        }

        val bundle = android.os.Bundle()
        parameters?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        Firebase.analytics.logEvent(name, bundle)
    }

    fun selectCourse(course: Course) {
        log("select_course", mapOf("name" to course.name.lowercase()))
    }

    fun setUserProperty(value: String, name: String) {
        Firebase.analytics.setUserProperty(name, value)
    }

    fun setDefaultBag(bagType: BagType) {
        setUserProperty(bagType.rawValue, "default_bag")
    }

    fun setSnapchat(usingSnapchat: Boolean) {
        setUserProperty(if (usingSnapchat) "true" else "false", "snapchat")
    }

    fun setDisplayMode(isDefault: Boolean) {
        setUserProperty(if (isDefault) "default" else "cupholder", "displayMode")
    }

    fun setUnits(isMetric: Boolean) {
        setUserProperty(if (isMetric) "metric" else "english", "units")
    }
}
