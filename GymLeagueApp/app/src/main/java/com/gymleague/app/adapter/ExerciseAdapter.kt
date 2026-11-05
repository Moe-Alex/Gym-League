package com.gymleague.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.R
import com.gymleague.app.models.Exercise

class ExerciseAdapter(
    private var exercises: List<Exercise> = emptyList(),
    private val currentUserId: Int,
    private val onExerciseClick: (Exercise) -> Unit,
    private val onDeleteClick: (Exercise) -> Unit
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val exerciseName: TextView =
            itemView.findViewById(R.id.tvExerciseName)

        val muscleGroup: TextView =
            itemView.findViewById(R.id.tvMuscleGroup)

        val description: TextView =
            itemView.findViewById(R.id.tvExerciseDescription)

        val deleteButton: ImageButton =
            itemView.findViewById(R.id.btnDeleteExercise)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExerciseViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_exercise, parent, false)

        return ExerciseViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ExerciseViewHolder,
        position: Int
    ) {
        val exercise = exercises[position]

        holder.exerciseName.text = exercise.exerciseName
        holder.muscleGroup.text = exercise.muscleGroup
        holder.description.text =
            exercise.description ?: holder.itemView.context.getString(R.string.workout_no_description)

        holder.itemView.setOnClickListener {
            onExerciseClick(exercise)
        }

        // Only the person who created a custom exercise can delete it.
        // Base/seeded exercises (createdByUserId == null) are never deletable.
        val canDelete = exercise.createdByUserId != null &&
                exercise.createdByUserId == currentUserId

        if (canDelete) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener {
                onDeleteClick(exercise)
            }
        } else {
            holder.deleteButton.visibility = View.GONE
            holder.deleteButton.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int {
        return exercises.size
    }

    fun updateExercises(newExercises: List<Exercise>) {
        exercises = newExercises
        notifyDataSetChanged()
    }
}