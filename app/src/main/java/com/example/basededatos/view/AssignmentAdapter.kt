package com.example.basededatos.view

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.basededatos.R
import com.example.basededatos.databinding.ItemAssignmentBinding
import com.example.basededatos.network.AssignmentRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssignmentAdapter(
    private val onCompletionChanged: (AssignmentRecord, Boolean) -> Unit,
    private val onDelete: (AssignmentRecord) -> Unit
) : RecyclerView.Adapter<AssignmentAdapter.AssignmentViewHolder>() {

    private var assignments: List<AssignmentRecord> = emptyList()

    fun submitList(items: List<AssignmentRecord>) {
        assignments = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssignmentViewHolder {
        val binding = ItemAssignmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AssignmentViewHolder(binding, onCompletionChanged, onDelete)
    }

    override fun onBindViewHolder(holder: AssignmentViewHolder, position: Int) {
        holder.bind(assignments[position])
    }

    override fun getItemCount(): Int = assignments.size

    class AssignmentViewHolder(
        private val binding: ItemAssignmentBinding,
        private val onCompletionChanged: (AssignmentRecord, Boolean) -> Unit,
        private val onDelete: (AssignmentRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(assignment: AssignmentRecord) {
            binding.tvTituloAsignacion.text = assignment.title
            binding.tvMateriaAsignacion.text = assignment.subject
            binding.cbCompletada.setOnCheckedChangeListener(null)
            binding.cbCompletada.isChecked = assignment.isCompleted
            binding.tvTituloAsignacion.paintFlags =
                if (assignment.isCompleted) {
                    binding.tvTituloAsignacion.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    binding.tvTituloAsignacion.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

            binding.tvFechaLimite.text = if (assignment.isCompleted) {
                "Completada"
            } else {
                "Entrega: " + SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                    .format(Date(assignment.dueAt))
            }
            val dateColor = if (!assignment.isCompleted && assignment.dueAt < System.currentTimeMillis()) {
                R.color.deadline_overdue
            } else {
                R.color.muted
            }
            binding.tvFechaLimite.setTextColor(
                ContextCompat.getColor(binding.root.context, dateColor)
            )

            binding.cbCompletada.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != assignment.isCompleted) {
                    onCompletionChanged(assignment, isChecked)
                }
            }
            binding.btnEliminarAsignacion.setOnClickListener { onDelete(assignment) }
        }
    }
}
