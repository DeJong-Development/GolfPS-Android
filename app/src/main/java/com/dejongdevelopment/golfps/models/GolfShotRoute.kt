package com.dejongdevelopment.golfps.models

import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.tools.DebugLogger
import com.dejongdevelopment.golfps.tools.MapTools
import com.dejongdevelopment.golfps.util.gaussianRandom
import com.dejongdevelopment.golfps.util.geopoint
import com.dejongdevelopment.golfps.util.latLng
import com.google.firebase.firestore.GeoPoint

class GolfShotRoute(
    private val club1: Club,
    private val club2: Club,
    hole: Hole
) {
    private val teeGeopoint: GeoPoint = hole.teeLocations.first()
    private val dogLegGeopoint: GeoPoint? = hole.dogLegLocation
    private val bunkerGeopoints: List<GeoPoint> = hole.bunkerLocations
    private val pinGeopoint: GeoPoint = hole.pinLocation

    var teeTarget: GeoPoint? = null
        private set

    var secondShotTarget: GeoPoint? = null
        private set

    var optimalNumberOfShots: Int = 0
        private set

    var totalNumberOfIterations: Double = 0.0
        private set

    var totalNumberOfShots: Double = 0.0
        private set

    var averageNumberOfShots: Double = 0.0
        private set

    private var didPuttOut: Boolean = false
    private var numberOfHits: Int = 0

    fun isTeeShotTargettingFairway(): Boolean = true

    fun isSecondShotTargettingFairway(): Boolean = true

    fun applyInitialBearingDeviations(teeShotDeviation: Double, secondShotDeviation: Double) {
        val teeShotDistance = club1.distance.toDouble()
        val teeShotTarget = dogLegGeopoint ?: pinGeopoint
        val teeShotBearing = MapTools.calculateBearing(teeGeopoint, teeShotTarget)
        teeTarget = MapTools.coordinates(
            startingCoordinates = teeGeopoint.latLng,
            atDistance = teeShotDistance,
            atAngle = teeShotBearing + teeShotDeviation
        ).geopoint

        val firstTarget = teeTarget ?: return
        val secondShotDistance = club2.distance.toDouble()
        val secondShotBearing = MapTools.calculateBearing(firstTarget, pinGeopoint)
        secondShotTarget = MapTools.coordinates(
            startingCoordinates = firstTarget.latLng,
            atDistance = secondShotDistance,
            atAngle = secondShotBearing + secondShotDeviation
        ).geopoint

        playHolePerfectly()
    }

    private fun playHolePerfectly() {
        optimalNumberOfShots = 0
        totalNumberOfShots = 0.0
        averageNumberOfShots = 0.0
        numberOfHits = 0
        didPuttOut = false

        val firstTarget = teeTarget ?: return
        hitShot(start = teeGeopoint, target = firstTarget, shotNum = 1, isPerfect = true)

        optimalNumberOfShots = totalNumberOfShots.toInt()
        totalNumberOfIterations = 1.0
    }

    fun playHole(numIterations: Int) {
        if (numIterations <= 0) {
            DebugLogger.report(null, "Unable to play hole less than 1 time.")
            return
        }

        val firstTarget = teeTarget ?: return
        repeat(numIterations) {
            numberOfHits = 0
            didPuttOut = false
            hitShot(start = teeGeopoint, target = firstTarget, shotNum = 1)
        }

        if (totalNumberOfShots <= 0) {
            DebugLogger.report(null, "There should always be at least 1 shot.")
            return
        }

        totalNumberOfIterations += numIterations.toDouble()
        averageNumberOfShots = totalNumberOfShots / totalNumberOfIterations

        if (averageNumberOfShots <= 0) {
            DebugLogger.report(null, "There should always be at least 1 shot.")
            return
        }

        averageNumberOfShots = (100 * averageNumberOfShots).toInt().toDouble() / 100
    }

    private fun hitShot(start: GeoPoint, target: GeoPoint, shotNum: Int, isPerfect: Boolean = false) {
        numberOfHits += 1

        val distanceToTarget = MapTools.distanceFrom(start, target)
        val distanceToPin = MapTools.distanceFrom(start, pinGeopoint)
        val bearingToTarget = MapTools.calculateBearing(start, target)

        val club = when (shotNum) {
            1 -> club1
            2 -> club2
            else -> GolfApplication.me.bag.getClubSuggestion(distanceToPin)
        } ?: return

        val targetDistance = if (distanceToTarget > 90) club.distance else distanceToTarget

        val shotBearing: Double
        val shotDistance: Double
        if (isPerfect) {
            shotBearing = bearingToTarget.toDouble()
            shotDistance = targetDistance.toDouble()
        } else {
            shotBearing = bearingToTarget + 0.0.gaussianRandom(stdDev = 15.0)
            shotDistance = targetDistance.toDouble().gaussianRandom(stdDev = 0.08 * targetDistance)
        }

        val shotLandingCoordinates = MapTools.coordinates(start.latLng, shotDistance, shotBearing)
        val remainingDistanceToPin = MapTools.distanceFrom(shotLandingCoordinates.geopoint, pinGeopoint)

        if (remainingDistanceToPin < 2) {
            totalNumberOfShots += (shotNum + 1).toDouble()
            didPuttOut = true
            return
        }

        if (distanceToPin < 10) {
            totalNumberOfShots += (shotNum + 2).toDouble()
            didPuttOut = true
            return
        }

        var shotPenalty = 0
        for (bunkerGP in bunkerGeopoints) {
            val distanceToBunker = MapTools.distanceFrom(shotLandingCoordinates.geopoint, bunkerGP)
            if (distanceToBunker < 10) {
                shotPenalty = 3
                break
            }
        }

        val nextTarget = if (shotNum == 1) {
            secondShotTarget ?: pinGeopoint
        } else {
            pinGeopoint
        }

        hitShot(
            start = shotLandingCoordinates.geopoint,
            target = nextTarget,
            shotNum = shotNum + 1 + shotPenalty,
            isPerfect = isPerfect
        )
    }
}
