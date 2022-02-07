package com.dejongdevelopment.golfps.models

import com.dejongdevelopment.golfps.GolfApplication

class Club(number: Int) {

    var number:Int = 1
        private set

    init {
        this.number = number
    }

    private val defaultName:String
        get() {
            when (number) {
                1 -> return "Driver"
                2 -> return "5 Wood"
                3 -> return "3 Wood"
                4 -> return "3 Iron"
                5 -> return "4 Iron"
                6 -> return "5 Iron"
                7 -> return "6 Iron"
                8 -> return "7 Iron"
                9 -> return "8 Iron"
                10 -> return "9 Iron"
                11 -> return "Pitching Wedge"
                12 -> return "Gap Wedge"
                13 -> return "Sand Wedge"
                else -> return "22"
            }
        }
    private val defaultYards:Int
        get() {
            when (number) {
                1 -> return 250
                2 -> return 230
                3 -> return 220
                4 -> return 205
                5 -> return 192
                6 -> return 184
                7 -> return 173
                8 -> return 164
                9 -> return 156
                10 -> return 140
                11 -> return 130
                12 -> return 110
                13 -> return 80
                else -> return -1
            }
        }
    private val defaultDistance:Int
        get() = if (GolfApplication.metric) (defaultYards.toDouble() * 0.9144).toInt() else defaultYards

    var name:String
        get() {
            return GolfApplication.preferences?.getString("clubname$number", defaultName) ?: defaultName
        }
        set(newName) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putString("clubname$number", newName)
                editor.apply()
            }
        }

    var distance:Int
        get() {
            val d = GolfApplication.preferences?.getInt("clubdistance$number", defaultDistance) ?: defaultDistance
            if (d > 0) { return d }
            return defaultDistance
        }
        set(newDistance) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putInt("clubdistance$number", newDistance)
                editor.apply()
            }
        }
}