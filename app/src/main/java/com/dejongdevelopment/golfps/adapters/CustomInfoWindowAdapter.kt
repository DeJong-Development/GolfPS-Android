package com.dejongdevelopment.golfps.adapters

import android.graphics.Color
import android.view.View
import com.dejongdevelopment.golfps.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

//class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

//    private val showDefaultInfo = false
//
//    // These are both view groups containing an ImageView with id "badge" and two
//    // TextViews with id "title" and "snippet".
//    private val window: View = layoutInflater.inflate(R.layout.custom_info_window, null)
//    private val contents: View = layoutInflater.inflate(R.layout.custom_info_contents, null)
//
//    override fun getInfoWindow(marker: Marker): View? {
//        if (showDefaultInfo) {
//            // This means that getInfoContents will be called.
//            return null
//        }
//        render(marker, window)
//        return window
//    }
//
//    override fun getInfoContents(marker: Marker): View? {
//        if (showDefaultInfo) {
//            // This means that the default info contents will be used.
//            return null
//        }
//        render(marker, contents)
//        return contents
//    }
//
//    private fun render(marker: Marker, view: View) {
//        val badge = when (marker.title) {
//            "Brisbane" -> R.drawable.badge_qld
//            "Adelaide" -> R.drawable.badge_sa
//            "Sydney" -> R.drawable.badge_nsw
//            "Melbourne" -> R.drawable.badge_victoria
//            "Perth" -> R.drawable.badge_wa
//            in "Darwin Marker 1".."Darwin Marker 4" -> R.drawable.badge_nt
//            else -> 0 // Passing 0 to setImageResource will clear the image view.
//        }
//
//        view.findViewById<ImageView>(R.id.badge).setImageResource(badge)
//
//        // Set the title and snippet for the custom info window
//        val title: String? = marker.title
//        val titleUi = view.findViewById<TextView>(R.id.title)
//
//        if (title != null) {
//            // Spannable string allows us to edit the formatting of the text.
//            titleUi.text = SpannableString(title).apply {
//                setSpan(ForegroundColorSpan(Color.RED), 0, length, 0)
//            }
//        } else {
//            titleUi.text = ""
//        }
//
//        val snippet: String? = marker.snippet
//        val snippetUi = view.findViewById<TextView>(R.id.snippet)
//        if (snippet != null && snippet.length > 12) {
//            snippetUi.text = SpannableString(snippet).apply {
//                setSpan(ForegroundColorSpan(Color.MAGENTA), 0, 10, 0)
//                setSpan(ForegroundColorSpan(Color.BLUE), 12, snippet.length, 0)
//            }
//        } else {
//            snippetUi.text = ""
//        }
//    }
//}