package com.dejongdevelopment.golfps.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.databinding.ActivityAddCourseBinding
import com.dejongdevelopment.golfps.util.hideKeyboard
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddCourseActivity : FragmentActivity() {

    private lateinit var binding: ActivityAddCourseBinding

    private var requestTask: Task<DocumentReference>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddCourseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            progressCardView.visibility = View.GONE

            cancelRequestButton.setOnClickListener {
                this@AddCourseActivity.finish()
            }
            submitRequestButton.setOnClickListener {
                val name:String = courseNameEditText.text.toString().trim()
                val city:String = cityEditText.text.toString().trim()
                val state:String = stateEditText.text.toString().trim()
                val country:String = countryEditText.text.toString().trim()

                if (name.isBlank() || city.isBlank() || state.isBlank() || country.isBlank()) {
                    //show an error
                    Toast.makeText(this@AddCourseActivity, "Unable to request course. Please fill in all fields and try again.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                //dismiss keyboard
                hideKeyboard()

                //show loading indicator
                progressCardView.visibility = View.VISIBLE

                //disable the submit button
                submitRequestButton.isEnabled = false

                //disable all text fields
                courseNameEditText.isEnabled = false
                cityEditText.isEnabled = false
                stateEditText.isEnabled = false
                countryEditText.isEnabled = false

                requestTask = Firebase.firestore.collection("course-requests")
                    .add(hashMapOf(
                        "name" to name,
                        "city" to city,
                        "state" to state,
                        "country" to country,
                        "user" to GolfApplication.me.id,
                        "requestDate" to Timestamp.now()
                    ))
                    .addOnCompleteListener { task ->
                        //hide loading indicator
                        progressCardView.visibility = View.GONE

                        if (task.isSuccessful) {
                            Log.d("REQUEST COURSE", "Success!")
                            this@AddCourseActivity.finish()
                        } else {
                            Log.d("REQUEST COURSE", "Error writing course request")

                            //enable the submit button
                            submitRequestButton.isEnabled = true

                            //enable all text fields
                            courseNameEditText.isEnabled = true
                            cityEditText.isEnabled = true
                            stateEditText.isEnabled = true
                            countryEditText.isEnabled = true

                            Toast.makeText(this@AddCourseActivity, "Error requesting course.", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

    }
}