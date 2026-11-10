package com.example.basededatos.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.basededatos.databinding.ItemUniversityBinding
import com.example.basededatos.model.University

class UniversityAdapter(
    private val onUniversitySelected: (University) -> Unit
) : RecyclerView.Adapter<UniversityAdapter.UniversityViewHolder>() {

    private var universities: List<University> = emptyList()

    fun submitList(items: List<University>) {
        universities = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UniversityViewHolder {
        val binding =
            ItemUniversityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UniversityViewHolder(binding, onUniversitySelected)
    }

    override fun onBindViewHolder(holder: UniversityViewHolder, position: Int) {
        holder.bind(universities[position])
    }

    override fun getItemCount(): Int = universities.size

    class UniversityViewHolder(
        private val binding: ItemUniversityBinding,
        private val onUniversitySelected: (University) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(university: University) {
            binding.tvNombreUni.text = university.name ?: "Universidad"
            binding.tvPaisUni.text = university.country.orEmpty()
            binding.root.setOnClickListener { onUniversitySelected(university) }
        }
    }
}
