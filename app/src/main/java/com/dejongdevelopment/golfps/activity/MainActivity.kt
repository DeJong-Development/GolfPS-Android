package com.dejongdevelopment.golfps.activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.databinding.ActivityMainBinding
import com.dejongdevelopment.golfps.fragment.BagFragment
import com.dejongdevelopment.golfps.fragment.CourseSelectFragment
import com.dejongdevelopment.golfps.fragment.SettingsFragment
import com.dejongdevelopment.golfps.models.Me
import com.dejongdevelopment.golfps.tools.DebugLogger
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private var didSetupTabs = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showSetupLoading()
        auth.currentUser?.let { user ->
            Log.d("LOGIN", "Using existing Firebase user.")
            loadPlayer(user.uid)
            return
        }

        signInAnonymously()
    }

    private fun signInAnonymously() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LOGIN", "signInAnonymously:success")
                    auth.currentUser?.let { user ->
                        loadPlayer(user.uid)
                    } ?: run {
                        DebugLogger.report(null, "Anonymous sign-in succeeded but currentUser was null.")
                        continueOffline()
                    }
                } else {
                    logAuthFailure(task.exception as? Exception)
                    Toast.makeText(
                        baseContext,
                        R.string.app_setup_auth_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                    continueOffline()
                }
            }
    }

    private fun loadPlayer(userId: String) {
        GolfApplication.me = Me(userId)
        GolfApplication.me.docReference?.get()
            ?.addOnSuccessListener { document ->
                document.data?.let { data ->
                    GolfApplication.me = Me(userId, data)
                }
                setupTabs()
            }
            ?.addOnFailureListener { exception ->
                Log.w("LOGIN", "Unable to load player profile.", exception)
                setupTabs()
            }
    }

    private fun continueOffline() {
        GolfApplication.me = Me("offline")
        setupTabs()
    }

    private fun setupTabs() {
        if (didSetupTabs) {
            hideSetupLoading()
            return
        }

        didSetupTabs = true
        binding.pager.adapter = MainPageTabAdapter(this)
        binding.pager.isUserInputEnabled = false

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.pager.currentItem = tab.position
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        hideSetupLoading()
    }

    private fun showSetupLoading() {
        binding.mainContent.visibility = View.GONE
        binding.setupLoadingPanel.visibility = View.VISIBLE
    }

    private fun hideSetupLoading() {
        binding.setupLoadingPanel.visibility = View.GONE
        binding.mainContent.visibility = View.VISIBLE
    }

    private fun logAuthFailure(exception: Exception?) {
        val exceptionName = exception?.javaClass?.simpleName ?: "UnknownException"
        val exceptionMessage = exception?.localizedMessage ?: "No Firebase Auth exception message."
        Log.w("LOGIN", "signInAnonymously:failure [$exceptionName] $exceptionMessage", exception)
        DebugLogger.report(exception, "Anonymous Firebase sign-in failed [$exceptionName]")
    }
}

class MainPageTabAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return CourseSelectFragment()
            1 -> return BagFragment()
            2 -> return SettingsFragment()
        }
        throw Error("invalid number of fragments in view pager")
    }
}
