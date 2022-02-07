package com.dejongdevelopment.golfps.models

import com.dejongdevelopment.golfps.GolfApplication

class Bag {
    private val defaultNumberOfClubs:Int = 13
    private var numberOfClubs:Int
        get() {
            val d = GolfApplication.preferences?.getInt("numberofclubs", defaultNumberOfClubs) ?: defaultNumberOfClubs
            if (d in 1..23) {
                return d
            }
            return defaultNumberOfClubs
        }
        set(newNumber) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putInt("numberofclubs", newNumber)
                editor.apply()
            }
        }

    var myClubs:MutableList<Club> = mutableListOf()
        private set

    init {
        for (i in 1 until this.numberOfClubs + 1) {
            myClubs.add(Club(i))
        }

        sortClubs()
    }

    fun getClubSuggestion(distanceTo: Int): Club {
        val avgDistances:MutableList<Int> = mutableListOf()

        for (c in this.myClubs) {
            avgDistances.add(c.distance)
        }

        var clubNum = 0
        //iterate until we hit the appropriate club
        //do not select a club num past our number of clubs in the bag
        while (clubNum < avgDistances.size - 1 && distanceTo < avgDistances[clubNum]) {
            clubNum += 1
        }

        return this.myClubs[clubNum]
    }

    fun removeClubFromBag(number:Int) {
        val foundIndex = myClubs.indexOfFirst { it.number == number }
        if (foundIndex == -1) {
            return
        }

        myClubs.removeAt(foundIndex)
        this.numberOfClubs = myClubs.size
    }

    fun removeClubFromBag(club:Club) {
        val foundIndex = myClubs.indexOfFirst { it.number == club.number }
        if (foundIndex == -1) {
            return
        }

        myClubs.removeAt(foundIndex)
        this.numberOfClubs = myClubs.size
    }

    fun addClub(club:Club) {
        myClubs.add(club)
        this.numberOfClubs = myClubs.size
    }
    fun moveClub(club:Club, source: Int, destination: Int) {
        this.myClubs.removeAt(source)
        this.myClubs.add(destination, club)
    }

    private fun sortClubs() {
        myClubs.sortByDescending { it.distance }
    }
}