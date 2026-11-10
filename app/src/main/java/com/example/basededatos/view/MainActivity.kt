package com.example.basededatos.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.basededatos.databinding.ActivityMainBinding
import com.example.basededatos.network.UserProfile
import com.example.basededatos.viewmodel.AppViewModelFactory
import com.example.basededatos.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authViewModel: AuthViewModel

    private val registerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.toProfile()?.let(::openHome)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        authViewModel = ViewModelProvider(
            this,
            AppViewModelFactory(application)
        )[AuthViewModel::class.java]

        authViewModel.state.observe(this) { state ->
            binding.progressLogin.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.btnIngresar.isEnabled = !state.isLoading
            state.errorMessage?.let { Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show() }
            state.profile?.let(::openHome)
        }

        binding.btnIngresar.setOnClickListener {
            val email = binding.etCorreo.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            if (email.isBlank() || password.isBlank()) {
                Snackbar.make(binding.root, "Escribe tu correo y contraseña.", Snackbar.LENGTH_LONG).show()
            } else {
                authViewModel.login(email, password)
            }
        }
        binding.tvCrearCuenta.setOnClickListener {
            registerLauncher.launch(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun openHome(profile: UserProfile) {
        startActivity(Intent(this, HomeActivity::class.java).putProfile(profile))
        finish()
    }
}

private fun Intent.toProfile(): UserProfile? {
    val name = getStringExtra(ProfileExtras.NAME) ?: return null
    val university = getStringExtra(ProfileExtras.UNIVERSITY) ?: return null
    val email = getStringExtra(ProfileExtras.EMAIL) ?: return null
    val subjects = getStringArrayListExtra(ProfileExtras.SUBJECTS).orEmpty()
    return UserProfile(name, university, email, subjects)
}

fun Intent.putProfile(profile: UserProfile): Intent = apply {
    putExtra(ProfileExtras.NAME, profile.name)
    putExtra(ProfileExtras.UNIVERSITY, profile.university)
    putExtra(ProfileExtras.EMAIL, profile.email)
    putStringArrayListExtra(ProfileExtras.SUBJECTS, ArrayList(profile.subjects))
}
