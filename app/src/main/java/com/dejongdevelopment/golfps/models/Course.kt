package com.dejongdevelopment.golfps.models

import android.os.Parcel
import android.os.Parcelable
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.util.latLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class Course(val id: String): Parcelable {

    constructor(id: String, firestoreData: MutableMap<String, Any>?) : this(id) {
        this.name = (firestoreData?.get("name") as? String)?.trim() ?: "Unknown"
        this.city = (firestoreData?.get("city") as? String)?.trim() ?: ""
        this.state = (firestoreData?.get("state") as? String)?.trim() ?: ""
        this.spectation = firestoreData?.get("spectation") as? GeoPoint
    }

    var name: String = ""
        private set
    var city: String = ""
        private set
    var state: String = ""
        private set
    var spectation: GeoPoint? = null
        private set

    val fullStateName:String?
        get() {
            when (state.uppercase()) {
                "AL" -> return "alabama"
                "AK" -> return "alaska"
                "AZ" -> return "arizona"
                "AR" -> return "arkansas"
                "CA" -> return "california"
                "CO" -> return "colorado"
                "CT" -> return "connecticut"
                "DE" -> return "delaware"
                "FL" -> return "florida"
                "GA" -> return "georgia"
                "HI" -> return "hawaii"
                "ID" -> return "idaho"
                "IL" -> return "illinois"
                "IN" -> return "indiana"
                "IA" -> return "iowa"
                "KS" -> return "kansas"
                "KY" -> return "kentucky"
                "LA" -> return "louisiana"
                "ME" -> return "maine"
                "MD" -> return "maryland"
                "MA" -> return "massachusetts"
                "MI" -> return "michigan"
                "MN" -> return "minnesota"
                "MS" -> return "mississippi"
                "MO" -> return "missouri"
                "MT" -> return "montana"
                "NE" -> return "nebraska"
                "NV" -> return "nevada"
                "NH" -> return "new hampshire"
                "NJ" -> return "new jersy"
                "NM" -> return "new mexico"
                "NY" -> return "new york"
                "NC" -> return "north carolina"
                "ND" -> return "north dakota"
                "OH" -> return "ohio"
                "OK" -> return "oklahoma"
                "OR" -> return "oregon"
                "PA" -> return "pennsylvania"
                "RI" -> return "rhode island"
                "SC" -> return "south carolina"
                "SD" -> return "south dakota"
                "TN" -> return "tennessee"
                "TX" -> return "texas"
                "UT" -> return "utah"
                "VT" -> return "vermont"
                "VA" -> return "virginia"
                "WA" -> return "washington"
                "WV" -> return "west virginia"
                "WI" -> return "wisconsin"
                "WY" -> return "wyoming"
                else -> return null
            }
    }

    val stateIcon: Int?
        get() {
            when (state.uppercase()) {
                "AL" -> return null
                "AK" -> return null
                "AZ" -> return null
                "AR" -> return null
                "CA" -> return R.drawable.california
                "CO" -> return null
                "CT" -> return null
                "DE" -> return null
                "FL" -> return R.drawable.florida
                "GA" -> return null
                "HI" -> return null
                "ID" -> return null
                "IL" -> return R.drawable.illinois
                "IN" -> return null
                "IA" -> return null
                "KS" -> return null
                "KY" -> return R.drawable.kentucky
                "LA" -> return null
                "ME" -> return null
                "MD" -> return null
                "MA" -> return null
                "MI" -> return R.drawable.michigan
                "MN" -> return null
                "MS" -> return null
                "MO" -> return null
                "MT" -> return null
                "NE" -> return null
                "NV" -> return null
                "NH" -> return null
                "NJ" -> return null
                "NM" -> return null
                "NY" -> return null
                "NC" -> return R.drawable.northcarolina
                "ND" -> return null
                "OH" -> return R.drawable.ohio
                "OK" -> return null
                "OR" -> return null
                "PA" -> return null
                "RI" -> return null
                "SC" -> return null
                "SD" -> return null
                "TN" -> return R.drawable.tennessee
                "TX" -> return null
                "UT" -> return R.drawable.utah
                "VT" -> return null
                "VA" -> return null
                "WA" -> return null
                "WV" -> return null
                "WI" -> return null
                "WY" -> return null
                "UK" -> return R.drawable.unitedkingdom
                "ON" -> return R.drawable.ontario
                "QC" -> return R.drawable.quebec
                else -> return null
            }
        }

    var holes:MutableList<Hole> = mutableListOf()
        private set

    var didPlayHere:Boolean
        get() = GolfApplication.preferences?.getBoolean("played_at_$id)", false) ?: false
        set(newSharePreference) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putBoolean("played_at_$id)", newSharePreference)
                editor.apply()
            }
        }

    val docReference: DocumentReference?
        get() {
            if (id == "") return null
            return Firebase.firestore.collection("courses").document(this.id)
        }

    val bounds:LatLngBounds
        get() {
            val builder = LatLngBounds.Builder()
            for (hole in this.holes) {
                builder.include(hole.pinLocation.latLng)
                for (tee in hole.teeLocations) {
                    builder.include(tee.latLng)
                }
                for (bunker in hole.bunkerLocations) {
                    builder.include(bunker.latLng)
                }
            }
            if (spectation != null) {
                builder.include(spectation!!.latLng)
            }
            return builder.build()
        }

    constructor(parcel: Parcel) : this(parcel.readString() ?: UUID.randomUUID().toString()) {
        this.name = parcel.readString() ?: "Unknown"
        this.city = parcel.readString() ?: ""
        this.state = parcel.readString() ?: ""

        val specationLatitude = parcel.readDouble()
        val specationLongitude = parcel.readDouble()
        if (specationLatitude > 0.0 && specationLongitude > 0.0) {
            this.spectation = GeoPoint(specationLatitude, specationLongitude)
        }
    }

    fun addHoles(completion: (Boolean, Exception?) -> Unit) {
        val courseDocRef = this.docReference
        if (courseDocRef == null) {
            completion(false, null)
            return
        }

        this.holes.clear()

        courseDocRef.collection("holes").get()
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    for (document in querySnapshot.documents) {
                        val data = document.data ?: continue
                        val holeNumber = document.id.toIntOrNull() ?: continue

                        val hole = Hole(holeNumber, data)
                        this.holes.add(hole)
                    }
                    completion(true, null)
                    return@addOnCompleteListener
                }

                completion(false, task.exception)
            }
    }

    ///get the long drive data associated with this hole
    fun getLongestDrives(hole: Hole, completion: (Boolean, Exception?) -> Unit) {
        val holeDocRef = hole.docReference
        if (holeDocRef == null) {
            completion(false, null)
            return
        }

        holeDocRef.collection("drives")
            .orderBy("distance", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnCompleteListener { task ->
                hole.longestDrives.clear()

                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    for (driveDoc in querySnapshot.documents) {
                        val driveUser = driveDoc.id
                        val driveData = driveDoc.data ?: continue

                        val driveLocation = driveData["location"] as? GeoPoint
                        driveLocation?.let {
                            hole.longestDrives[driveUser] = it
                        }
                        val driveDistance = driveData["distance"] as? Number
                        driveDistance?.let {
                            if (driveUser == GolfApplication.me.id) {
                                hole.myLongestDriveInYards = it.toInt()
                                hole.myLongestDriveInMeters = (it.toDouble() / 1.09361).toInt()
                            }
                        }
                    }
                    completion(true, null)
                    return@addOnCompleteListener
                }

                completion(false, task.exception)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(this.id)
        parcel.writeString(this.name)
        parcel.writeString(this.city)
        parcel.writeString(this.state)
        parcel.writeDouble(this.spectation?.latitude ?: 0.0)
        parcel.writeDouble(this.spectation?.longitude ?: 0.0)
    }

    override fun describeContents(): Int {
        return 0
    }
    companion object CREATOR : Parcelable.Creator<Course> {
        override fun createFromParcel(parcel: Parcel): Course {
            return Course(parcel)
        }
        override fun newArray(size: Int): Array<Course?> {
            return arrayOfNulls(size)
        }
    }
}