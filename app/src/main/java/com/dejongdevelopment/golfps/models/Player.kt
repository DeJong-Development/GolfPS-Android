package com.dejongdevelopment.golfps.models

import com.dejongdevelopment.golfps.GolfApplication
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.net.URL
import java.util.*

open class Player(id: String) {
    var name:String = "Incognito"
        private set
    var id:String = UUID.randomUUID().toString()
        private set

    var geoPoint: GeoPoint? = null
    var lastLocationUpdate: Date? = null
        private set
    var avatarURL: URL? = null
        private set

    val docReference: DocumentReference?
        get() {
            if (id == "") return null
            return Firebase.firestore.collection("players").document(this.id)
        }

    constructor(id: String, data:MutableMap<String,Any>) : this(id) {
        this.geoPoint = data["location"] as? GeoPoint
        this.lastLocationUpdate = (data["updateTime"] as? Timestamp)?.toDate()

        val avatarPath = data["image"] as? String
        avatarPath?.let {
            this.avatarURL = URL(it)
        }
    }
}

class Me(id:String) : Player(id) {
    var numStrokes:Int = 0

//    var badges:[Badge] = [Badge]()
    var bag:Bag = Bag()
        private set

    val numUniqueCourses:Int
        get() = coursesVisited?.size ?: 0

    val coursesVisited:Set<String>?
        get() = GolfApplication.preferences?.getStringSet("player_courses_visited", null)

    fun addCourseVisitation(courseId:String) {
        val cv = coursesVisited ?: return

        val newCoursesVisited:MutableSet<String> = mutableSetOf()
        newCoursesVisited.addAll(cv)
        newCoursesVisited.add(courseId)
        GolfApplication.preferences?.edit()?.let { editor ->
            editor.putStringSet("player_courses_visited", newCoursesVisited)
            editor.apply()
        }
    }
    var didLogLongDrive:Boolean
        get() = GolfApplication.preferences?.getBoolean("player_logged_long_drive", false) ?: false
        set(didLongDrive) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putBoolean("player_logged_long_drive", didLongDrive)
                editor.apply()
            }
        }

    var didCustomizeBag:Boolean
        get() = GolfApplication.preferences?.getBoolean("player_customize_bag", false) ?: false
        set(didCustomizeBag) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putBoolean("player_customize_bag", didCustomizeBag)
                editor.apply()
            }
        }

    var shareLocation:Boolean
        get() = GolfApplication.preferences?.getBoolean("player_share_location", false) ?: false
        set(newSharePreference) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putBoolean("player_share_location", newSharePreference)
                editor.apply()
            }
        }

    var shareBitmoji:Boolean
        get() = GolfApplication.preferences?.getBoolean("player_share_bitmoji", false) ?: false
        set(newSharePreference) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putBoolean("player_share_bitmoji", newSharePreference)
                editor.apply()
            }
        }
}