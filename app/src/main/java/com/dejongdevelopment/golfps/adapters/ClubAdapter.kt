package com.dejongdevelopment.golfps.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dejongdevelopment.golfps.databinding.CellClubBinding
import com.dejongdevelopment.golfps.models.Club

class ClubAdapter(
    private var clubs: List<Club>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ClubViewHolder(val binding: CellClubBinding) : RecyclerView.ViewHolder(binding.root)

    private lateinit var context: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClubViewHolder {
        this.context = parent.context
        val inflater = LayoutInflater.from(parent.context)

        return ClubViewHolder(CellClubBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position > this.clubs.size) {
            return
        }
        if (holder !is ClubViewHolder) {
            return
        }

        val club = this.clubs[position]

        holder.binding.apply {
            this.myBagClubName.setText(club.name)
            this.myBagDistance.setText(club.distance.toString())

            myBagClubName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    club.name = s.toString()
                }
            })

            myBagDistance.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    s.toString().toIntOrNull()?.let {
                        club.distance = it
                    }
                }
            })
        }
    }

    override fun getItemCount(): Int = clubs.size
}