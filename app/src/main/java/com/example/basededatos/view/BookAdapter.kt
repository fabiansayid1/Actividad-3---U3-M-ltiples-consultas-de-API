package com.example.basededatos.view

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.basededatos.R
import com.example.basededatos.databinding.ItemBookBinding
import com.example.basededatos.databinding.ItemBookHomeBinding
import com.example.basededatos.model.BookItem

class BookAdapter(
    private val horizontalCards: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var books: List<BookItem> = emptyList()

    fun submitList(items: List<BookItem>) {
        books = items
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (horizontalCards) TYPE_HOME else TYPE_GRID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HOME) {
            HomeBookViewHolder(
                ItemBookHomeBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        } else {
            GridBookViewHolder(
                ItemBookBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HomeBookViewHolder -> holder.bind(books[position])
            is GridBookViewHolder -> holder.bind(books[position])
        }
    }

    override fun getItemCount(): Int = books.size

    private class HomeBookViewHolder(
        private val binding: ItemBookHomeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: BookItem) = renderBook(
            book,
            binding.ivPortada,
            binding.tvTitulo,
            binding.tvAutor
        )
    }

    private class GridBookViewHolder(
        private val binding: ItemBookBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: BookItem) = renderBook(
            book,
            binding.ivPortada,
            binding.tvTitulo,
            binding.tvAutor
        )
    }

    private companion object {
        const val TYPE_GRID = 0
        const val TYPE_HOME = 1
    }
}

private fun renderBook(
    book: BookItem,
    cover: ImageView,
    titleView: TextView,
    authorView: TextView
) {
    val info = book.volumeInfo
    val title = info?.title?.takeIf { it.isNotBlank() } ?: "Sin título"
    titleView.text = title
    authorView.text = info?.authors?.takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
        ?: "Autor desconocido"
    cover.contentDescription = "Portada de $title"

    val imageUrl = info?.imageLinks?.thumbnail?.replace("http://", "https://")
    Glide.with(cover)
        .load(imageUrl)
        .placeholder(R.drawable.bg_book_placeholder)
        .error(R.drawable.bg_book_placeholder)
        .centerCrop()
        .into(cover)
}
