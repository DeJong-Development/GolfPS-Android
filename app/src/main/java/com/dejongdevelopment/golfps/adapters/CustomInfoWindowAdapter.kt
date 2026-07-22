package com.dejongdevelopment.golfps.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.dejongdevelopment.golfps.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class CustomInfoWindowAdapter(context: Context) : GoogleMap.InfoWindowAdapter {
    private val window = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)

    override fun getInfoWindow(marker: Marker): View {
        window.findViewById<TextView>(R.id.title).apply {
            text = marker.title.orEmpty()
            visibility = if (marker.title.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        window.findViewById<TextView>(R.id.snippet).apply {
            text = marker.snippet.orEmpty()
            visibility = if (marker.snippet.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        return window
    }

    override fun getInfoContents(marker: Marker): View? = null
}
