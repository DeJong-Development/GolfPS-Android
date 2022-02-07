package com.dejongdevelopment.golfps.models

import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.util.MapTools
import com.dejongdevelopment.golfps.util.latLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint

class Hole(number: Int, data: MutableMap<String, Any>) {

    var number:Int = 1
        private set

    val docReference: DocumentReference?
        get() = GolfApplication.course?.docReference?.collection("holes")?.document("${this.number}")

    var pinLocation:GeoPoint
        private set
    var teeLocations:List<GeoPoint>
        private set
    var bunkerLocations:List<GeoPoint>
        private set
    var dogLegLocation:GeoPoint?
        private set

    var pinElevation:Double?
    var isLongDrive:Boolean

    var myLongestDriveInYards:Int? = null
    var myLongestDriveInMeters:Int? = null

    var longestDrives:MutableMap<String,GeoPoint> = mutableMapOf()

    val distanceToPinFromTee:Int?
        get() = MapTools.distanceFrom(teeLocations.firstOrNull(), pinLocation)

    val bearingToDogLeg:Float?
        get() = MapTools.calculateBearing(teeLocations.firstOrNull(), dogLegLocation)
    val bearingToPinFromTee:Float?
        get() = MapTools.calculateBearing(teeLocations.firstOrNull(), pinLocation)

    init {
        this.number = number
        bunkerLocations = listOf()
        teeLocations = listOf()
        dogLegLocation = null
        isLongDrive = false

        pinLocation = data["pin"] as GeoPoint
        teeLocations = data["tee"] as? List<GeoPoint> ?: listOf()
        bunkerLocations = data["bunkers"] as? List<GeoPoint> ?: listOf()

        dogLegLocation = data["dogLeg"] as? GeoPoint
        pinElevation = data["pinElevation"] as? Double

        isLongDrive = data["longDrive"] as? Boolean ?: false

        if (isLongDrive) {
//            CourseTools.getLongestDrives(for: self) { [weak self] (success, error) in
//                    if (success) {
//                        self?.updateDelegate?.didUpdateLongDrive()
//                    }
//            }
        }
    }

    val bounds:LatLngBounds
        get() {
            val builder = LatLngBounds.Builder()
            builder.include(this.pinLocation.latLng)
            for (tee in this.teeLocations) {
                builder.include(tee.latLng)
            }
            for (bunker in this.bunkerLocations) {
                builder.include(bunker.latLng)
            }
            this.dogLegLocation?.apply {
                builder.include(this.latLng)
            }
            return builder.build()
        }

    fun setLongestDrive(distance:Int?) {
        if (distance == null) {
            myLongestDriveInMeters = null
            myLongestDriveInYards = null
            return
        }

        if (GolfApplication.metric) {
            myLongestDriveInMeters = distance
            myLongestDriveInYards = (distance.toDouble() * 1.09361).toInt()
        } else {
            myLongestDriveInMeters = (distance.toDouble() / 1.09361).toInt()
            myLongestDriveInYards = distance
        }
    }
}