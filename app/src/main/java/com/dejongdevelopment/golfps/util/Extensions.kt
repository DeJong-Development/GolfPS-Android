package com.dejongdevelopment.golfps.util

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dejongdevelopment.golfps.GolfApplication
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

/** Set visibility to VISIBLE */
fun View.show() {
    this.visibility = View.VISIBLE
}

/** Set visibility to INVISIBLE */
fun View.hide() {
    this.visibility = View.INVISIBLE
}

/** Set visibility to GONE */
fun View.remove() {
    this.visibility = View.GONE
}

fun Int.asColor(context: Context) = context.resources.getColor(this, null)
fun Int.asColorState(context: Context) =
    ColorStateList.valueOf(ContextCompat.getColor(context, this))

fun Int.asDrawable(context: Context) = ContextCompat.getDrawable(context, this)
fun Int.asColorDrawable(context: Context) = ColorDrawable(ContextCompat.getColor(context, this))

// -------------------- NUMBERS -------------------- //

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Float.dp: Float
    get() = (this / Resources.getSystem().displayMetrics.density)
val Float.px: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

/** String distance in yards or meters depending on user preference */
val Int.distance: String
    get() {
        return if (GolfApplication.metric) {
            "$this m"
        } else {
            "$this yds"
        }
    }

/** 00HR 00MIN 00SEC */
val Int.verboseDuration: String
    get() {
        if (this == 0) {
            return ""
        }

        val hours = this % 86400 / 3600
        val minutes = this % 3600 / 60
        val seconds = this % 60

        val roundedSeconds: Int = ((seconds / 5).toDouble().roundToInt() * 5)

        val hourString = "${hours}HR"
        val minuteString = "${minutes}MIN"
        val secondString = "${roundedSeconds}SEC"
        if (hours == 0) {
            if (minutes == 0) {
                return secondString
            }
            if (roundedSeconds == 0) {
                return minuteString
            }
            return "$minuteString $secondString"
        } else {
            if (minutes == 0) {
                return "$hourString $secondString"
            }
            if (roundedSeconds == 0) {
                return "$hourString $minuteString"
            }
            return "$hourString $minuteString $secondString"
        }
    }

/** MM:SS */
val Int.minuteSecondDuration: String
    get() {
        if (this == 0) {
            return "0:00"
        }

        val hours = this % 86400 / 3600
        val minutes = this % 3600 / 60
        val seconds = this % 60

        val minuteString = "${minutes + hours * 60}"
        var secondString = "$seconds"
        if (seconds < 10) {
            secondString = "0$secondString"
        }
        return "$minuteString:$secondString"
    }

/** MM:SS */
val Double.minuteSecondDuration: String
    get() {
        return this.toInt().minuteSecondDuration
    }

// ------------- STRINGS ------------ //
fun String.isValidEmail(): Boolean =
    this.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

val String.alphanumeric: String
    get() {
        val re = Regex("[^A-Za-z0-9]")
        return re.replace(this, "")
    }

//https://www.objc.io/blog/2020/08/18/fuzzy-search/
fun String.fuzzyMatch(query: String): Boolean {
    if (query.isEmpty()) { return true }
    val remainder = query.toCharArray()
    for (char in this) {
        if (char == remainder[remainder.lastIndex]) {
            remainder.drop(1)
            if (remainder.isEmpty()) { return true }
        }
    }
    return false
}

// ----------- Date --------------------- //
/** MMM yyyy */
val Date.monthYear: String
    get() = SimpleDateFormat("MMM yyyy", Locale.US).format(this)

// ---------- Context -----------//
fun Context.makeLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// ---------- Geo ----------------//
val GeoPoint.latLng: LatLng
    get() = LatLng(this.latitude, this.longitude);
val GeoPoint.location: Location
    get() {
        val targetLocation = Location("")
        targetLocation.latitude = this.latitude
        targetLocation.longitude = this.longitude
        return targetLocation
    }
val LatLng.geopoint: GeoPoint
    get() = GeoPoint(this.latitude, this.longitude)
val Location.latLng: LatLng
    get() = LatLng(this.latitude, this.longitude)
val Location.geopoint: GeoPoint
    get() = GeoPoint(this.latitude, this.longitude)