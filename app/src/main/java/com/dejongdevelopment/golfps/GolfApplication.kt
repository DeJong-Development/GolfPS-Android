package com.dejongdevelopment.golfps

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.dejongdevelopment.golfps.models.Course
import com.dejongdevelopment.golfps.models.Me

class GolfApplication : Application() {
    companion object {
        var preferences: SharedPreferences? = null
        private var appContext: Context? = null

        var course: Course? = null
        var me: Me = Me("offline")
        var testing: Boolean = false

        var metric:Boolean
            set(value) {
                this.preferences?.edit()?.let { editor ->
                    editor.putBoolean("using_metric", value)
                    editor.apply()
                }
            }
            get() = this.preferences?.getBoolean("using_metric", false) ?: false

        var cupholderMode:Boolean
            set(value) {
                this.preferences?.edit()?.let { editor ->
                    editor.putBoolean("cupholder_mode", value)
                    editor.apply()
                }
            }
            get() = this.preferences?.getBoolean("cupholder_mode", false) ?: false

        fun valueForAPIKey(keyName: String): String {
            val context = appContext ?: return "no-key-found"
            val resourceId = context.resources.getIdentifier(keyName, "string", context.packageName)
            if (resourceId == 0) {
                return "no-key-found"
            }
            return context.getString(resourceId)
        }
    }

    override fun onCreate() {
        super.onCreate()

        appContext = this
        preferences = getSharedPreferences("golf-preferences", Context.MODE_PRIVATE)

//        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//        } else {
//            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
//        }

//        if (BuildConfig.DEBUG) {
//        }
    }
}
