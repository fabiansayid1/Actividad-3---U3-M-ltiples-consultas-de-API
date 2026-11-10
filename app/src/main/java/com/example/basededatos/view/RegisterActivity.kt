package com.example.basededatos.view

import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basededatos.databinding.ActivityRegisterBinding
import com.example.basededatos.model.University
import com.example.basededatos.network.UserProfile
import com.example.basededatos.viewmodel.AppViewModelFactory
import com.example.basededatos.viewmodel.AuthViewModel
import com.example.basededatos.viewmodel.UniversitySearchViewModel
import com.google.android.material.snackbar.Snackbar

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authViewModel: AuthViewModel
    private lateinit var universitySearchViewModel: UniversitySearchViewModel
    private var selectedUniversity: University? = null
    private var settingUniversity = false
    private var lastUniversityError: String? = null
    private val universityAdapter by lazy { UniversityAdapter(::selectUniversity) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val factory = AppViewModelFactory(application)
        authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]
        universitySearchViewModel =
            ViewModelProvider(this, factory)[UniversitySearchViewModel::class.java]

        binding.rvUniversities.layoutManager = LinearLayoutManager(this)
        binding.rvUniversities.adapter = universityAdapter
        binding.rvUniversities.isNestedScrollingEnabled = false

        universitySearchViewModel.state.observe(this) { state ->
            universityAdapter.submitList(state.universities)
            binding.progressUniversities.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.rvUniversities.visibility =
                if (state.universities.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvSinUniversidades.visibility =
                if (!state.isLoading && state.universities.isEmpty() &&
                    (binding.etUniversidad.text?.length ?: 0) >= 2
                ) View.VISIBLE else View.GONE
            state.errorMessage?.let {
                if (it != lastUniversityError) {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    lastUniversityError = it
                }
            }
            if (state.errorMessage == null && !state.isLoading) lastUniversityError = null
        }

        authViewModel.state.observe(this) { state ->
            binding.progressRegistro.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.btnRegistrar.isEnabled = !state.isLoading
            state.errorMessage?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
            state.profile?.let { profile ->
                setResult(RESULT_OK, android.content.Intent().putProfile(profile))
                finish()
            }
        }

        binding.etUniversidad.doAfterTextChanged { text ->
            if (!settingUniversity) {
                selectedUniversity = null
                universitySearchViewModel.search(text?.toString().orEmpty())
            }
        }
        binding.btnRegistrar.setOnClickListener { submitRegistration() }
        binding.tvYaTengoCuenta.setOnClickListener { finish() }
    }

    private fun selectUniversity(university: University) {
        selectedUniversity = university
        val name = university.name.orEmpty()
        val country = university.country.orEmpty()
        settingUniversity = true
        binding.etUniversidad.setText(if (country.isBlank()) name else "$name, $country")
        binding.etUniversidad.setSelection(binding.etUniversidad.text?.length ?: 0)
        settingUniversity = false
        binding.rvUniversities.visibility = View.GONE
        binding.tvSinUniversidades.visibility = View.GONE
    }

    private fun submitRegistration() {
        val name = binding.etNombre.text?.toString()?.trim().orEmpty()
        val email = binding.etCorreo.text?.toString()?.trim()?.lowercase().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        if (name.isBlank()) {
            binding.etNombre.error = "Escribe tu nombre."
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etCorreo.error = "Escribe un correo válido."
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Usa al menos 6 caracteres."
            return
        }
        val university = selectedUniversity
        if (university == null) {
            binding.etUniversidad.error = "Elige una universidad de los resultados."
            return
        }
        val universityName = listOfNotNull(university.name, university.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
        authViewModel.register(
            UserProfile(name, universityName, email, emptyList()),
            password
        )
    }
}
