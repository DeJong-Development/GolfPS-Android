package com.dejongdevelopment.golfps.adapters

import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.dejongdevelopment.golfps.databinding.CellClubBinding
import com.dejongdevelopment.golfps.models.Club
import com.dejongdevelopment.golfps.tools.ClubTools

class ClubAdapter(
    private val clubs: MutableList<Club>,
    private val onClubUpdated: (Club) -> Unit,
    private val onClubRemoved: (Club) -> Unit
) : RecyclerView.Adapter<ClubAdapter.ClubViewHolder>() {

    class ClubViewHolder(val binding: CellClubBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClubViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ClubViewHolder(CellClubBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ClubViewHolder, position: Int) {
        val club = clubs.getOrNull(position) ?: return

        holder.binding.apply {
            myBagClubName.setText(club.name)
            myBagDistance.setText(club.distance.toString())

            myBagClubName.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    cleanAndSaveClubName(holder.binding, club)
                }
            }

            myBagClubName.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    cleanAndSaveClubName(holder.binding, club)
                    myBagClubName.clearFocus()
                    true
                } else {
                    false
                }
            }

            myBagDistance.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    cleanAndSaveClubDistance(holder.binding, club)
                }
            }

            myBagDistance.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    cleanAndSaveClubDistance(holder.binding, club)
                    myBagDistance.clearFocus()
                    true
                } else {
                    false
                }
            }

            removeClubButton.setOnClickListener {
                onClubRemoved(club)
            }
        }
    }

    override fun getItemCount(): Int = clubs.size

    private fun cleanAndSaveClubName(binding: CellClubBinding, club: Club) {
        val cleanName = ClubTools.cleanClubName(binding.myBagClubName.text?.toString()).trim()
        if (cleanName.isBlank()) {
            binding.myBagClubName.setText(club.name)
            return
        }

        binding.myBagClubName.setText(cleanName)
        if (club.name != cleanName) {
            club.name = cleanName
            onClubUpdated(club)
        }
    }

    private fun cleanAndSaveClubDistance(binding: CellClubBinding, club: Club) {
        val cleanDistance = ClubTools.cleanClubDistance(binding.myBagDistance.text?.toString()).trim()
        val distance = cleanDistance.toIntOrNull()
        if (distance == null) {
            binding.myBagDistance.setText(club.distance.toString())
            return
        }

        binding.myBagDistance.setText(cleanDistance)
        if (club.distance != distance) {
            club.distance = distance
            onClubUpdated(club)
        }
    }
}
