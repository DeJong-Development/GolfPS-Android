package com.dejongdevelopment.golfps.util

import android.location.Location
import android.util.Log
import android.util.Size
import android.view.View
import com.dejongdevelopment.golfps.GolfApplication
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.GeoPoint
import java.lang.Math.floorMod
import kotlin.math.*

object MapTools {
    fun getBoundsCenter(bounds: LatLngBounds): LatLng {
        val ne = bounds.northeast
        val sw = bounds.southwest

        val latCenter = (ne.latitude + sw.latitude) / 2
        val longCenter = (ne.longitude + sw.longitude) / 2

        return LatLng(latCenter, longCenter)
    }
    fun getBoundsZoomLevel(bounds: LatLngBounds, screenSize: Size): Float {
        val zoomMax:Float = 20f

        fun latRad(lat:Double): Double {
            val sinRad = sin(lat * Math.PI / 180)
            val radX2 = log((1 + sinRad) / (1 - sinRad), 10.0) / 2
            return max(min(radX2, Math.PI), -Math.PI) / 2
        }

        fun zoom(mapPx:Double, worldPx:Double, fraction:Double): Float {
            return (log(mapPx / worldPx / fraction, 10.0) / log(2.0, 10.0)).toFloat()
        }

        val ne = bounds.northeast
        val sw = bounds.southwest

        val latFraction = (latRad(ne.latitude) - latRad(sw.latitude)) / Math.PI

        val lngDiff = ne.longitude - sw.longitude
        val lngFraction = (if (lngDiff < 0) (lngDiff + 360) else lngDiff) / 360

        val latZoom = zoom((screenSize.height).toDouble(), 256.0, latFraction)
        val lngZoom = zoom((screenSize.width).toDouble(), 256.0, lngFraction)

        return listOf(latZoom, lngZoom, zoomMax).minOrNull()!!
    }

    fun getBoundsZoomLevel(bounds: LatLngBounds, view: View): Float {
        val ne = bounds.northeast
        val sw = bounds.southwest

        fun latRad(lat:Double): Double {
            val sinRad = sin(lat * Math.PI / 180)
            val radX2 = log((1 + sinRad) / (1 - sinRad), 10.0) / 2
            return max(min(radX2, Math.PI), -Math.PI) / 2
        }
        fun zoom(mapPx:Double, worldPx:Double, fraction:Double): Float {
            return (log(mapPx / worldPx / fraction, 10.0) / log(2.0, 10.0)).toFloat()
        }

        val latFraction: Double = (latRad(ne.latitude) - latRad(sw.latitude)) / Math.PI
        val lngDiff = ne.longitude - sw.longitude
        val lngFraction = (if (lngDiff < 0) lngDiff + 360 else lngDiff) / 360

        val viewHeight = view.measuredHeight.toDouble()
        val scaleY = view.scaleY.toDouble()
        val viewWidth = view.measuredWidth.toDouble()
        val scaleX = view.scaleX.toDouble()

        val latZoom: Float = zoom(viewHeight / scaleY / 2, 256.0, latFraction)
        val lngZoom: Float = zoom(viewWidth / scaleX / 2, 256.0, lngFraction)

        //can this be adjusted to account for bearing as well?
        //don't zoom in too far until we account for bearing
        return min(min(latZoom, lngZoom), 19f)
    }

    fun getCircularZoom(holeLength: Double, holeWidth: Double, view: View): Float {
        fun zoom(mapPx:Double, worldPx:Double, fraction:Double): Float {
            return (log(mapPx / worldPx / fraction, 10.0) / log(2.0, 10.0)).toFloat()
        }

        val viewHeight = view.measuredHeight.toDouble()
        val scaleY = view.scaleY.toDouble()
        val viewWidth = view.measuredWidth.toDouble()
        val scaleX = view.scaleX.toDouble()

        val verticalZoom: Float = zoom(viewHeight / scaleY / 3, 256.0, holeLength / 30000000)
        val horizontalZoom: Float = zoom(viewWidth / scaleX / 3, 256.0, holeWidth / 30000000)

        return min(min(verticalZoom, horizontalZoom), 19f)
    }

