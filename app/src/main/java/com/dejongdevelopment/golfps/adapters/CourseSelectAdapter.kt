package com.dejongdevelopment.golfps.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.activity.PlayGolfActivity
import com.dejongdevelopment.golfps.databinding.CellCourseBinding
import com.dejongdevelopment.golfps.models.Course
import com.dejongdevelopment.golfps.util.asColorDrawable
import com.dejongdevelopment.golfps.util.asDrawable

class CourseSelectAdapter(
    private var courses: Array<Course>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class CourseViewHolder(val binding: CellCourseBinding) : RecyclerView.ViewHolder(binding.root)

    private lateinit var context: Context

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CourseViewHolder {
        this.context = parent.context
        val inflater = LayoutInflater.from(parent.context)

        return CourseViewHolder(CellCourseBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position > this.courses.size) {
            return
        }
        if (holder !is CourseViewHolder) {
            return
        }

        val course = this.courses[position]

        holder.binding.apply {
            this.cellCourseName.text = course.name
            this.cellCourseState.text = course.state
            this.cellCourseStateIcon.setImageDrawable(course.stateIcon?.asDrawable(context))

            this.root.setOnClickListener {
                GolfApplication.course = course

                val intent = Intent(context, PlayGolfActivity::class.java)
                ContextCompat.startActivity(context, intent, null)
            }
        }
    }

    override fun getItemCount(): Int = courses.size
}