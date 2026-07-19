package com.dejongdevelopment.golfps.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.models.Hole
import com.dejongdevelopment.golfps.util.latLng
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions

object MarkerTools {
    fun mapIcon(context: Context, resource: Int, size: Int): BitmapDescriptor {
        val iconBitmap = BitmapFactory.decodeResource(context.resources, resource)
        val scaledBitmap = Bitmap.createScaledBitmap(iconBitmap, size, size, false)
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
    }

    fun pinMarkerOptions(context: Context, hole: Hole): MarkerOptions {
        return MarkerOptions()
            .position(hole.pinLocation.latLng)
            .title("Pin #${hole.number}")
            .icon(mapIcon(context, R.drawable.flag_marker, 55))
    }

    fun teeMarkerOptions(context: Context, hole: Hole): MarkerOptions? {
        val teeLocation = hole.teeLocations.firstOrNull() ?: return null
        return MarkerOptions()
            .position(teeLocation.latLng)
            .title("Tee #${hole.number}")
            .icon(mapIcon(context, R.drawable.tee_marker, 55))
    }

    fun bunkerMarkerOptions(context: Context, hole: Hole): List<MarkerOptions> {
        return hole.bunkerLocations.mapIndexed { index, bunkerLocation ->
            MarkerOptions()
                .position(bunkerLocation.latLng)
                .title("Hazard")
                .icon(mapIcon(context, R.drawable.hazard_marker, 35))
                .zIndex(index.toFloat())
        }
    }
}
