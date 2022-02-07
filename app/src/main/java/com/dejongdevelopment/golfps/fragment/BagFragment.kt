package com.dejongdevelopment.golfps.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.adapters.ClubAdapter
import com.dejongdevelopment.golfps.adapters.CourseSelectAdapter
import com.dejongdevelopment.golfps.databinding.FragmentBagBinding

class BagFragment: Fragment() {
    private var _binding: FragmentBagBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBagBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val clubs = GolfApplication.me.bag.myClubs.toList()
        binding.golfBagList.apply {
            setHasFixedSize(true)
            setItemViewCacheSize(0)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                .also { it.initialPrefetchItemCount = 0 }
            adapter = ClubAdapter(clubs)
        }
    }
}