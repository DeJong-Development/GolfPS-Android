package com.dejongdevelopment.golfps.tools

import android.util.Log
import com.dejongdevelopment.golfps.BuildConfig
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.ktx.Firebase

object DebugLogger {
    fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("GolfPS", message)
        }
        Firebase.crashlytics.log(message)
    }

    fun report(error: Exception?, message: String? = null, isFatal: Boolean = false) {
        if (error == null) {
            message?.let {
                if (BuildConfig.DEBUG) {
                    Log.e("GolfPS", it)
                }
                Firebase.crashlytics.log(it)
            }
            return
        }

        val errorDescription = "${message ?: "Error"}: ${error.localizedMessage}"
        if (BuildConfig.DEBUG) {
            Log.e("GolfPS", errorDescription, error)
        }

        message?.let { Firebase.crashlytics.log(it) }
        Firebase.crashlytics.recordException(error)

        if (error is FirebaseFirestoreException && BuildConfig.DEBUG) {
            Log.e("GolfPS", "FIRESTORE ERROR CODE: ${error.code}")
        }

        if (isFatal) {
            throw error
        }
    }
}
