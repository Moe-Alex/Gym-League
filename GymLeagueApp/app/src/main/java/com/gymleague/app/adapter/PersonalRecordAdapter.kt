package com.gymleague.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.R
import com.gymleague.app.models.PersonalRecord
import com.gymleague.app.util.OneRepMaxUtils

class PersonalRecordAdapter(
    private var records: List<PersonalRecord> = emptyList()
) : RecyclerView.Adapter<PersonalRecordAdapter.PersonalRecordViewHolder>() {

    class PersonalRecordViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val badge: TextView =
            itemView.findViewById(R.id.tvPrBadge)

        val exerciseName: TextView =
            itemView.findViewById(R.id.tvPrExerciseName)

        val oneRepMax: TextView =
            itemView.findViewById(R.id.tvPrOneRepMax)

        val bestSet: TextView =
            itemView.findViewById(R.id.tvPrBestSet)

        val dateAchieved: TextView =
            itemView.findViewById(R.id.tvPrDate)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PersonalRecordViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_personal_record, parent, false)

        return PersonalRecordViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PersonalRecordViewHolder,
        position: Int
    ) {
        val record = records[position]

        holder.badge.text =
            record.exerciseName
                .trim()
                .take(1)
                .uppercase()
                .ifEmpty { "?" }

        holder.exerciseName.text = record.exerciseName

        holder.oneRepMax.text =
            OneRepMaxUtils.formatOneRepMax(record.estimatedOneRepMax)

        holder.bestSet.text =
            holder.itemView.context.getString(R.string.record_best_set, OneRepMaxUtils.formatWeight(record.heaviestWeight), record.repsAtHeaviestWeight)

        holder.dateAchieved.text =
            holder.itemView.context.getString(R.string.record_achieved, record.dateAchieved.take(10))
    }

    override fun getItemCount(): Int {
        return records.size
    }

    fun updateRecords(newRecords: List<PersonalRecord>) {
        records = newRecords
        notifyDataSetChanged()
    }
}