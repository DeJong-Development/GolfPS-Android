package com.dejongdevelopment.golfps.models

import com.dejongdevelopment.golfps.GolfApplication
import java.util.UUID

class Club {
    val id: String

    constructor(id: String) {
        this.id = id
    }

    constructor(name: String, distance: Int) {
        this.id = UUID.randomUUID().toString()
        this.name = name
        this.distance = distance
    }

    private val defaultName:String
        get() {
            return when (order) {
                1 -> "Driver"
                2 -> "5 Wood"
                3 -> "3 Wood"
                4 -> "3 Iron"
                5 -> "4 Iron"
                6 -> "5 Iron"
                7 -> "6 Iron"
                8 -> "7 Iron"
                9 -> "8 Iron"
                10 -> "9 Iron"
                11 -> "Pitching Wedge"
                12 -> "Gap Wedge"
                13 -> "Sand Wedge"
                14 -> "Putter"
                else -> "22"
            }
        }

    private val defaultYards:Int
        get() {
            return when (order) {
                1 -> 250
                2 -> 230
                3 -> 220
                4 -> 205
                5 -> 192
                6 -> 184
                7 -> 173
                8 -> 164
                9 -> 156
                10 -> 140
                11 -> 130
                12 -> 110
                13 -> 80
                14 -> 5
                else -> -1
            }
        }

    private val defaultDistance:Int
        get() = if (GolfApplication.metric) (defaultYards.toDouble() * 0.9144).toInt() else defaultYards

    var order:Int
        get() = GolfApplication.preferences?.getInt("cluborder$id", 0) ?: 0
        set(newOrder) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putInt("cluborder$id", newOrder)
                editor.apply()
            }
        }

    var name:String
        get() = GolfApplication.preferences?.getString("clubname$id", defaultName) ?: defaultName
        set(newName) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putString("clubname$id", newName)
                editor.apply()
            }
        }

    var distance:Int
        get() {
            val distance = GolfApplication.preferences?.getInt("clubdistance$id", defaultDistance) ?: defaultDistance
            if (distance > 0) {
                return distance
            }
            return defaultDistance
        }
        set(newDistance) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putInt("clubdistance$id", newDistance)
                editor.apply()
            }
        }

    val isActive:Boolean
        get() = !(GolfApplication.preferences?.getBoolean("clubnotactive$id", false) ?: false)

    fun activateClub() {
        GolfApplication.preferences?.edit()?.let { editor ->
            editor.putBoolean("clubnotactive$id", false)
            editor.apply()
        }
    }

    fun deactivateClub() {
        GolfApplication.preferences?.edit()?.let { editor ->
            editor.putBoolean("clubnotactive$id", true)
            editor.apply()
        }
    }
}