    fun coordinates(startingCoordinates: LatLng, atDistance: Double, atAngle: Double): LatLng {
        val earthsRadiusInYards:Double = 6371 * 1093.6133
        val earthsRadiusInMeters:Double = 6371 * 1000.0

        val angularDistance:Double = if (GolfApplication.metric) {
            atDistance / earthsRadiusInMeters
        } else {
            atDistance / earthsRadiusInYards
        }

        val bearingRadians = this.degreesToRadians(atAngle)
        val fromLatRadians = this.degreesToRadians(startingCoordinates.latitude)
        val fromLonRadians = this.degreesToRadians(startingCoordinates.longitude)

        val toLatRadians:Double = asin(sin(fromLatRadians) * cos(angularDistance) + cos(fromLatRadians) * sin(angularDistance) * cos(bearingRadians))
        var toLonRadians:Double = fromLonRadians + atan2(sin(bearingRadians) * sin(angularDistance) * cos(fromLatRadians), cos(angularDistance) - sin(fromLatRadians) * sin(toLatRadians))

        val a = toLonRadians + 3.0 * Math.PI
        val b = 2.0 * Math.PI

        //need to have very good precision so we can track coordinate on surface of earth
        val c:Double = floorMod((a * 100000000).toInt(), (b * 100000000).toInt()).toDouble() / 100000000.0

        toLonRadians = c - Math.PI

        val lat = this.radiansToDegrees(toLatRadians)
        val lon = this.radiansToDegrees(toLonRadians)

        return LatLng(lat, lon)
    }

    fun calculateBearing(start: GeoPoint, finish: GeoPoint): Float {

        val latRad1:Double = degreesToRadians(start.latitude)
        val latRad2:Double = degreesToRadians(finish.latitude)
        val longDiff:Double = degreesToRadians(finish.longitude - start.longitude)
        val y:Double = sin(longDiff) * cos(latRad2)
        val x:Double = cos(latRad1) * sin(latRad2) - sin(latRad1) * cos(latRad2) * cos(longDiff)

//        let x = 8.625
//        print(x / 0.75)
//        // Prints "11.5"
//
//        let q = (x / 0.75).rounded(.towardZero)
//        // q == 11.0
//        let r = x.truncatingRemainder(dividingBy: 0.75)
//        // r == 0.375
//
//        let x1 = 0.75 * q + r
//        r = x - (0.75 * q)
//        // x1 == 8.625

//        val calcBearing:Double = (radiansToDegrees(atan2(y, x)) + 360).truncatingRemainder(dividingBy: 360)
        val bearingDegrees:Double = radiansToDegrees(atan2(y, x)) + 360
        val totalBearing:Int = (bearingDegrees / 360).roundToInt()
        val calcBearing = bearingDegrees - (totalBearing * 360).toFloat()
        val realBearing = start.location.bearingTo(finish.location)

        Log.d("BEARING", "manual: $calcBearing; provided: $realBearing")
        return realBearing
    }
    fun calculateBearing(start: GeoPoint?, finish: GeoPoint?): Float? {
        if (start == null) return null
        if (finish == null) return null
        return calculateBearing(start, finish)
    }

    private fun degreesToRadians(degrees:Double): Double {
        return degrees * Math.PI / 180.0
    }
    private fun radiansToDegrees(radians:Double): Double {
        return radians * 180 / Math.PI
    }

    fun distanceFrom(first:GeoPoint, second:GeoPoint): Int {
        val earthRadiusKm = 6371.0

        var lat1:Double = first.latitude
        val lon1:Double = first.longitude
        var lat2:Double = second.latitude
        val lon2:Double = second.longitude

        val dLat:Double = degreesToRadians(lat2 - lat1)
        val dLon:Double = degreesToRadians(lon2 - lon1)

        lat1 = degreesToRadians(lat1)
        lat2 = degreesToRadians(lat2)

        val a:Double = sin(dLat / 2) * sin(dLat / 2) + sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
        val c:Double = 2 * atan2(sqrt(a), sqrt(1-a))

        return if (GolfApplication.metric) {
            (earthRadiusKm * c * 1000).toInt() //convert km to meters
        } else {
            (earthRadiusKm * c * 1093.61).toInt() //convert km to yards
        }
    }
    fun distanceFrom(first: LatLng?, second: LatLng?): Int? {
        if (first == null) return null
        if (second == null) return null
        return distanceFrom(first.geopoint, second.geopoint)
    }
    fun distanceFrom(first: GeoPoint?, second: GeoPoint?): Int? {
        if (first == null) return null
        if (second == null) return null
        return distanceFrom(first, second)
    }
}