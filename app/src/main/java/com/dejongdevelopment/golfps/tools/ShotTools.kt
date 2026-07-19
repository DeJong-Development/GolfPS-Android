package com.dejongdevelopment.golfps.tools

import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.util.toYards
import com.google.firebase.firestore.GeoPoint
import org.json.JSONObject
import java.net.URL
import kotlin.math.tan

object ShotTools {
    fun getElevationChange(
        start: GeoPoint,
        finish: GeoPoint,
        completion: (startElevation: Double, finishElevation: Double, distance: Double, elevation: Double, error: String?) -> Unit
    ) {
        getElevation(atLocation = start) { startElevation ->
            if (startElevation < -1000) {
                completion(0.0, 0.0, 0.0, 0.0, "Invalid starting elevation")
                return@getElevation
            }

            getElevation(atLocation = finish) { finishElevation ->
                if (finishElevation < -1000) {
                    completion(0.0, 0.0, 0.0, 0.0, "Invalid finishing elevation")
                    return@getElevation
                }

                completeElevationChange(startElevation, finishElevation, completion)
            }
        }
    }

    fun getElevationChange(
        start: GeoPoint,
        finishElevation: Double,
        completion: (startElevation: Double, finishElevation: Double, distance: Double, elevation: Double, error: String?) -> Unit
    ) {
        DebugLogger.log("Using Elevation API 2")
        getElevation(atLocation = start) { startElevation ->
            if (startElevation < -1000) {
                completion(startElevation, finishElevation, 0.0, 0.0, "Invalid elevation change.")
                return@getElevation
            }

            completeElevationChange(startElevation, finishElevation, completion)
        }
    }

    private fun completeElevationChange(
        startElevation: Double,
        finishElevation: Double,
        completion: (startElevation: Double, finishElevation: Double, distance: Double, elevation: Double, error: String?) -> Unit
    ) {
        val elevationChange = finishElevation - startElevation
        val distanceMeters = elevationChange / tan(45.0)
        val distance = if (GolfApplication.metric) distanceMeters else distanceMeters.toYards()
        completion(startElevation, finishElevation, distance, elevationChange, null)
    }

    private fun getElevation(atLocation: GeoPoint, completion: (Double) -> Unit) {
        Thread {
            val elevation = try {
                val url = URL("https://api.open-elevation.com/api/v1/lookup?locations=${atLocation.latitude},${atLocation.longitude}")
                val response = url.readText()
                val results = JSONObject(response).optJSONArray("results")
                results?.optJSONObject(0)?.optDouble("elevation", -100000.0) ?: -100000.0
            } catch (exception: Exception) {
                DebugLogger.report(exception, "Unable to get elevation")
                -100000.0
            }
            completion(elevation)
        }.start()
    }
}
