package com.dejongdevelopment.golfps.tools

import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.models.Course
import com.dejongdevelopment.golfps.models.Hole
import com.dejongdevelopment.golfps.util.toMeters
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object CourseTools {
    data class CoursePage(
        val courses: List<Course>,
        val lastDocument: DocumentSnapshot?,
        val hasMore: Boolean
    )

    fun getAvailableStates(completion: (List<String>, Exception?) -> Unit) {
        Firebase.firestore.collection("courses")
            .orderBy("state")
            .get()
            .addOnSuccessListener { snapshot ->
                val states = snapshot.documents.mapNotNull { document ->
                    (document.data?.get("state") as? String)
                        ?.trim()
                        ?.uppercase()
                        ?.takeIf { it.isNotEmpty() }
                }.toSet().sorted()
                completion(states, null)
            }
            .addOnFailureListener { completion(listOf(), it) }
    }

    fun getCourses(withIDs: List<String>, completion: (List<Course>, Exception?) -> Unit) {
        val uniqueIds = withIDs.toSet().filter { it.isNotBlank() }
        if (uniqueIds.isEmpty()) {
            completion(listOf(), null)
            return
        }

        val courses = mutableListOf<Course>()
        var remaining = uniqueIds.count()
        var firstError: Exception? = null

        uniqueIds.forEach { id ->
            Firebase.firestore.collection("courses").document(id)
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.data?.let { data ->
                        courses.add(Course(snapshot.id, data))
                    }
                    remaining -= 1
                    if (remaining == 0) {
                        completion(courses, firstError)
                    }
                }
                .addOnFailureListener { exception ->
                    if (firstError == null) {
                        firstError = exception
                    }
                    remaining -= 1
                    if (remaining == 0) {
                        completion(courses, firstError)
                    }
                }
        }
    }

    fun getCourses(
        limit: Long,
        afterDocument: DocumentSnapshot? = null,
        completion: (CoursePage, Exception?) -> Unit
    ) {
        var query: Query = Firebase.firestore.collection("courses")
            .orderBy("name")
            .limit(limit)

        afterDocument?.let {
            query = query.startAfter(it)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val courses = snapshot.documents.mapNotNull { document ->
                    document.data?.let { Course(document.id, it) }
                }
                completion(
                    CoursePage(
                        courses = courses,
                        lastDocument = snapshot.documents.lastOrNull(),
                        hasMore = snapshot.documents.size.toLong() == limit
                    ),
                    null
                )
            }
            .addOnFailureListener {
                completion(CoursePage(listOf(), afterDocument, false), it)
            }
    }

    fun getCourses(inState: String, completion: (List<Course>, Exception?) -> Unit) {
        val trimmedState = inState.trim().uppercase()
        if (trimmedState.isEmpty()) {
            completion(listOf(), null)
            return
        }

        Firebase.firestore.collection("courses")
            .whereEqualTo("state", trimmedState)
            .orderBy("name")
            .get()
            .addOnSuccessListener { snapshot ->
                val courses = snapshot.documents.mapNotNull { document ->
                    document.data?.let { Course(document.id, it) }
                }
                completion(courses, null)
            }
            .addOnFailureListener { completion(listOf(), it) }
    }

    fun updateHoleInfo(forCourse: Course, completion: (Boolean, Exception?) -> Unit) {
        forCourse.addHoles(completion)
    }

    fun getLongestDrives(forHole: Hole, completion: (Boolean, Exception?) -> Unit) {
        val holeDocRef = forHole.docReference
        if (holeDocRef == null) {
            completion(false, null)
            return
        }

        holeDocRef.collection("drives")
            .orderBy("distance", Query.Direction.DESCENDING)
            .limit(3)
            .get()
            .addOnSuccessListener { snapshot ->
                forHole.longestDrives.clear()
                for (driveDoc in snapshot.documents) {
                    val driveUser = driveDoc.id
                    val driveData = driveDoc.data ?: continue

                    (driveData["distance"] as? Number)?.let { driveDistance ->
                        if (driveUser == GolfApplication.me.id) {
                            forHole.myLongestDriveInYards = driveDistance.toInt()
                            forHole.myLongestDriveInMeters = driveDistance.toInt().toMeters()
                        }
                    }

                    (driveData["location"] as? GeoPoint)?.let { driveLocation ->
                        forHole.longestDrives[driveUser] = driveLocation
                    }
                }
                completion(true, null)
            }
            .addOnFailureListener {
                DebugLogger.report(it, "Error adding long drive")
                completion(false, it)
            }
    }
}
