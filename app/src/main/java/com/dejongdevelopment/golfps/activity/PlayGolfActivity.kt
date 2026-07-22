package com.dejongdevelopment.golfps.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.app.AlertDialog
import android.os.*
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.core.app.ActivityCompat
import androidx.core.view.size
import androidx.fragment.app.FragmentActivity
import com.dejongdevelopment.golfps.BuildConfig
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.databinding.ActivityPlayGolfBinding
import com.dejongdevelopment.golfps.models.Hole
import com.dejongdevelopment.golfps.tools.MapTools
import com.dejongdevelopment.golfps.tools.AnalyticsLogger
import com.dejongdevelopment.golfps.tools.CourseTools
import com.dejongdevelopment.golfps.tools.LocationUpdateTimer
import com.dejongdevelopment.golfps.tools.LocationUpdateTimerDelegate
import com.dejongdevelopment.golfps.tools.PlayerUpdateTimer
import com.dejongdevelopment.golfps.tools.PlayerUpdateTimerDelegate
import com.dejongdevelopment.golfps.util.latLng
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.GeoPoint
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.models.Club
import com.dejongdevelopment.golfps.models.Course
import com.dejongdevelopment.golfps.models.Player
import com.dejongdevelopment.golfps.util.distance
import com.dejongdevelopment.golfps.util.geopoint
import com.dejongdevelopment.golfps.util.toYards
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.drawable.Drawable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.min

