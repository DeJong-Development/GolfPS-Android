package com.dejongdevelopment.golfps.models

import com.dejongdevelopment.golfps.GolfApplication

enum class BagType(val rawValue: String, val displayName: String) {
    PRO("pro", "Pro Golfer"),
    LONG("long", "Long Hitter"),
    AVERAGE("average", "Average Hitter");
}

class Bag {
    private val defaultNumberOfClubs:Int = 14

    private var numberOfClubs:Int
        get() {
            val storedNumber = GolfApplication.preferences?.getInt("numberofclubs", 0) ?: 0
            if (storedNumber in 1..23) {
                return storedNumber
            }
            return defaultNumberOfClubs
        }
        set(newNumber) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putInt("numberofclubs", newNumber)
                editor.apply()
            }
        }

    private var clubIds:List<String>
        get() {
            val storedIds = GolfApplication.preferences?.getString("assignedclubids", "") ?: ""
            if (storedIds.isBlank()) {
                return listOf()
            }
            return storedIds.split("|").filter { it.isNotBlank() }
        }
        set(newIds) {
            GolfApplication.preferences?.edit()?.let { editor ->
                editor.putString("assignedclubids", newIds.joinToString("|"))
                editor.apply()
            }
        }

    var myClubs:MutableList<Club> = mutableListOf()
        private set

    init {
        for (clubId in clubIds) {
            val club = Club(clubId)
            if (club.isActive) {
                myClubs.add(club)
            }
        }

        sortClubs()
    }

    fun populateBag(type: BagType) {
        when (type) {
            BagType.PRO -> populateBagLong()
            BagType.LONG -> populateBagAverage()
            BagType.AVERAGE -> populateBagShort()
        }
    }

    private fun populateBagLong() {
        val clubs = listOf(
            Club("Driver", 300),
            Club("3 Wood", 270),
            Club("3 Hybrid", 245),
            Club("4 Iron", 226),
            Club("5 Iron", 212),
            Club("6 Iron", 198),
            Club("7 Iron", 184),
            Club("8 Iron", 170),
            Club("9 Iron", 156),
            Club("Pitching Wedge", 144),
            Club("Gap Wedge", 131),
            Club("Sand Wedge", 118),
            Club("Lob Wedge", 105)
        )
        activeClubs(clubs)
    }

    private fun populateBagAverage() {
        val clubs = listOf(
            Club("Driver", 260),
            Club("3 Wood", 235),
            Club("5 Wood", 215),
            Club("3 Hybrid", 200),
            Club("4 Iron", 190),
            Club("5 Iron", 185),
            Club("6 Iron", 177),
            Club("7 Iron", 168),
            Club("8 Iron", 158),
            Club("9 Iron", 145),
            Club("Pitching Wedge", 125),
            Club("Sand Wedge", 105),
            Club("Lob Wedge", 90)
        )
        activeClubs(clubs)
    }

    private fun populateBagShort() {
        val clubs = listOf(
            Club("Driver", 217),
            Club("3 Wood", 205),
            Club("5 Wood", 195),
            Club("3 Hybrid", 185),
            Club("4 Iron", 170),
            Club("5 Iron", 160),
            Club("6 Iron", 150),
            Club("7 Iron", 140),
            Club("8 Iron", 130),
            Club("9 Iron", 115),
            Club("Pitching Wedge", 105),
            Club("Sand Wedge", 80),
            Club("Lob Wedge", 70)
        )
        activeClubs(clubs)
    }

    private fun activeClubs(clubs: List<Club>) {
        clubIds = clubs.map { it.id }

        clubs.forEach { it.activateClub() }

        myClubs.clear()
        myClubs.addAll(clubs)
        numberOfClubs = myClubs.count()
        sortClubs()
    }

    fun getClubSuggestion(distanceTo: Int): Club? {
        val avgDistances = myClubs.map { it.distance }

        var clubNum = 0
        while (clubNum < avgDistances.size - 1 && distanceTo < avgDistances[clubNum]) {
            clubNum += 1
        }

        if (clubNum >= myClubs.count()) {
            return null
        }
        return myClubs[clubNum]
    }

    fun removeClubFromBag(index: Int) {
        if (index !in myClubs.indices) {
            return
        }

        val clubToDeactivate = myClubs.removeAt(index)
        clubToDeactivate.deactivateClub()
        clubIds = myClubs.map { it.id }
        numberOfClubs = myClubs.count()
    }

    fun removeClubFromBag(club: Club) {
        val foundIndex = myClubs.indexOfFirst { it.id == club.id }
        if (foundIndex == -1) {
            return
        }

        removeClubFromBag(foundIndex)
    }

    fun addClub(club: Club) {
        val existingIds = clubIds.toMutableList()
        existingIds.add(club.id)
        clubIds = existingIds

        club.activateClub()
        myClubs.add(club)
        numberOfClubs = myClubs.count()
        sortClubs()
    }

    fun moveClub(club: Club, source: Int, destination: Int) {
        if (source !in myClubs.indices || destination !in 0..myClubs.count()) {
            return
        }

        myClubs.removeAt(source)
        myClubs.add(destination.coerceIn(0, myClubs.count()), club)
        clubIds = myClubs.map { it.id }
    }

    fun sortClubs() {
        myClubs.sortByDescending { it.distance }

        for (i in 0 until myClubs.count()) {
            myClubs[i].order = i
        }
        clubIds = myClubs.map { it.id }
    }
}
