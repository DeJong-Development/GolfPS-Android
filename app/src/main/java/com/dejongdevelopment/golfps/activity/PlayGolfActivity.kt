package com.dejongdevelopment.golfps.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.dejongdevelopment.golfps.BuildConfig
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.databinding.ActivityPlayGolfBinding
import com.dejongdevelopment.golfps.models.Hole
import com.dejongdevelopment.golfps.util.MapTools
import com.dejongdevelopment.golfps.util.latLng
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.GeoPoint
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.models.Club
import com.dejongdevelopment.golfps.models.Course
import com.dejongdevelopment.golfps.util.distance
import com.dejongdevelopment.golfps.util.geopoint
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import kotlin.math.min

class PlayGolfActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityPlayGolfBinding
    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private var mapReady:Boolean = false

    private var vibe: Vibrator? = null

    private var currentHole:Hole? = null

    private var meMarker: Marker? = null
    private var currentPinMarker: Marker? = null
    private var currentTeeMarker: Marker? = null
    private var currentBunkerMarkers:MutableList<Marker> = mutableListOf()
    private var currentDistanceMarker:Marker? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var lineToMyLocation: Polyline? = null
    private var lineToTee: Polyline? = null
    private var lineToPin: Polyline? = null
    private var isDraggingDistanceMarker = false

    private var drivingDistanceLines:MutableList<Polyline> = mutableListOf()
    private var suggestedDistanceLines:MutableList<Polyline> = mutableListOf()
    private val drivingDistanceLineColors:List<Int> = listOf(Color.GREEN, Color.CYAN, Color.YELLOW)

    private val hasLocationPermission:Boolean
        get() {
            val fineLocation = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val courseLocation = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            return fineLocation == PackageManager.PERMISSION_GRANTED ||
                    courseLocation == PackageManager.PERMISSION_GRANTED
        }

    private fun getMapIcon(resource:Int, size:Int = 150): BitmapDescriptor? {
        val iconBitmap = BitmapFactory.decodeResource(resources, resource)
        val scaledBitmap = Bitmap.createScaledBitmap(iconBitmap, size, size, false) ?: return null
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()

        val course: Course = GolfApplication.course ?: return
        binding.courseName.text = course.name

        if (course.holes.isNotEmpty() && this.mapReady) {
            goToHole()
        }

        course.addHoles { success, exception ->
            if (exception != null) {
                Log.d("HOLES", exception.localizedMessage ?: "error getting holes")
                Toast.makeText(this@PlayGolfActivity, "Error retrieving the hole information.", Toast.LENGTH_LONG)
                    .show()
                return@addHoles
            }

            if (success) {
                val updateMap = hashMapOf(
                    "course" to course.id,
                    "updateTime" to Timestamp.now()
                )
                GolfApplication.me.docReference?.set(updateMap, SetOptions.merge())

                updateDidPlayHere()

                //we have the holes so lets go to the first one
                if (this.mapReady) {
                    goToHole()
                }
            }
        }
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayGolfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        vibe = getSystemService(VIBRATOR_SERVICE) as Vibrator

        mapFragment = SupportMapFragment.newInstance()
        mapFragment.getMapAsync(this)
        supportFragmentManager
            .beginTransaction()
            .add(binding.contentFrame.id, mapFragment)
            .commit()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation

                GolfApplication.me.geoPoint = location.geopoint
                updatePlayerMarker()

                //add course visitation
                updateDidPlayHere()

                updateDistances()
            }
        }

        locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionCheck(permissions)
        }

        binding.closeButton.setOnClickListener {
            finish()
        }
        binding.nextButton.setOnClickListener {
            vibrate()
            goToHole(increment = 1)
        }
        binding.previousButton.setOnClickListener {
            vibrate()
            goToHole(increment = -1)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.map = googleMap

        if (!hasLocationPermission) {
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            try {
                map.isMyLocationEnabled = true
                map.uiSettings.isMyLocationButtonEnabled = true
                startLocationUpdates()
            } catch (e: SecurityException) {
                Log.d("LOCATION", "location permission granted but rejected")
            }
        }

        val camera = CameraPosition(LatLng(40.0, -75.0), 3.5f, 0f, 0f)
        val update = CameraUpdateFactory.newCameraPosition(camera)
        this.map.mapType = MAP_TYPE_SATELLITE
        this.map.uiSettings.isCompassEnabled = false
        if (BuildConfig.DEBUG) {
            this.map.uiSettings.isZoomControlsEnabled = true
        }
        this.map.animateCamera(update)

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }
        map.setOnMapClickListener { //remove lines on map if we just click
            currentDistanceMarker?.apply { this.remove() }
            currentDistanceMarker = null

            removeMapLines()
        }
        map.setOnMapLongClickListener { latLng ->
            if (!isDraggingDistanceMarker) {
                vibrate()

                currentDistanceMarker?.apply { this.remove() }
                currentDistanceMarker = null
                currentDistanceMarker = map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .draggable(true)
                        .icon(getMapIcon(R.drawable.golf_ball_blank, 75))
                )?.also {
                    it.tag = "distance_marker"
                }
                updateDistanceMarker()
            }
        }
        map.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
                vibrate()

                isDraggingDistanceMarker = true
                marker.showInfoWindow()
            }
            override fun onMarkerDrag(marker: Marker) {
                if (marker.tag == currentDistanceMarker?.tag) {
                    updateDistanceMarker()
                }
            }
            override fun onMarkerDragEnd(marker: Marker) {
                isDraggingDistanceMarker = false
            }
        })

        this.mapReady = true

        val courseHoles = GolfApplication.course?.holes ?: return
        if (courseHoles.isNotEmpty()) {
            goToHole()
        }
    }

    private fun handlePermissionCheck(permissions: Map<String,Boolean>) {
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                try {
                    map.isMyLocationEnabled = true
                    map.uiSettings.isMyLocationButtonEnabled = true
                    startLocationUpdates()
                } catch (e: SecurityException) {
                    Log.d("LOCATION", "fine location permission granted but rejected")
                }
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                try {
                    map.isMyLocationEnabled = true
                    map.uiSettings.isMyLocationButtonEnabled = true
                    startLocationUpdates()
                } catch (e: SecurityException) {
                    Log.d("LOCATION", "coarse location permission granted but rejected")
                }
            }
            else -> {
                // No location access granted.
                try {
                    map.isMyLocationEnabled = false
                } catch (e: SecurityException) {
                    Log.d("LOCATION", "no location permission granted and still rejected")
                }
                map.uiSettings.isMyLocationButtonEnabled = false
            }
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission) {
            Log.d("LOCATION", "device has denied location access")
            return
        }

        //TODO: change priority if user device battery level is low?
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.d("LOCATION", "has location permission but failed to get location updates")
        }
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun vibrate() {
        vibe?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.vibrate(VibrationEffect.createOneShot(100, 1))
            } else this.vibrate(100)
        }
    }

    private fun removeMapLines() {
        lineToPin?.apply { this.remove() }
        lineToTee?.apply { this.remove() }
        lineToMyLocation?.apply { this.remove() }
    }

    private fun updateDidPlayHere() {
        val course: Course = GolfApplication.course ?: return
        GolfApplication.me.geoPoint?.let { myGeoPoint ->
            if (course.bounds.contains(myGeoPoint.latLng)) {
                GolfApplication.me.addCourseVisitation(course.id)
                course.didPlayHere = true
            }
        }
    }

    private fun goToHole(increment: Int = 0) {
        val course = GolfApplication.course ?: return

        currentDistanceMarker?.apply { this.remove() }
        currentDistanceMarker = null

        //remove any lines drawn to a click location
        removeMapLines()

        var holeNum = currentHole?.number ?: 1
        holeNum += increment
        if (holeNum > course.holes.size) {
            holeNum = 1
        } else if (holeNum <= 0) {
            holeNum = course.holes.size
        }
        binding.holeNumberLabel.text = "#$holeNum"

        val nextHole = course.holes.firstOrNull { it.number == holeNum } ?: return

        currentHole = nextHole

        updatePinMarker()
        updateTeeMarker()
        updateBunkerMarkers()

        updateDistances()

        moveCamera(nextHole.bounds)

        currentPinMarker?.showInfoWindow()

        //location manager will update elevation effect
        //trigger once in case we haven't moved yet
//        guard let hole = currentHole else {
//            return
//        }
//        if let myGeoPoint = self.me.geoPoint {
//            //update elevation numbers since we changed places!
//            if let pinElevation = hole.pinElevation {
//                ShotTools.getElevationChange(start: myGeoPoint, finishElevation: pinElevation, completion: calculateElevation)
//            } else if let pinPosition = hole.pinLocation {
//                ShotTools.getElevationChange(start: myGeoPoint, finish: pinPosition, completion: calculateElevation)
//            }
//        } else if let pinElevation = currentHole.pinElevation {
//            ShotTools.getElevationChange(start: hole.teeLocations.first!, finishElevation: pinElevation, completion: calculateElevation)
//        } else {
//            ShotTools.getElevationChange(start: hole.teeLocations.first!, finish: hole.pinLocation!, completion: calculateElevation)
//        }
    }

    private fun updatePlayerMarker() {
        val meLocation:LatLng = GolfApplication.me.geoPoint?.latLng ?: return

        val myMarker:Marker? = meMarker
        if (myMarker == null) {
            val markerOptions = MarkerOptions()
                .position(meLocation)
                .title("Me")
                .icon(getMapIcon(R.drawable.player_marker))
//            myPlayerMarker!.icon = bitmojiImage.toNewSize(CGSize(width: 55, height: 55))
            meMarker = map.addMarker(markerOptions)
        } else {
            myMarker.position = meLocation
        }
    }

    private fun updateBunkerMarkers() {
        currentBunkerMarkers.forEach { it.remove() }
        currentBunkerMarkers.clear()

        val holeNumber: Int = currentHole?.number ?: return
        val teePoint: GeoPoint = currentHole?.teeLocations?.firstOrNull() ?: return
        val bunkerLocationsForHole:List<GeoPoint> = currentHole?.bunkerLocations ?: return

        bunkerLocationsForHole.forEachIndexed { bunkerIndex, bunkerGeoPoint ->
            val distanceToBunker:Int = MapTools.distanceFrom(bunkerGeoPoint, teePoint)

            val markerOptions = MarkerOptions()
                .position(bunkerGeoPoint.latLng)
                .title("Hazard")
                .snippet(distanceToBunker.distance)
                .icon(getMapIcon(R.drawable.hazard_marker))
            map.addMarker(markerOptions)?.let { marker ->
                marker.setTag("$holeNumber:T$bunkerIndex")
                currentBunkerMarkers.add(marker)
            }
        }
    }

    private fun updateTeeMarker() {
        val holeNumber: Int = currentHole?.number ?: return

        val teePoint: GeoPoint = currentHole?.teeLocations?.firstOrNull() ?: return

        val teeMarker:Marker? = currentTeeMarker
        if (teeMarker == null) {
            val markerOptions = MarkerOptions()
                .position(teePoint.latLng)
                .title("Tee #${holeNumber}")
                .icon(getMapIcon(R.drawable.tee_marker))
            currentTeeMarker = map.addMarker(markerOptions)
            currentTeeMarker!!.tag = "$holeNumber:T"
        } else {
            teeMarker.position = teePoint.latLng
            teeMarker.title = "Tee #$holeNumber"
            teeMarker.tag = "$holeNumber:T"
        }
    }

    private fun updatePinMarker() {
        val holeNumber: Int = currentHole?.number ?: return
        val pinPoint = currentHole?.pinLocation ?: return
        val distanceToPin:Int = currentHole?.distanceToPinFromTee ?: return

        val pinMarker:Marker? = currentPinMarker
        if (pinMarker == null) {
            val markerOptions = MarkerOptions()
                .position(pinPoint.latLng)
                .title("Pin #${holeNumber}")
                .snippet("$distanceToPin yds")
                .icon(getMapIcon(R.drawable.flag_marker))
            currentPinMarker = map.addMarker(markerOptions)
            currentPinMarker!!.tag = "$holeNumber:P"
        } else {
            pinMarker.position = pinPoint.latLng
            pinMarker.title = "Pin #$holeNumber"
            pinMarker.snippet = "$distanceToPin yds"
            pinMarker.tag = "$holeNumber:P"
        }

//        new GetElevationTask().execute(currentPinLatLng);
//        double pinAltitude = MapTools.getAltitude(currentPinLatLng);
//        double teeAltitude = MapTools.getAltitude(currentHole.teeLocations.get(0));

//        if (mCurrentLocation != null && currentPinLatLng != null) {
//            waitingForLocation = false
//            val myYardsToPin: Double = MapTools.distanceFrom(
//                LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()),
//                currentPinLatLng
//            )
//            distanceToPinTV.setText(myYardsToPin as Int.toString() + " yds")
//        } else {
//            waitingForLocation = true
//        }
    }

    private fun updateDistances() {
        //update any suggestion lines, driver or others
        updateSuggestionLines()

        //update any distance markers we already have displayed when we update our location
        updateDistanceMarker()

        //update yardage
        val distance = distanceToMeFromPin ?: currentHole?.distanceToPinFromTee ?: return
        binding.distanceToPin.text = distance.distance
        binding.suggestedClub.text = GolfApplication.me.bag.getClubSuggestion(distance).name
    }

    private val distanceToMeFromTee:Int?
        get() = MapTools.distanceFrom(meMarker?.position, currentTeeMarker?.position)
    private val distanceToMeFromPin:Int?
        get() = MapTools.distanceFrom(meMarker?.position, currentPinMarker?.position)
    private val distanceToPressFromTee:Int?
        get() = MapTools.distanceFrom(currentTeeMarker?.position, currentDistanceMarker?.position)
    private val distanceToPressFromLocation:Int?
        get() = MapTools.distanceFrom(GolfApplication.me.geoPoint?.latLng, currentDistanceMarker?.position)
    private val distanceToPressFromPin:Int?
        get() = MapTools.distanceFrom(currentPinMarker?.position, currentDistanceMarker?.position)

    private fun updateDistanceMarker() {
        val distanceMarker: Marker = currentDistanceMarker ?: return

        //redraw line to pin
        updateDistanceLineToPin(distanceMarker.position)
        updateDistanceLineToTee(distanceMarker.position)
        updateDistanceLineToMe(distanceMarker.position)

        //draw line to player or tee depending on current player location
        val distanceToTee = distanceToPressFromTee
        val distanceToMe = distanceToPressFromLocation

        val distance:Int? = when {
            distanceToTee == null -> distanceToMe
            distanceToMe != null && distanceToMe < distanceToTee + 25 -> distanceToMe
            else -> distanceToTee
        }

        val suggestedClub: Club? = if (distance != null) GolfApplication.me.bag.getClubSuggestion(distance) else null

        distanceMarker.title = distance?.distance ?: "Distance Marker"
//        distanceMarker.snippet = distanceToPressFromPin?.distance
        distanceMarker.snippet = suggestedClub?.name
        distanceMarker.showInfoWindow()
    }

    private fun moveCamera(bounds: LatLngBounds) {
        val teeLocation: GeoPoint = currentHole?.teeLocations?.firstOrNull() ?: return
        val pinLocation: GeoPoint = currentHole?.pinLocation ?: return
        val bearing = MapTools.calculateBearing(teeLocation, pinLocation) - 20f

        val zoom: Float = MapTools.getBoundsZoomLevel(bounds, this.binding.contentFrame)
        val center: LatLng = MapTools.getBoundsCenter(bounds)

        val camera = CameraPosition(center, zoom, 45f, bearing)
        val update = CameraUpdateFactory.newCameraPosition(camera)
        this.map.animateCamera(update)
    }

    private fun updateDistanceLineToPin(clickLatLng: LatLng) {
        lineToPin?.apply { this.remove() }

        val pinLatLng = currentHole?.pinLocation?.latLng ?: return
        lineToPin = map.addPolyline(
            PolylineOptions()
                .add(clickLatLng)
                .add(pinLatLng)
                .width(2f)
                .color(Color.WHITE)
                .geodesic(true)
        )
    }
    private fun updateDistanceLineToTee(clickLatLng: LatLng) {
        lineToTee?.apply { this.remove() }

        val teeLatLng = currentHole?.teeLocations?.first()?.latLng ?: return
        lineToTee = map.addPolyline(
            PolylineOptions()
                .add(clickLatLng)
                .add(teeLatLng)
                .width(2f)
                .color(Color.WHITE)
                .geodesic(true)
        )
    }
    private fun updateDistanceLineToMe(clickLatLng: LatLng) {
        lineToMyLocation?.apply { this.remove() }

        val playerLocation = GolfApplication.me.geoPoint?.latLng ?: return
        lineToMyLocation = map.addPolyline(
            PolylineOptions()
                .add(clickLatLng)
                .add(playerLocation)
                .width(2f)
                .color(Color.WHITE)
        )
    }

    private fun clearDistanceLines() {
        for (line in drivingDistanceLines) {
            line.remove()
        }
        for (line in suggestedDistanceLines) {
            line.remove()
        }
        drivingDistanceLines.clear()
        suggestedDistanceLines.clear()
    }

    private fun updateSuggestionLines() {
        clearDistanceLines()

        //do not draw any lines if we do not have pin or tee information
        val distancePinTee = currentHole?.distanceToPinFromTee ?: return

        val distancePinMe = distanceToMeFromPin
        val distanceTeeMe = distanceToMeFromTee

        //if we do not have player location, attempt to show driving distance only
        if (distancePinMe == null || distanceTeeMe == null) {
            updateDrivingDistanceLines()
            return
        }

        val suggestedClub:Club = GolfApplication.me.bag.getClubSuggestion(distancePinMe)

        val meIsCloseToPin:Boolean = distancePinMe < (distancePinTee - 30)
        val meIsCloseToSelectedHole:Boolean = (distanceTeeMe + distancePinMe) < (distancePinTee + 75)

        //if we are not being suggested the driver -> show the resulting suggested club arcs
        if (meIsCloseToPin && meIsCloseToSelectedHole) {
            updateRecommendedClubLines(suggestedClub)
        } else {
            //not close to the pin OR not close to the selected hole
            updateDrivingDistanceLines()
        }
    }

    private fun updateDrivingDistanceLines() {
        clearDistanceLines()

        val teeLocation:GeoPoint = currentHole?.teeLocations?.firstOrNull() ?: return
        val bearingToTarget = currentHole?.bearingToDogLeg ?: currentHole?.bearingToPinFromTee ?: return

        val minBearing:Int = (bearingToTarget - 12f).toInt()
        val maxBearing:Int = (bearingToTarget + 12f).toInt()

        val teeYardsToPin:Int = currentHole?.distanceToPinFromTee ?: return

        val driver = Club(1)
        if (driver.distance > teeYardsToPin) {
            return
        }

        for (i in 0..2) {
            val drivingClub = Club(i + 1)
            Log.d("LINES", "club distance: ${drivingClub.name} - ${drivingClub.distance}")
            @ColorInt val lineColor:Int = drivingDistanceLineColors[i]

            val distancePoints:MutableList<LatLng> = mutableListOf()
            for (angle in minBearing..maxBearing) {
                val distanceLatLng = MapTools.coordinates(
                    teeLocation.latLng,
                    (drivingClub.distance).toDouble(),
                    (angle).toDouble()
                )
                distancePoints.add(distanceLatLng)
            }

            val clubDistanceLine = map.addPolyline(
                PolylineOptions()
                    .addAll(distancePoints)
                    .width(8f)
                    .color(lineColor)
                    .geodesic(true)
            )
            drivingDistanceLines.add(clubDistanceLine)
        }

        Log.d("LINES", "did draw driving distance lines")
    }
    private fun updateRecommendedClubLines(suggestedClub:Club) {
        clearDistanceLines()

        val myGeopoint = GolfApplication.me.geoPoint ?: return
        val pinGeopoint:GeoPoint = currentHole?.pinLocation ?: return
        val distancePinMe = distanceToMeFromPin ?: return

        val bearingToPin:Float = MapTools.calculateBearing(myGeopoint, pinGeopoint)
        val minBearing:Int = (bearingToPin - 12f).toInt()
        val maxBearing:Int = (bearingToPin + 12f).toInt()

        //only show suggestion line if the min distance is less than current distance
        val shortestWedge:Club = GolfApplication.me.bag.myClubs.lastOrNull() ?: return
        if (shortestWedge.distance > distancePinMe) {
            //no need to draw lines because we are too close to the hole to need recommendation
            return
        }

        //show up to 2 club ups - if suggesting driver then 0 change allowed
        val clubUps:Int = -min(suggestedClub.number - 1, 2)

        //show up to 2 club downs but not past smallest club
        val clubDowns:Int = min(GolfApplication.me.bag.myClubs.size - suggestedClub.number, 2) + 1

        for (i in clubUps..clubDowns) {
            val clubSelectionToShow:Club = Club(suggestedClub.number + i)

            @ColorInt val lineColor:Int = when (i) {
                -1 -> Color.RED
                0 -> Color.GREEN
                1 -> Color.YELLOW
                else -> Color.argb(64, 255, 255, 255)
            }

            val distancePoints:MutableList<LatLng> = mutableListOf()
            for (angle in minBearing..maxBearing) {
                val distanceLatLng = MapTools.coordinates(
                    myGeopoint.latLng,
                    clubSelectionToShow.distance.toDouble(),
                    angle.toDouble()
                )
                distancePoints.add(distanceLatLng)
            }

            val clubDistanceLine = map.addPolyline(
                PolylineOptions()
                    .addAll(distancePoints)
                    .width(5f)
                    .color(lineColor)
                    .geodesic(true)
            )
            suggestedDistanceLines.add(clubDistanceLine)
        }
        Log.d("LINES", "did draw suggested club distance lines")
    }
}