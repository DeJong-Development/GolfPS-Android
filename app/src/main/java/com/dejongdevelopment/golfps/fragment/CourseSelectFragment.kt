package com.dejongdevelopment.golfps.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dejongdevelopment.golfps.BuildConfig
import com.dejongdevelopment.golfps.activity.AddCourseActivity
import com.dejongdevelopment.golfps.activity.PlayGolfActivity
import com.dejongdevelopment.golfps.adapters.CourseSelectAdapter
import com.dejongdevelopment.golfps.databinding.FragmentCourseSelectBinding
import com.dejongdevelopment.golfps.models.Course
import com.dejongdevelopment.golfps.util.fuzzyMatch
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CourseSelectFragment: Fragment() {
    private var _binding: FragmentCourseSelectBinding? = null
    private val binding get() = _binding!!

    private var allGolfCourses:Array<Course> = arrayOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCourseSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getCourses()

        binding.editTextCourseFilter.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                val courseSearchText = s.toString()

                if (courseSearchText.length <= 1) {
                    binding.availableCourseRecyclerView.adapter = CourseSelectAdapter(allGolfCourses)
                    return
                }

                queryCourses(courseSearchText)
            }
        })

        binding.availableCourseRecyclerView.adapter = CourseSelectAdapter(arrayOf())

        binding.addCourseButton.setOnClickListener {
            context?.apply {
                val intent = Intent(this, AddCourseActivity::class.java)
                ContextCompat.startActivity(this, intent, null)
            }
        }
    }

    private fun getCourses() {
        val golfCourses:MutableList<Course> = mutableListOf()

        Firebase.firestore.collection("courses")
            .orderBy("name")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val snapshot = task.result
                    for (document in snapshot.documents) {
                        val course = Course(document.id, document.data)
                        golfCourses.add(course)
                    }
                } else {
                    Log.d("COURSE", task.exception?.localizedMessage ?: "unknown error")
                    return@addOnCompleteListener
                }

                golfCourses.sortBy { it.name }
                this.allGolfCourses = golfCourses.toTypedArray()

                Log.d("COURSE", "found ${allGolfCourses.size} courses")
                binding.availableCourseRecyclerView.adapter = CourseSelectAdapter(this.allGolfCourses)
            }
    }

    private fun queryCourses(query:String) {
        val q = query.lowercase().trim()
        val coursesThatMatch:MutableSet<Course> = mutableSetOf()
        for (course in allGolfCourses) {
            val name = course.name.lowercase()
            val abbrev = course.state.lowercase()
            val state = course.fullStateName?.lowercase()

            if (name == "test course" && !BuildConfig.DEBUG) {
                continue
            } else if (query == "") {
                coursesThatMatch.add(course)
                continue
            }

            if (name.contains(q) || abbrev.contains(q) || q.startsWith(name) || q.startsWith(abbrev)) {
                coursesThatMatch.add(course)
            } else if (state != null && (state.contains(q) || q.startsWith(state.lowercase()) || query.fuzzyMatch(state))) {
                coursesThatMatch.add(course)
            } else if (query.fuzzyMatch(name) || query.fuzzyMatch(abbrev)) {
                coursesThatMatch.add(course)
            }
        }
        val courseArray = coursesThatMatch.toTypedArray()
        courseArray.sortBy { it.name }

        binding.availableCourseRecyclerView.adapter = CourseSelectAdapter(courseArray)
    }

}