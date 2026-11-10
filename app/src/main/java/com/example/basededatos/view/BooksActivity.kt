package com.example.basededatos.view

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.basededatos.databinding.ActivityBooksBinding
import com.example.basededatos.viewmodel.AppViewModelFactory
import com.example.basededatos.viewmodel.BooksViewModel
import com.google.android.material.snackbar.Snackbar

class BooksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBooksBinding
    private lateinit var booksViewModel: BooksViewModel
    private val bookAdapter by lazy { BookAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolver.setOnClickListener { finish() }

        booksViewModel = ViewModelProvider(
            this,
            AppViewModelFactory(application)
        )[BooksViewModel::class.java]
        binding.rvBooks.layoutManager = GridLayoutManager(this, 2)
        binding.rvBooks.adapter = bookAdapter

        booksViewModel.state.observe(this) { state ->
            bookAdapter.submitList(state.books)
            binding.progressBooks.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.rvBooks.visibility =
                if (!state.isLoading && state.books.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvListaVacia.visibility =
                if (!state.isLoading && state.isEmpty) View.VISIBLE else View.GONE
            state.errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }

        val subjects = intent.getStringArrayListExtra(ProfileExtras.SUBJECTS).orEmpty()
        binding.tvMateriasBuscadas.text = if (subjects.isEmpty()) {
            "Tus materias aparecerán aquí"
        } else {
            "Libros relacionados con: " + subjects.joinToString(" · ")
        }
        booksViewModel.loadSubjects(subjects)
    }
}
