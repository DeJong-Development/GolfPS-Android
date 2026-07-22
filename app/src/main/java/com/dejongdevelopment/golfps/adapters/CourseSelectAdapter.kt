package com.dejongdevelopment.golfps.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dejongdevelopment.golfps.GolfApplication
import com.dejongdevelopment.golfps.databinding.CellCourseBinding
import com.dejongdevelopment.golfps.databinding.CellCourseSectionHeaderBinding
import com.dejongdevelopment.golfps.models.Course
import com.dejongdevelopment.golfps.util.asDrawable

class CourseSelectAdapter(
    private var items: List<CourseListItem>,
    private val onCourseSelected: (Course) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class CourseViewHolder(val binding: CellCourseBinding) : RecyclerView.ViewHolder(binding.root)
    class SectionViewHolder(val binding: CellCourseSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    sealed class CourseListItem {
        data class SectionHeader(val title: String) : CourseListItem()
        data class CourseRow(
            val course: Course,
            val distanceLabel: String?,
            val isAmbassador: Boolean
        ) : CourseListItem()
        data class EmptyRow(val message: String) : CourseListItem()
    }

    fun updateItems(newItems: List<CourseListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION -> SectionViewHolder(
                CellCourseSectionHeaderBinding.inflate(inflater, parent, false)
            )
            else -> CourseViewHolder(CellCourseBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is CourseListItem.SectionHeader -> {
                if (holder is SectionViewHolder) {
                    holder.binding.courseSectionTitle.text = item.title
                }
            }
            is CourseListItem.CourseRow -> {
                if (holder is CourseViewHolder) {
                    bindCourse(holder, item)
                }
            }
            is CourseListItem.EmptyRow -> {
                if (holder is CourseViewHolder) {
                    bindEmpty(holder, item)
                }
            }
        }
    }

    private fun bindCourse(holder: CourseViewHolder, item: CourseListItem.CourseRow) {
        val context = holder.itemView.context
        val course = item.course

        holder.binding.apply {
            cellCourseName.text = course.name
            cellCourseName.setTextColor(context.getColor(com.dejongdevelopment.golfps.R.color.text))
            cellCourseState.text = course.state.uppercase()
            cellCourseStateIcon.setImageDrawable(course.stateIcon?.asDrawable(context))
            cellCourseStateIcon.visibility = if (course.stateIcon == null) View.GONE else View.VISIBLE
            cellCourseDistance.text = item.distanceLabel.orEmpty()
            cellCourseDistance.visibility = if (item.distanceLabel.isNullOrBlank()) View.GONE else View.VISIBLE
            cellCourseAmbassador.visibility = if (item.isAmbassador) View.VISIBLE else View.GONE
            root.isEnabled = true
            root.setOnClickListener {
                GolfApplication.course = course
                onCourseSelected(course)
            }
        }
    }

    private fun bindEmpty(holder: CourseViewHolder, item: CourseListItem.EmptyRow) {
        val context = holder.itemView.context
        holder.binding.apply {
            cellCourseName.text = item.message
            cellCourseName.setTextColor(context.getColor(com.dejongdevelopment.golfps.R.color.danger))
            cellCourseState.text = ""
            cellCourseStateIcon.setImageDrawable(null)
            cellCourseStateIcon.visibility = View.GONE
            cellCourseDistance.visibility = View.GONE
            cellCourseAmbassador.visibility = View.GONE
            root.isEnabled = false
            root.setOnClickListener(null)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CourseListItem.SectionHeader -> VIEW_TYPE_SECTION
            else -> VIEW_TYPE_COURSE
        }
    }

    override fun getItemCount(): Int = items.size

    companion object {
        private const val VIEW_TYPE_COURSE = 0
        private const val VIEW_TYPE_SECTION = 1
    }
}
