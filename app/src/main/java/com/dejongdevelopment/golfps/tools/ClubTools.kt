package com.dejongdevelopment.golfps.tools

object ClubTools {
    private val clubNameRegex = Regex("[^a-zA-Z0-9\\$#! ]")
    private val clubDistanceRegex = Regex("[^0-9]")

    fun cleanClubName(name: String?): String {
        return clubNameRegex.replace(name ?: "", "")
    }

    fun cleanClubDistance(distance: String?): String {
        return clubDistanceRegex.replace(distance ?: "", "")
    }
}
