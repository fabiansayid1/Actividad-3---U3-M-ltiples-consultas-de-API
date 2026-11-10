package com.example.basededatos.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.basededatos.R
import com.example.basededatos.databinding.ItemSubjectBinding

class SubjectAdapter(
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

    private var subjects: List<String> = emptyList()

    fun submitList(items: List<String>) {
        subjects = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val binding = ItemSubjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubjectViewHolder(binding, onRemove)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.bind(subjects[position], position)
    }

    override fun getItemCount(): Int = subjects.size

    class SubjectViewHolder(
        private val binding: ItemSubjectBinding,
        private val onRemove: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: String, position: Int) {
            val colors = intArrayOf(
                R.color.subject_coral,
                R.color.subject_mint,
                R.color.subject_blue
            )
            val symbols = arrayOf("∑", "✦", "文")
            binding.cardSubject.setCardBackgroundColor(
                ContextCompat.getColor(binding.root.context, colors[position % colors.size])
            )
            binding.tvSubjectIcon.text = symbols[position % symbols.size]
            binding.tvSubjectLabel.text = subject
            binding.btnRemoveSubject.setOnClickListener { onRemove(subject) }
            binding.root.setOnLongClickListener {
                onRemove(subject)
                true
            }
        }
    }
}