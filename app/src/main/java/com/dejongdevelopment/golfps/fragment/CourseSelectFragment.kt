package com.dejongdevelopment.golfps.fragment

import android.Manifest
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dejongdevelopment.golfps.BuildConfig
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.activity.AddCourseActivity
import com.dejongdevelopment.golfps.activity.PlayGolfActivity
import com.dejongdevelopment.golfps.adapters.CourseSelectAdapter
import com.dejongdevelopment.golfps.adapters.CourseSelectAdapter.CourseListItem
import com.dejongdevelopment.golfps.databinding.FragmentCourseSelectBinding
import com.dejongdevelopment.golfps.models.Course
import com.dejongdevelopment.golfps.tools.AnalyticsLogger
import com.dejongdevelopment.golfps.tools.AppUtility
import com.dejongdevelopment.golfps.tools.CourseTools
import com.dejongdevelopment.golfps.tools.DebugLogger
import com.dejongdevelopment.golfps.tools.LocationPermissionTools
import com.dejongdevelopment.golfps.tools.MapTools
import com.dejongdevelopment.golfps.util.fuzzyMatch
import com.dejongdevelopment.golfps.util.geopoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class CourseSelectFragment: Fragment() {
    private var _binding: FragmentCourseSelectBinding? = null
    private val binding get() = _binding!!

    private var allGolfCourses: List<Course> = listOf()
    private var primaryAmbassadorCourses: List<Course> = listOf()
    private var primaryNearbyCourses: List<Course> = listOf()
    private var primaryVisitedCourses: List<Course> = listOf()
    private var searchResults: List<Course> = listOf()
    private var courseDistances: Map<String, String> = mapOf()
    private var isShowingSearchResults = false
    private var isLoadingCourses = true
    private var isShowingAvailableFallback = false
    private var isLoadingMoreAvailableCourses = false
    private var hasMoreAvailableCourses = false
    private var availableCourseCursor: DocumentSnapshot? = null
    private var didRequestLocationPermissionThisSession = false
    private var refreshAfterCurrentLoad = false
    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var courseSelectAdapter: CourseSelectAdapter

    private data class NearbyCourseResult(
        val courses: List<Course>,
        val hasKnownLocation: Boolean
    )

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val hasLocationPermission: Boolean
        get() = LocationPermissionTools.hasLocationPermission(context)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourseSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            LocationPermissionTools.markRequested()
            val didGrantLocation = permissions.values.any { it }
            if (!didGrantLocation) {
                updateLocationPermissionPrompt()
                renderCourseList()
                return@registerForActivityResult
            }

            updateLocationPermissionPrompt()
            if (isLoadingCourses) {
                refreshAfterCurrentLoad = true
            } else {
                getCourses()
            }
        }

        courseSelectAdapter = CourseSelectAdapter(listOf()) { goToCourse(it) }
        binding.availableCourseRecyclerView.adapter = courseSelectAdapter
        binding.loadingCoursesBar.visibility = View.INVISIBLE
        binding.courseRefreshLayout.setColorSchemeResources(R.color.grass, R.color.gold)
        binding.courseRefreshLayout.setOnRefreshListener {
            binding.editTextCourseFilter.setText("")
            isShowingSearchResults = false
            searchResults = listOf()
            getCourses()
        }
        binding.availableCourseRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0 || !isShowingAvailableFallback || isShowingSearchResults) {
                    return
                }

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
                val itemCount = recyclerView.adapter?.itemCount ?: return
                if (lastVisiblePosition >= itemCount - AVAILABLE_LOAD_MORE_THRESHOLD) {
                    loadMoreAvailableCourses()
                }
            }
        })

        getCourses()

        binding.editTextCourseFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val courseSearchText = s.toString()

                if (courseSearchText.length <= 1) {
                    isShowingSearchResults = false
                    searchResults = listOf()
                    renderCourseList()
                    return
                }

                queryCourses(courseSearchText)
            }
        })

        binding.addCourseButton.setOnClickListener {
            context?.apply {
                val intent = Intent(this, AddCourseActivity::class.java)
                ContextCompat.startActivity(this, intent, null)
            }
        }

        binding.openLocationSettingsButton.setOnClickListener {
            AnalyticsLogger.log("click_location_permission_settings")
            if (!AppUtility.openAppSettings(requireContext())) {
                Toast.makeText(
                    requireContext(),
                    R.string.location_permission_unable_to_open_settings,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        updateLocationPermissionPrompt()
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) {
            return
        }

        val wasShowingAvailableCourses = isShowingAvailableFallback
        updateLocationPermissionPrompt()
        if (hasLocationPermission && wasShowingAvailableCourses && !isLoadingCourses) {
            getCourses()
        }
    }

    private fun getCourses() {
        isLoadingCourses = true
        isShowingAvailableFallback = false
        resetAvailableCoursePagination()
        renderCourseList()
        binding.loadingCoursesBar.visibility =
            if (binding.courseRefreshLayout.isRefreshing) View.INVISIBLE else View.VISIBLE

        loadNearbyCourses { nearbyResult ->
            val courseIDsToLoad = GolfApplication.me.ambassadorCourses +
                    (GolfApplication.me.coursesVisited?.toList() ?: listOf())

            CourseTools.getCourses(withIDs = courseIDsToLoad) { savedCourses, error ->
                error?.let {
                    DebugLogger.report(it, "Error retrieving saved courses.")
                }

                if (!nearbyResult.hasKnownLocation && nearbyResult.courses.isEmpty()) {
                    CourseTools.getCourses(limit = AVAILABLE_COURSE_PAGE_SIZE) { fallbackPage, fallbackError ->
                        fallbackError?.let {
                            DebugLogger.report(it, "Error retrieving fallback courses.")
                        }
                        availableCourseCursor = fallbackPage.lastDocument
                        hasMoreAvailableCourses = fallbackPage.hasMore
                        updatePrimaryCourses(nearbyResult, savedCourses, fallbackPage.courses)
                    }
                } else {
                    updatePrimaryCourses(nearbyResult, savedCourses, listOf())
                }
            }
        }
    }

    private fun updatePrimaryCourses(
        nearbyResult: NearbyCourseResult,
        savedCourses: List<Course>,
        fallbackCourses: List<Course>
    ) {
        val ambassadorIDs = GolfApplication.me.ambassadorCourses.toSet()
        val visitedIDs = GolfApplication.me.coursesVisited ?: setOf()

        primaryAmbassadorCourses = savedCourses
            .filter { ambassadorIDs.contains(it.id) }
            .sortedWith(::defaultSort)
        primaryVisitedCourses = savedCourses
            .filter { visitedIDs.contains(it.id) && !ambassadorIDs.contains(it.id) }
            .sortedWith(::defaultSort)

        val nearbyCandidates = if (nearbyResult.hasKnownLocation) {
            isShowingAvailableFallback = false
            nearbyResult.courses
        } else {
            isShowingAvailableFallback = true
            fallbackCourses
        }

        primaryNearbyCourses = nearbyCandidates
            .filter { !ambassadorIDs.contains(it.id) && !visitedIDs.contains(it.id) }
            .sortedWith(::defaultSort)

        courseDistances = courseDistanceLabels(nearbyCandidates + savedCourses)

        finishCourseLoading()
    }

    private fun loadMoreAvailableCourses() {
        if (!isShowingAvailableFallback ||
            isLoadingCourses ||
            isLoadingMoreAvailableCourses ||
            !hasMoreAvailableCourses
        ) {
            return
        }

        isLoadingMoreAvailableCourses = true
        binding.loadingCoursesBar.visibility = View.VISIBLE

        CourseTools.getCourses(
            limit = AVAILABLE_COURSE_PAGE_SIZE,
            afterDocument = availableCourseCursor
        ) { availablePage, error ->
            error?.let {
                DebugLogger.report(it, "Error retrieving more available courses.")
            }

            availableCourseCursor = availablePage.lastDocument
            hasMoreAvailableCourses = availablePage.hasMore

            val hiddenIDs = hiddenAvailableCourseIDs()
            val existingIDs = primaryNearbyCourses.map { it.id }.toSet()
            val newCourses = availablePage.courses.filter { course ->
                !hiddenIDs.contains(course.id) && !existingIDs.contains(course.id)
            }

            if (newCourses.isNotEmpty()) {
                primaryNearbyCourses = (primaryNearbyCourses + newCourses).sortedWith(::defaultSort)
            }

            isLoadingMoreAvailableCourses = false
            binding.loadingCoursesBar.visibility = View.INVISIBLE
            renderCourseList()
        }
    }

    private fun resetAvailableCoursePagination() {
        availableCourseCursor = null
        hasMoreAvailableCourses = false
        isLoadingMoreAvailableCourses = false
    }

    private fun hiddenAvailableCourseIDs(): Set<String> {
        return GolfApplication.me.ambassadorCourses.toSet() +
                (GolfApplication.me.coursesVisited ?: setOf())
    }

    private fun finishCourseLoading() {
        isLoadingCourses = false
        binding.loadingCoursesBar.visibility = View.INVISIBLE
        binding.courseRefreshLayout.isRefreshing = false
        renderCourseList()

        if (refreshAfterCurrentLoad) {
            refreshAfterCurrentLoad = false
            getCourses()
        }
    }

    private fun queryCourses(query:String) {
        val q = query.lowercase().trim()
        if (allGolfCourses.isEmpty()) {
            binding.loadingCoursesBar.visibility = View.VISIBLE
            Firebase.firestore.collection("courses")
                .orderBy("name")
                .get()
                .addOnCompleteListener { task ->
                    binding.loadingCoursesBar.visibility = View.INVISIBLE
                    if (task.isSuccessful) {
                        allGolfCourses = task.result.documents.mapNotNull { document ->
                            document.data?.let { Course(document.id, it) }
                        }.filterNot { course ->
                            course.name.lowercase() == "test course" && !BuildConfig.DEBUG
                        }.sortedWith(::defaultSort)
                        queryCourses(query)
                    } else {
                        Log.d("COURSE", task.exception?.localizedMessage ?: "unknown search error")
                    }
                }
            return
        }

        val coursesThatMatch: MutableSet<Course> = mutableSetOf()
        for (course in allGolfCourses) {
            if (courseMatchesSearch(course, q)) {
                coursesThatMatch.add(course)
            }
        }

        searchResults = coursesThatMatch.sortedWith(::defaultSort)
        isShowingSearchResults = true
        renderCourseList()
    }

    private fun renderCourseList() {
        val adapterItems = if (isShowingSearchResults) {
            buildSectionItems(
                title = getString(R.string.course_select_section_search),
                courses = searchResults,
                emptyMessage = getString(R.string.course_select_empty_search),
                hideDistance = false
            )
        } else {
            val items = mutableListOf<CourseListItem>()
            if (GolfApplication.me.ambassadorCourses.isNotEmpty()) {
                items += buildSectionItems(
                    title = getString(R.string.course_select_section_ambassador),
                    courses = primaryAmbassadorCourses,
                    emptyMessage = getString(R.string.course_select_empty_ambassador),
                    hideDistance = true
                )
            }
            if (isShowingAvailableFallback) {
                items += visitedSectionItems()
                items += availableOrNearbySectionItems()
            } else {
                items += availableOrNearbySectionItems()
                items += visitedSectionItems()
            }
            items
        }

        courseSelectAdapter.updateItems(adapterItems)
    }

    private fun availableOrNearbySectionItems(): List<CourseListItem> {
        return buildSectionItems(
            title = getString(
                if (isShowingAvailableFallback) {
                    R.string.course_select_section_available
                } else {
                    R.string.course_select_section_nearby
                }
            ),
            courses = primaryNearbyCourses,
            emptyMessage = getString(
                if (isShowingAvailableFallback) {
                    R.string.course_select_empty_available
                } else {
                    R.string.course_select_empty_nearby
                }
            ),
            hideDistance = false
        )
    }

    private fun visitedSectionItems(): List<CourseListItem> {
        return buildSectionItems(
            title = getString(R.string.course_select_section_visited),
            courses = primaryVisitedCourses,
            emptyMessage = getString(R.string.course_select_empty_visited),
            hideDistance = false
        )
    }

    private fun buildSectionItems(
        title: String,
        courses: List<Course>,
        emptyMessage: String,
        hideDistance: Boolean
    ): List<CourseListItem> {
        val items = mutableListOf<CourseListItem>(CourseListItem.SectionHeader(title.uppercase()))
        if (courses.isEmpty()) {
            if (!isLoadingCourses) {
                items += CourseListItem.EmptyRow(emptyMessage.uppercase())
            }
            return items
        }

        courses.forEach { course ->
            items += CourseListItem.CourseRow(
                course = course,
                distanceLabel = if (hideDistance) null else courseDistances[course.id],
                isAmbassador = GolfApplication.me.isAmbassadorOf(course)
            )
        }
        return items
    }

    private fun goToCourse(course: Course) {
        AnalyticsLogger.selectCourse(course)
        GolfApplication.course = course

        context?.apply {
            val intent = Intent(this, PlayGolfActivity::class.java)
            ContextCompat.startActivity(this, intent, null)
        }
    }

    private fun loadNearbyCourses(completion: (NearbyCourseResult) -> Unit) {
        if (!hasLocationPermission) {
            val shouldRequestLocation = !LocationPermissionTools.wasRequested &&
                    !didRequestLocationPermissionThisSession

            if (shouldRequestLocation) {
                didRequestLocationPermissionThisSession = true
                LocationPermissionTools.markRequested()
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                updateLocationPermissionPrompt()
            }
            completion(NearbyCourseResult(courses = listOf(), hasKnownLocation = false))
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    GolfApplication.me.geoPoint = location.geopoint
                    loadCoursesNear(location, completion)
                    return@addOnSuccessListener
                }

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { lastLocation ->
                        if (lastLocation == null) {
                            completion(NearbyCourseResult(courses = listOf(), hasKnownLocation = false))
                            return@addOnSuccessListener
                        }

                        GolfApplication.me.geoPoint = lastLocation.geopoint
                        loadCoursesNear(lastLocation, completion)
                    }
                    .addOnFailureListener {
                        completion(NearbyCourseResult(courses = listOf(), hasKnownLocation = false))
                    }
            }
            .addOnFailureListener {
                DebugLogger.report(it, "Unable to retrieve location for nearby courses.")
                completion(NearbyCourseResult(courses = listOf(), hasKnownLocation = false))
            }
        } catch (_: SecurityException) {
            completion(NearbyCourseResult(courses = listOf(), hasKnownLocation = false))
        }
    }

    @Suppress("DEPRECATION")
    private fun loadCoursesNear(location: Location, completion: (NearbyCourseResult) -> Unit) {
        val appContext = requireContext().applicationContext
        Thread {
            val stateCode = try {
                val adminArea = Geocoder(appContext, Locale.US)
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()
                    ?.adminArea
                stateCode(fromAdminArea = adminArea)
            } catch (exception: Exception) {
                DebugLogger.report(exception, "Error reverse geocoding player location for nearby courses.")
                null
            }

            activity?.runOnUiThread {
                if (stateCode.isNullOrBlank()) {
                    completion(NearbyCourseResult(courses = listOf(), hasKnownLocation = false))
                    return@runOnUiThread
                }

                CourseTools.getCourses(inState = stateCode) { courses, error ->
                    error?.let {
                        DebugLogger.report(it, "Error retrieving nearby state courses.")
                    }
                    completion(NearbyCourseResult(courses = courses, hasKnownLocation = true))
                }
            }
        }.start()
    }

    private fun stateCode(fromAdminArea: String?): String? {
        val normalized = fromAdminArea?.trim()?.lowercase(Locale.US) ?: return null
        if (normalized.length == 2) {
            return normalized.uppercase(Locale.US)
        }
        return STATE_CODES_BY_NAME[normalized]
    }

    private fun courseMatchesSearch(course: Course, query: String): Boolean {
        val name = course.name.lowercase()
        val city = course.city.lowercase()
        val stateCode = course.state.lowercase()
        val stateName = course.fullStateName?.lowercase()

        if (name == "test course") {
            return false
        }

        if (name.contains(query) || city.contains(query) || stateCode.contains(query)) {
            return true
        }

        if (stateName != null && (stateName.contains(query) || query.fuzzyMatch(stateName))) {
            return true
        }

        return query.fuzzyMatch(name) || query.fuzzyMatch(city) || query.fuzzyMatch(stateCode)
    }

    private fun courseDistanceLabels(courses: List<Course>): Map<String, String> {
        val myLocation = GolfApplication.me.geoPoint ?: return mapOf()

        val labels = mutableMapOf<String, String>()
        courses.forEach { course ->
            val courseLocation = course.spectation ?: return@forEach
            val rawDistance = MapTools.distanceFrom(myLocation, courseLocation)
            labels[course.id] = if (GolfApplication.metric) {
                String.format(Locale.US, "%.1f km", rawDistance.toDouble() / 1000)
            } else {
                String.format(Locale.US, "%.1f mi", rawDistance.toDouble() / 1760)
            }
        }
        return labels
    }

    private fun defaultSort(lhs: Course, rhs: Course): Int {
        val lhsIsAmbassador = GolfApplication.me.ambassadorCourses.contains(lhs.id)
        val rhsIsAmbassador = GolfApplication.me.ambassadorCourses.contains(rhs.id)
        if (lhsIsAmbassador != rhsIsAmbassador) {
            return if (lhsIsAmbassador) -1 else 1
        }

        val visitedIDs = GolfApplication.me.coursesVisited ?: setOf()
        val lhsWasVisited = visitedIDs.contains(lhs.id)
        val rhsWasVisited = visitedIDs.contains(rhs.id)
        if (lhsWasVisited != rhsWasVisited) {
            return if (lhsWasVisited) -1 else 1
        }

        GolfApplication.me.geoPoint?.let { myLocation ->
            val lhsDistance = distanceToCourse(lhs, myLocation)
            val rhsDistance = distanceToCourse(rhs, myLocation)
            if (lhsDistance != rhsDistance) {
                return lhsDistance.compareTo(rhsDistance)
            }
        }

        return lhs.name.compareTo(rhs.name)
    }

    private fun distanceToCourse(course: Course, fromGeoPoint: com.google.firebase.firestore.GeoPoint): Int {
        val spectation = course.spectation ?: return Int.MAX_VALUE
        return MapTools.distanceFrom(fromGeoPoint, spectation)
    }

    private fun updateLocationPermissionPrompt() {
        val shouldShowPrompt = !hasLocationPermission && LocationPermissionTools.wasRequested
        _binding?.locationPermissionPrompt?.visibility =
            if (shouldShowPrompt) View.VISIBLE else View.GONE
    }

    companion object {
        private const val AVAILABLE_COURSE_PAGE_SIZE = 25L
        private const val AVAILABLE_LOAD_MORE_THRESHOLD = 6

        private val STATE_CODES_BY_NAME = mapOf(
            "alabama" to "AL",
            "alaska" to "AK",
            "arizona" to "AZ",
            "arkansas" to "AR",
            "california" to "CA",
            "colorado" to "CO",
            "connecticut" to "CT",
            "delaware" to "DE",
            "florida" to "FL",
            "georgia" to "GA",
            "hawaii" to "HI",
            "idaho" to "ID",
            "illinois" to "IL",
            "indiana" to "IN",
            "iowa" to "IA",
            "kansas" to "KS",
            "kentucky" to "KY",
            "louisiana" to "LA",
            "maine" to "ME",
            "maryland" to "MD",
            "massachusetts" to "MA",
            "michigan" to "MI",
            "minnesota" to "MN",
            "mississippi" to "MS",
            "missouri" to "MO",
            "montana" to "MT",
            "nebraska" to "NE",
            "nevada" to "NV",
            "new hampshire" to "NH",
            "new jersey" to "NJ",
            "new mexico" to "NM",
            "new york" to "NY",
            "north carolina" to "NC",
            "north dakota" to "ND",
            "ohio" to "OH",
            "oklahoma" to "OK",
            "oregon" to "OR",
            "pennsylvania" to "PA",
            "rhode island" to "RI",
            "south carolina" to "SC",
            "south dakota" to "SD",
            "tennessee" to "TN",
            "texas" to "TX",
            "utah" to "UT",
            "vermont" to "VT",
            "virginia" to "VA",
            "washington" to "WA",
            "west virginia" to "WV",
            "wisconsin" to "WI",
            "wyoming" to "WY",
            "district of columbia" to "DC",
            "alberta" to "AB",
            "british columbia" to "BC",
            "manitoba" to "MB",
            "new brunswick" to "NB",
            "newfoundland and labrador" to "NL",
            "northwest territories" to "NT",
            "nova scotia" to "NS",
            "nunavut" to "NU",
            "ontario" to "ON",
            "prince edward island" to "PE",
            "quebec" to "QC",
            "saskatchewan" to "SK",
            "yukon" to "YT",
            "dominican republic" to "DR",
            "mexico" to "MX",
            "united kingdom" to "UK"
        )
    }

}
