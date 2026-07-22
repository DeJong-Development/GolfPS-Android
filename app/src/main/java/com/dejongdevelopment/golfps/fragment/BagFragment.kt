package com.dejongdevelopment.golfps.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.R
import com.dejongdevelopment.golfps.activity.AddClubActivity
import com.dejongdevelopment.golfps.adapters.ClubAdapter
import com.dejongdevelopment.golfps.databinding.FragmentBagBinding
import com.dejongdevelopment.golfps.models.BagType
import com.dejongdevelopment.golfps.models.Club
import com.dejongdevelopment.golfps.tools.AnalyticsLogger

class BagFragment: Fragment() {
    private var _binding: FragmentBagBinding? = null
    private val binding get() = _binding!!

    private val addClubLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        val data = result.data ?: return@registerForActivityResult
        val clubName = data.getStringExtra(AddClubActivity.EXTRA_CLUB_NAME) ?: return@registerForActivityResult
        val clubDistance = data.getIntExtra(AddClubActivity.EXTRA_CLUB_DISTANCE, -1)
        if (clubDistance <= 0) {
            return@registerForActivityResult
        }

        GolfApplication.me.bag.addClub(Club(clubName, clubDistance))
        GolfApplication.me.didCustomizeBag = true
        AnalyticsLogger.log("add_club")
        refreshBagList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        activity?.currentFocus?.clearFocus()
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.golfBagList.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(0)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                .also { it.initialPrefetchItemCount = 0 }
        }

        binding.addClubButton.setOnClickListener {
            AnalyticsLogger.log("click_add_club")
            launchAddClub()
        }

        binding.proBagButton.setOnClickListener { populateDefaultBag(BagType.PRO) }
        binding.longBagButton.setOnClickListener { populateDefaultBag(BagType.LONG) }
        binding.averageBagButton.setOnClickListener { populateDefaultBag(BagType.AVERAGE) }
        binding.customBagButton.setOnClickListener {
            AnalyticsLogger.log("click_custom_bag")
            launchAddClub()
        }

        refreshBagList()
    }

    private fun refreshBagList() {
        val clubs = GolfApplication.me.bag.myClubs
        val isBagEmpty = clubs.isEmpty()

        if (isBagEmpty) {
            binding.addClubButton.visibility = View.GONE
            binding.golfBagExplanation.visibility = View.GONE
            binding.emptyBagLabel.visibility = View.VISIBLE
            binding.pickBagView.visibility = View.VISIBLE
            binding.golfBagList.visibility = View.GONE
        } else {
            binding.addClubButton.visibility = View.VISIBLE
            binding.golfBagExplanation.visibility = View.VISIBLE
            binding.emptyBagLabel.visibility = View.GONE
            binding.pickBagView.visibility = View.GONE
            binding.golfBagList.visibility = View.VISIBLE
        }

        binding.golfBagList.adapter = ClubAdapter(
            clubs = clubs,
            onClubUpdated = {
                GolfApplication.me.bag.sortClubs()
                GolfApplication.me.didCustomizeBag = true
                AnalyticsLogger.log("edit_club")
                refreshBagList()
            },
            onClubRemoved = { club ->
                GolfApplication.me.bag.removeClubFromBag(club)
                GolfApplication.me.didCustomizeBag = true
                AnalyticsLogger.log("remove_club")
                refreshBagList()
            }
        )
    }

    private fun populateDefaultBag(bagType: BagType) {
        GolfApplication.me.bag.populateBag(bagType)
        AnalyticsLogger.setDefaultBag(bagType)
        AnalyticsLogger.log("select_default_bag", mapOf("bag" to bagType.rawValue))
        refreshBagList()
    }

    private fun launchAddClub() {
        val intent = Intent(requireContext(), AddClubActivity::class.java)
        addClubLauncher.launch(intent)
    }
}