class PlayGolfActivity : FragmentActivity(), OnMapReadyCallback,
    LocationUpdateTimerDelegate, PlayerUpdateTimerDelegate {

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
    private var myDrivingDistanceMarker: Marker? = null
    private var currentLongDriveMarkers: MutableList<Marker> = mutableListOf()
    private var longDriveControlsExpanded = false

    private val locationUpdateTimer = LocationUpdateTimer()
    private val playerUpdateTimer = PlayerUpdateTimer()
    private var playerListener: ListenerRegistration? = null
    private var previousPublishedLocation: GeoPoint? = null
    private var otherPlayers: List<Player> = listOf()
    private val otherPlayerMarkers = mutableMapOf<String, Marker>()

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
        showAmbassadorMessageIfNeeded(course)

        if (course.holes.isNotEmpty() && this.mapReady) {
            goToHole()
        }
        if (mapReady) startLivePlayerUpdates(course)

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
            } else {
                Log.d("HOLES", "No success in getting holes")
            }
        }
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        stopLivePlayerUpdates()
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
                val location = locationResult.lastLocation ?: return

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
        binding.longDriveButton.setOnClickListener {
            AnalyticsLogger.log("click_long_drive")
            longDriveControlsExpanded = !longDriveControlsExpanded
            updateLongDriveControls()
        }
        binding.markDriveButton.setOnClickListener {
            AnalyticsLogger.log("click_long_drive_mark")
            addDrivePrompt()
        }
        binding.clearDriveButton.setOnClickListener {
            AnalyticsLogger.log("click_long_drive_clear")
            clearMyLongDrive()
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

                isDraggingDistanceMarker = marker == currentDistanceMarker
                marker.showInfoWindow()
            }
            override fun onMarkerDrag(marker: Marker) {
                if (marker.tag == currentDistanceMarker?.tag) {
                    updateDistanceMarker()
                }
            }
            override fun onMarkerDragEnd(marker: Marker) {
                isDraggingDistanceMarker = false
                saveAmbassadorMarkerMove(marker)
            }
        })

        this.mapReady = true

        GolfApplication.course?.let { startLivePlayerUpdates(it) }

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

    private fun startLivePlayerUpdates(course: Course) {
        stopLivePlayerUpdates(removeMarkers = false)
        previousPublishedLocation = null

        locationUpdateTimer.delegate = this
        locationUpdateTimer.startNewTimer(interval = 5.0)
        playerUpdateTimer.delegate = this
        playerUpdateTimer.startNewTimer(interval = 30.0)

        playerListener = Firebase.firestore.collection("players")
            .whereEqualTo("course", course.id)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.d("PLAYERS", exception.localizedMessage ?: "Error fetching player locations")
                    return@addSnapshotListener
                }

                val cutoff = System.currentTimeMillis() - FOUR_HOURS_MS
                otherPlayers = snapshot?.documents.orEmpty().mapNotNull { document ->
                    if (document.id == GolfApplication.me.id) return@mapNotNull null
                    val data = document.data ?: return@mapNotNull null
                    Player(document.id, data).takeIf { player ->
                        (player.lastLocationUpdate?.time ?: 0L) > cutoff
                    }
                }
                updatePlayersNow()
            }
    }

    private fun stopLivePlayerUpdates(removeMarkers: Boolean = true) {
        locationUpdateTimer.invalidate()
        playerUpdateTimer.invalidate()
        playerListener?.remove()
        playerListener = null
        otherPlayers = listOf()
        if (removeMarkers) {
            otherPlayerMarkers.values.forEach { it.remove() }
            otherPlayerMarkers.clear()
        }
    }

    override fun updateLocationsNow() {
        if (!GolfApplication.me.shareLocation) return
        val course = GolfApplication.course ?: return
        val location = GolfApplication.me.geoPoint ?: return
        val previous = previousPublishedLocation
        if (previous != null && MapTools.distanceFrom(previous, location) < 25) return

        previousPublishedLocation = location
        GolfApplication.me.docReference?.set(
            mapOf(
                "course" to course.id,
                "location" to location,
                "updateTime" to Timestamp.now()
            ),
            SetOptions.merge()
        )?.addOnFailureListener {
            Log.d("PLAYERS", it.localizedMessage ?: "Error publishing player location")
        }
    }

    override fun updatePlayersNow() {
        if (!mapReady) return
        val course = GolfApplication.course ?: return
        val validPlayerIds = otherPlayers.map { it.id }.toSet()

        otherPlayerMarkers.keys.filterNot { validPlayerIds.contains(it) }.forEach { playerId ->
            otherPlayerMarkers.remove(playerId)?.remove()
        }

        otherPlayers.forEach { player ->
            val markerLocation = playerMarkerLocation(player, course) ?: return@forEach
            val isSpectator = player.geoPoint == null ||
                (course.holes.isNotEmpty() && !course.bounds.contains(player.geoPoint!!.latLng))
            val marker = otherPlayerMarkers[player.id] ?: map.addMarker(
                MarkerOptions()
                    .position(markerLocation)
                    .title(if (isSpectator) "Spectator" else "Golfer")
                    .icon(getMapIcon(R.drawable.player_marker, 75))
            )?.also {
                it.tag = "player:${player.id}"
                otherPlayerMarkers[player.id] = it
            } ?: return@forEach

            marker.position = markerLocation
            marker.title = if (isSpectator) "Spectator" else "Golfer"
            val age = System.currentTimeMillis() - (player.lastLocationUpdate?.time ?: 0L)
            marker.alpha = if (age > ONE_MINUTE_MS) 0.75f else 1f
            player.avatarURL?.let { loadPlayerAvatar(player.id, it.toString()) }
        }
    }

    private fun playerMarkerLocation(player: Player, course: Course): LatLng? {
        val playerLocation = player.geoPoint?.latLng
        if (playerLocation != null &&
            (course.holes.isEmpty() || course.bounds.contains(playerLocation))) return playerLocation
        val spectation = course.spectation ?: return playerLocation

        val offsetSeed = player.id.hashCode()
        val latitudeOffset = ((offsetSeed and 0xff) / 255.0 - 0.5) * 0.00002
        val longitudeOffset = (((offsetSeed shr 8) and 0xff) / 255.0 - 0.5) * 0.00002
        return LatLng(
            spectation.latitude + latitudeOffset,
            spectation.longitude + longitudeOffset
        )
    }

    private fun loadPlayerAvatar(playerId: String, avatarUrl: String) {
        val marker = otherPlayerMarkers[playerId] ?: return
        if (marker.tag == "player-avatar:$avatarUrl") return
        marker.tag = "player-avatar:$avatarUrl"

        Glide.with(this)
            .asBitmap()
            .load(avatarUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val currentMarker = otherPlayerMarkers[playerId] ?: return
                    if (currentMarker.tag != "player-avatar:$avatarUrl") return
                    val scaled = Bitmap.createScaledBitmap(resource, 75, 75, false)
                    currentMarker.setIcon(BitmapDescriptorFactory.fromBitmap(scaled))
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit
            })
    }

    companion object {
        private const val ONE_MINUTE_MS = 60_000L
        private const val FOUR_HOURS_MS = 4 * 60 * 60 * 1000L
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

    private fun showAmbassadorMessageIfNeeded(course: Course) {
        if (!GolfApplication.me.isAmbassadorOf(course) || GolfApplication.me.didSeeAmbassadorMessage) return

        AlertDialog.Builder(this)
            .setIcon(R.drawable.ambassador)
            .setTitle(R.string.play_ambassador_title)
            .setMessage(R.string.play_ambassador_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                GolfApplication.me.didSeeAmbassadorMessage = true
            }
            .show()
    }

    private fun saveAmbassadorMarkerMove(marker: Marker) {
        val course = GolfApplication.course ?: return
        if (!GolfApplication.me.isAmbassadorOf(course)) return
        val hole = currentHole ?: return

        val successfulMove = when {
            marker == currentTeeMarker -> hole.saveNewTeeLocation(marker.position.geopoint)
            marker == currentPinMarker -> hole.saveNewPinLocation(marker.position.geopoint)
            currentBunkerMarkers.contains(marker) -> hole.saveNewBunkerLocations(
                currentBunkerMarkers.map { it.position.geopoint }
            )
            else -> return
        }

        if (!successfulMove) {
            updateTeeMarker()
            updatePinMarker()
            updateBunkerMarkers()
            AlertDialog.Builder(this)
                .setTitle(R.string.play_ambassador_move_error_title)
                .setMessage(R.string.play_ambassador_move_error_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        GolfApplication.me.didModifyAmbassadorCourse = true
        updateTeeMarker()
        updatePinMarker()
        updateBunkerMarkers()
        updateDistances()
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

        longDriveControlsExpanded = false

        updatePinMarker()
        updateTeeMarker()
        updateBunkerMarkers()

        if (nextHole.isLongDrive) {
            CourseTools.getLongestDrives(nextHole) { success, exception ->
                if (success && currentHole === nextHole) {
                    runOnUiThread {
                        updateLongDriveMarkers()
                        updateLongDriveControls()
                    }
                } else if (exception != null) {
                    Log.d("LONG_DRIVE", exception.localizedMessage ?: "Error getting long drives")
                }
            }
        } else {
            updateLongDriveMarkers()
        }
        updateLongDriveControls()

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

    private fun updateLongDriveControls() {
        val hole = currentHole
        if (hole?.isLongDrive != true) {
            binding.longDriveControls.visibility = View.GONE
            return
        }

        binding.longDriveControls.visibility = View.VISIBLE
        val driveDistance = if (GolfApplication.metric) {
            hole.myLongestDriveInMeters
        } else {
            hole.myLongestDriveInYards
        }
        val hasDrive = driveDistance != null

        binding.myDriveLabel.text = driveDistance?.distance ?: ""
        binding.myDriveLabel.visibility = if (hasDrive && longDriveControlsExpanded) View.VISIBLE else View.GONE
        binding.markDriveButton.visibility = if (!hasDrive && longDriveControlsExpanded) View.VISIBLE else View.GONE
        binding.clearDriveButton.visibility = if (hasDrive && longDriveControlsExpanded) View.VISIBLE else View.GONE
    }

    private fun updateLongDriveMarkers() {
        currentLongDriveMarkers.forEach { it.remove() }
        currentLongDriveMarkers.clear()
        myDrivingDistanceMarker?.remove()
        myDrivingDistanceMarker = null

        val hole = currentHole ?: return
        val teeLocation = hole.teeLocations.firstOrNull() ?: return
        hole.longestDrives.forEach { (playerId, driveLocation) ->
            val isMine = playerId == GolfApplication.me.id
            val distanceToTee = MapTools.distanceFrom(teeLocation, driveLocation)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(driveLocation.latLng)
                    .title(if (isMine) "My Drive" else "Long Drive")
                    .snippet(distanceToTee.distance)
                    .icon(getMapIcon(R.drawable.golf_ball_blank, if (isMine) 45 else 35))
            ) ?: return@forEach
            marker.tag = "Drive"

            if (isMine) {
                myDrivingDistanceMarker = marker
            } else {
                currentLongDriveMarkers.add(marker)
            }
        }
    }

    private fun addDrivePrompt() {
        AlertDialog.Builder(this)
            .setTitle(R.string.play_add_drive_title)
            .setMessage(R.string.play_add_drive_message)
            .setPositiveButton(R.string.play_add_drive_yes) { _, _ -> addLongDriveAtCurrentLocation() }
            .setNegativeButton(R.string.play_add_drive_no, null)
            .show()
    }

    private fun addLongDriveAtCurrentLocation() {
        val hole = currentHole ?: return
        val location = GolfApplication.me.geoPoint
        val teeLocation = hole.teeLocations.firstOrNull()
        if (location == null || teeLocation == null) {
            Toast.makeText(this, R.string.play_drive_location_unavailable, Toast.LENGTH_LONG).show()
            return
        }

        val distanceToTee = MapTools.distanceFrom(teeLocation, location)
        val distanceInYards = if (GolfApplication.metric) distanceToTee.toYards() else distanceToTee
        if (distanceInYards > 500) {
            Toast.makeText(this, R.string.play_drive_too_far, Toast.LENGTH_LONG).show()
            return
        }

        val driveDocument = hole.docReference?.collection("drives")?.document(GolfApplication.me.id)
        if (driveDocument == null) {
            Toast.makeText(this, R.string.play_drive_save_failed, Toast.LENGTH_LONG).show()
            return
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val driveData = mapOf(
            "location" to location,
            "distance" to distanceInYards,
            "date" to formatter.format(Date())
        )
        driveDocument.set(driveData)
            .addOnSuccessListener {
                hole.setLongestDrive(distanceToTee)
                hole.longestDrives[GolfApplication.me.id] = location
                GolfApplication.me.didLogLongDrive = true
                longDriveControlsExpanded = true
                updateLongDriveMarkers()
                updateLongDriveControls()
                myDrivingDistanceMarker?.showInfoWindow()
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.play_drive_save_failed, Toast.LENGTH_LONG).show()
                Log.d("LONG_DRIVE", it.localizedMessage ?: "Error saving long drive")
            }
    }

    private fun clearMyLongDrive() {
        val hole = currentHole ?: return
        val driveDocument = hole.docReference?.collection("drives")?.document(GolfApplication.me.id)
            ?: return
        driveDocument.delete()
            .addOnSuccessListener {
                hole.setLongestDrive(null)
                hole.longestDrives.remove(GolfApplication.me.id)
                myDrivingDistanceMarker?.remove()
                myDrivingDistanceMarker = null
                longDriveControlsExpanded = true
                updateLongDriveControls()
            }
            .addOnFailureListener {
                Toast.makeText(this, R.string.play_drive_save_failed, Toast.LENGTH_LONG).show()
                Log.d("LONG_DRIVE", it.localizedMessage ?: "Error clearing long drive")
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
                .draggable(GolfApplication.course?.let { GolfApplication.me.isAmbassadorOf(it) } == true)
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
                .draggable(GolfApplication.course?.let { GolfApplication.me.isAmbassadorOf(it) } == true)
            currentTeeMarker = map.addMarker(markerOptions)
            currentTeeMarker!!.tag = "$holeNumber:T"
        } else {
            teeMarker.position = teePoint.latLng
            teeMarker.title = "Tee #$holeNumber"
            teeMarker.tag = "$holeNumber:T"
            teeMarker.isDraggable = GolfApplication.course?.let { GolfApplication.me.isAmbassadorOf(it) } == true
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
                .draggable(GolfApplication.course?.let { GolfApplication.me.isAmbassadorOf(it) } == true)
            currentPinMarker = map.addMarker(markerOptions)
            currentPinMarker!!.tag = "$holeNumber:P"
        } else {
            pinMarker.position = pinPoint.latLng
            pinMarker.title = "Pin #$holeNumber"
            pinMarker.snippet = "$distanceToPin yds"
            pinMarker.tag = "$holeNumber:P"
            pinMarker.isDraggable = GolfApplication.course?.let { GolfApplication.me.isAmbassadorOf(it) } == true
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
        binding.suggestedClub.text = GolfApplication.me.bag.getClubSuggestion(distance)?.name ?: "-"
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
        val distance = MapTools.distanceFrom(teeLocation, pinLocation)

        val zoom1: Float = MapTools.getBoundsZoomLevel(bounds, this.binding.contentFrame)
        val zoom2: Float = MapTools.getCircularZoom(distance.toDouble(), 100.0, this.binding.contentFrame)
        val center: LatLng = MapTools.getBoundsCenter(bounds)

        val camera = CameraPosition(center, zoom2, 45f, bearing)
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

        val suggestedClub:Club = GolfApplication.me.bag.getClubSuggestion(distancePinMe) ?: return

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

        val driver = GolfApplication.me.bag.myClubs.firstOrNull() ?: return
        if (driver.distance > teeYardsToPin) {
            return
        }

        val drivingClubs = GolfApplication.me.bag.myClubs.take(3)
        for ((i, drivingClub) in drivingClubs.withIndex()) {
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

        val suggestedClubIndex = GolfApplication.me.bag.myClubs.indexOfFirst { it.id == suggestedClub.id }
        if (suggestedClubIndex == -1) {
            return
        }

        //show up to 2 club ups - if suggesting driver then 0 change allowed
        val clubUps:Int = -min(suggestedClubIndex, 2)

        //show up to 2 club downs but not past smallest club
        val clubDowns:Int = min(GolfApplication.me.bag.myClubs.lastIndex - suggestedClubIndex, 2)

        for (i in clubUps..clubDowns) {
            val clubSelectionToShow:Club = GolfApplication.me.bag.myClubs[suggestedClubIndex + i]

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
