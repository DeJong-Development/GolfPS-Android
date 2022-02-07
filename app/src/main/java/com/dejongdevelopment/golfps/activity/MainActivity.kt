package com.dejongdevelopment.golfps.activity

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.databinding.ActivityMainBinding
import com.dejongdevelopment.golfps.fragment.BagFragment
import com.dejongdevelopment.golfps.fragment.CourseSelectFragment
import com.dejongdevelopment.golfps.fragment.ProfileFragment
import com.dejongdevelopment.golfps.models.Me
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d("LOGIN", "signInAnonymously:success")
                auth.currentUser?.let { user ->
                    GolfApplication.me = Me(user.uid)
                }
            } else {
                // If sign in fails, display a message to the user.
                Log.w("LOGIN", "signInAnonymously:failure", task.exception)
                Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.pager.adapter = MainPageTabAdapter(this)
        binding.pager.isUserInputEnabled = false

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.pager.currentItem = tab.position
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}

class MainPageTabAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return CourseSelectFragment()
            1 -> return BagFragment()
            2 -> return ProfileFragment()
        }
        throw Error("invalid number of fragments in view pager")
    }
}