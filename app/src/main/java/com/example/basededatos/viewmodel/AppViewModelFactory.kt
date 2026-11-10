package com.example.basededatos.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.basededatos.BuildConfig
import com.example.basededatos.network.RetrofitClient
import com.example.basededatos.network.UserStore

class AppViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(UserStore(application)) as T
            modelClass.isAssignableFrom(UniversitySearchViewModel::class.java) ->
                UniversitySearchViewModel(RetrofitClient.universityApi) as T
            modelClass.isAssignableFrom(BooksViewModel::class.java) ->
                BooksViewModel(RetrofitClient.booksApi, BuildConfig.GOOGLE_BOOKS_API_KEY) as T
            modelClass.isAssignableFrom(StudyViewModel::class.java) ->
                StudyViewModel(UserStore(application)) as T
            else -> throw IllegalArgumentException("ViewModel no soportado: " + modelClass.name)
        }
    }
}
