package com.gymleague.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.R
import com.gymleague.app.models.Exercise

class ExerciseAdapter(
    private var exercises: List<Exercise> = emptyList()
) : RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder>() {

    class ExerciseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val exerciseName: TextView = itemView.findViewById(R.id.tvExerciseName)
        val muscleGroup: TextView = itemView.findViewById(R.id.tvMuscleGroup)
        val description: TextView = itemView.findViewById(R.id.tvExerciseDescription)
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
            exercise.description ?: "No description available"
    }

    override fun getItemCount(): Int {
        return exercises.size
    }

    fun updateExercises(newExercises: List<Exercise>) {
        exercises = newExercises
        notifyDataSetChanged()
    }
}