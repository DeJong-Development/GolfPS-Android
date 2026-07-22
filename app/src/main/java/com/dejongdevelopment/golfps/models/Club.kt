package com.dejongdevelopment.golfps.models

import com.dejongdevelopment.golfps.GolfApplication
import java.util.UUID

class Club {
    val id: String
    private var storedOrder: Int
    private var storedName: String?
    private var storedDistance: Int?

    constructor(id: String) {
        this.id = id
        this.storedOrder = GolfApplication.preferences?.getInt("cluborder$id", 0) ?: 0
        this.storedName = GolfApplication.preferences?.getString("clubname$id", null)
        this.storedDistance = GolfApplication.preferences?.getInt("clubdistance$id", -1)
            ?.takeIf { it > 0 }
    }

    constructor(name: String, distance: Int) {
        this.id = UUID.randomUUID().toString()
        this.storedOrder = 0
        this.storedName = name
        this.storedDistance = distance
        persistName(name)
        persistDistance(distance)
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
        get() = storedOrder
        set(newOrder) {
            storedOrder = newOrder
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putInt("cluborder$id", newOrder)
                editor.apply()
            }
        }

    var name:String
        get() = storedName ?: defaultName
        set(newName) {
            storedName = newName
            persistName(newName)
        }

    var distance:Int
        get() {
            return storedDistance?.takeIf { it > 0 } ?: defaultDistance
        }
        set(newDistance) {
            storedDistance = newDistance
            persistDistance(newDistance)
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

    private fun persistName(newName: String) {
        GolfApplication.preferences?.edit()?.let { editor ->
            editor.putString("clubname$id", newName)
            editor.apply()
        }
    }

    private fun persistDistance(newDistance: Int) {
        GolfApplication.preferences?.edit()?.let { editor ->
            editor.putInt("clubdistance$id", newDistance)
            editor.apply()
        }
    }
}
