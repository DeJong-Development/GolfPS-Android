package com.dejongdevelopment.golfps.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.databinding.ActivityAddClubBinding
import com.dejongdevelopment.golfps.tools.ClubTools
import com.dejongdevelopment.golfps.util.hideKeyboard

class AddClubActivity : FragmentActivity() {
    private lateinit var binding: ActivityAddClubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddClubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addClubCancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.addClubSubmitButton.setOnClickListener {
            addNewClub()
        }

        binding.clubDistanceEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addNewClub()
                true
            } else {
                false
            }
        }
    }

    private fun addNewClub() {
        val clubName = ClubTools.cleanClubName(binding.clubNameEditText.text?.toString())
            .trim()
        val clubDistanceString = ClubTools.cleanClubDistance(binding.clubDistanceEditText.text?.toString())
            .trim()
        val clubDistance = clubDistanceString.toIntOrNull()

        binding.clubNameEditText.setText(clubName)
        binding.clubDistanceEditText.setText(clubDistanceString)

        if (clubName.isBlank() || clubDistance == null) {
            Toast.makeText(this, R.string.add_club_invalid, Toast.LENGTH_LONG).show()
            return
        }

        hideKeyboard()

        val resultIntent = Intent()
            .putExtra(EXTRA_CLUB_NAME, clubName)
            .putExtra(EXTRA_CLUB_DISTANCE, clubDistance)

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_CLUB_NAME = "club_name"
        const val EXTRA_CLUB_DISTANCE = "club_distance"
    }
}
