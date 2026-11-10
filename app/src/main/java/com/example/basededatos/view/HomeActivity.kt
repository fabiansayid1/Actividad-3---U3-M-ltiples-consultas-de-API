package com.example.basededatos.view

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.basededatos.databinding.ActivityHomeBinding
import com.example.basededatos.databinding.DialogAddAssignmentBinding
import com.example.basededatos.databinding.DialogAddSubjectBinding
import com.example.basededatos.databinding.DialogDeleteAccountBinding
import com.example.basededatos.network.AssignmentRecord
import com.example.basededatos.viewmodel.AppViewModelFactory
import com.example.basededatos.viewmodel.BooksViewModel
import com.example.basededatos.viewmodel.StudyViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var booksViewModel: BooksViewModel
    private lateinit var studyViewModel: StudyViewModel
    private val bookAdapter by lazy { BookAdapter(horizontalCards = true) }
    private val subjectAdapter by lazy { SubjectAdapter(::confirmRemoveSubject) }
    private val assignmentAdapter by lazy {
        AssignmentAdapter(
            onCompletionChanged = { assignment, completed ->
                studyViewModel.setAssignmentCompleted(assignment, completed)
            },
            onDelete = ::confirmDeleteAssignment
        )
    }

    private var subjects: List<String> = emptyList()
    private var userName: String = ""
    private var university: String = ""
    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userName = intent.getStringExtra(ProfileExtras.NAME).orEmpty()
        university = intent.getStringExtra(ProfileExtras.UNIVERSITY).orEmpty()
        email = intent.getStringExtra(ProfileExtras.EMAIL).orEmpty()
        subjects = intent.getStringArrayListExtra(ProfileExtras.SUBJECTS).orEmpty()

        binding.tvBienvenida.text = if (userName.isBlank()) "Hola, estudiante" else "Hola, $userName"
        binding.tvInicialAvatar.text = userName.firstOrNull()?.uppercase() ?: "S"
        binding.tvUniversidad.text = university.ifBlank { "Universidad no especificada" }
        binding.tvCorreo.text = email

        val factory = AppViewModelFactory(application)
        studyViewModel = ViewModelProvider(this, factory)[StudyViewModel::class.java]
        booksViewModel = ViewModelProvider(this, factory)[BooksViewModel::class.java]

        binding.rvSubjects.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSubjects.adapter = subjectAdapter
        binding.rvHomeBooks.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvHomeBooks.adapter = bookAdapter
        binding.rvAssignments.layoutManager = LinearLayoutManager(this)
        binding.rvAssignments.adapter = assignmentAdapter
        binding.rvAssignments.isNestedScrollingEnabled = false

        subjectAdapter.submitList(subjects)
        renderSubjects()
        booksViewModel.state.observe(this) { state ->
            bookAdapter.submitList(state.books.take(HOME_BOOK_LIMIT))
            binding.progressBooksHome.visibility =
                if (state.isLoading) View.VISIBLE else View.GONE
            binding.rvHomeBooks.visibility =
                if (!state.isLoading && state.books.isNotEmpty()) View.VISIBLE else View.GONE
            binding.tvLibrosVacio.visibility =
                if (!state.isLoading && state.isEmpty) View.VISIBLE else View.GONE
            binding.tvLibrosVacio.text = if (subjects.isEmpty()) {
                "Agrega una materia desde Inicio para recibir recomendaciones."
            } else {
                "No encontramos libros para estas materias."
            }
            if (state.errorMessage != null) {
                Snackbar.make(binding.root, state.errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }
        booksViewModel.loadSubjects(subjects)

        binding.tvVerTodo.setOnClickListener { showAddSubjectDialog() }
        binding.btnAddSubject.setOnClickListener { showAddSubjectDialog() }
        binding.tvNuevaAsignacion.setOnClickListener { showAddAssignmentDialog() }
        binding.tvMenu.setOnClickListener { showHomeMenu(it) }
        binding.tvVerBiblioteca.setOnClickListener { openLibrary() }
        binding.cardLibros.setOnClickListener { openLibrary() }
        binding.tvNavLibros.setOnClickListener { openLibrary() }
        binding.tvNavMaterias.setOnClickListener {
            binding.scrollHome.smoothScrollTo(0, binding.tvNumeroMaterias.top)
        }
        binding.tvNavPerfil.setOnClickListener { showProfileDialog() }

        studyViewModel.loadForUser(email)
        studyViewModel.state.observe(this) { state ->
            if (!state.isLoading) {
                subjects = state.subjects
                subjectAdapter.submitList(subjects)
                assignmentAdapter.submitList(state.assignments)
                renderSubjects()
                binding.tvPendingCount.text = state.assignments.count { !it.isCompleted }.toString()
                binding.rvAssignments.visibility =
                    if (state.assignments.isEmpty()) View.GONE else View.VISIBLE
                binding.tvNoAssignments.visibility =
                    if (state.assignments.isEmpty()) View.VISIBLE else View.GONE
                booksViewModel.loadSubjects(subjects)
            }
            state.message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                studyViewModel.clearMessage()
            }
            if (state.accountDeleted) {
                startActivity(
                    Intent(this, MainActivity::class.java).addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                )
                finish()
            }
        }
    }

    private fun renderSubjects() {
        binding.tvNumeroMaterias.text = subjects.size.toString()
        binding.tvMaterias.text = if (subjects.isEmpty()) {
            "Agrega materias para recibir recomendaciones personalizadas."
        } else {
            subjects.joinToString(" · ")
        }
        binding.rvSubjects.visibility = if (subjects.isEmpty()) View.GONE else View.VISIBLE
        binding.tvNoSubjects.visibility = if (subjects.isEmpty()) View.VISIBLE else View.GONE
        binding.btnAddSubject.visibility = if (subjects.isEmpty()) View.VISIBLE else View.GONE
        binding.tvHintSubjects.visibility = if (subjects.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAddSubjectDialog() {
        val dialogBinding = DialogAddSubjectBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Agregar materia")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Agregar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val subject = dialogBinding.etNuevaMateria.text?.toString()?.trim().orEmpty()
                if (subject.isBlank()) {
                    dialogBinding.etNuevaMateria.error = "Escribe el nombre de la materia."
                } else {
                    studyViewModel.addSubject(subject)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun confirmRemoveSubject(subject: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Quitar materia")
            .setMessage("Se eliminarán también las asignaciones de $subject.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Quitar") { _, _ -> studyViewModel.removeSubject(subject) }
            .show()
    }

    private fun showAddAssignmentDialog() {
        if (subjects.isEmpty()) {
            Snackbar.make(binding.root, "Primero agrega una materia.", Snackbar.LENGTH_LONG).show()
            showAddSubjectDialog()
            return
        }

        val dialogBinding = DialogAddAssignmentBinding.inflate(layoutInflater)
        val subjectInput = dialogBinding.actvMateriaAsignacion
        subjectInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, subjects))
        subjectInput.threshold = 0
        subjectInput.setText("", false)
        subjectInput.setOnClickListener { subjectInput.showDropDown() }
        subjectInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) subjectInput.post { subjectInput.showDropDown() }
        }

        val dueDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var dueAt = dueDate.timeInMillis
        dialogBinding.etFechaLimite.setText(formatDate(dueAt))
        dialogBinding.etFechaLimite.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dueDate.set(Calendar.YEAR, year)
                    dueDate.set(Calendar.MONTH, month)
                    dueDate.set(Calendar.DAY_OF_MONTH, day)
                    dueDate.set(Calendar.HOUR_OF_DAY, 23)
                    dueDate.set(Calendar.MINUTE, 59)
                    dueDate.set(Calendar.SECOND, 0)
                    dueDate.set(Calendar.MILLISECOND, 0)
                    dueAt = dueDate.timeInMillis
                    dialogBinding.etFechaLimite.setText(formatDate(dueAt))
                },
                dueDate.get(Calendar.YEAR),
                dueDate.get(Calendar.MONTH),
                dueDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Nueva asignación")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val subject = dialogBinding.actvMateriaAsignacion.text?.toString()?.trim().orEmpty()
                val title = dialogBinding.etTituloAsignacion.text?.toString()?.trim().orEmpty()
                if (subject !in subjects) {
                    dialogBinding.actvMateriaAsignacion.error = "Selecciona una materia."
                } else if (title.isBlank()) {
                    dialogBinding.etTituloAsignacion.error = "Escribe el nombre de la asignación."
                } else {
                    studyViewModel.addAssignment(subject, title, dueAt)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteAssignment(assignment: AssignmentRecord) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar asignación")
            .setMessage("¿Quieres eliminar " + assignment.title + "?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                studyViewModel.deleteAssignment(assignment)
            }
            .show()
    }

    private fun showHomeMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Agregar materia").setOnMenuItemClickListener {
                showAddSubjectDialog()
                true
            }
            menu.add("Nueva asignación").setOnMenuItemClickListener {
                showAddAssignmentDialog()
                true
            }
            menu.add("Mi perfil").setOnMenuItemClickListener {
                showProfileDialog()
                true
            }
            menu.add("Eliminar cuenta").setOnMenuItemClickListener {
                confirmAccountDeletion()
                true
            }
            menu.add("Cerrar sesión").setOnMenuItemClickListener {
                confirmSignOut()
                true
            }
            show()
        }
    }

    private fun showProfileDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(userName.ifBlank { "Mi perfil" })
            .setMessage(university + "\n" + email)
            .setNegativeButton("Eliminar cuenta") { _, _ -> confirmAccountDeletion() }
            .setPositiveButton("Cerrar sesión") { _, _ -> confirmSignOut() }
            .show()
    }

    private fun confirmSignOut() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cerrar sesión")
            .setMessage("Volverás al inicio de sesión. Tu cuenta y tus datos se conservarán.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Cerrar sesión") { _, _ -> returnToLogin() }
            .show()
    }

    private fun returnToLogin() {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }

    private fun confirmAccountDeletion() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar cuenta")
            .setMessage("Esta acción borrará tu cuenta, materias y asignaciones guardadas. Para confirmar, escribe tu contraseña.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Continuar") { _, _ -> requestDeletePassword() }
            .show()
    }

    private fun requestDeletePassword() {
        val dialogBinding = DialogDeleteAccountBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar eliminación")
            .setView(dialogBinding.root)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar cuenta", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val password = dialogBinding.etConfirmarPassword.text?.toString().orEmpty()
                if (password.isBlank()) {
                    dialogBinding.etConfirmarPassword.error = "Escribe tu contraseña."
                } else {
                    studyViewModel.deleteAccount(password)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun openLibrary() {
        if (subjects.isEmpty()) {
            Snackbar.make(binding.root, "Agrega una materia para buscar libros.", Snackbar.LENGTH_LONG).show()
            return
        }
        startActivity(
            Intent(this, BooksActivity::class.java)
                .putStringArrayListExtra(ProfileExtras.SUBJECTS, ArrayList(subjects))
        )
    }

    private fun formatDate(timestamp: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))

    private companion object {
        const val HOME_BOOK_LIMIT = 8
    }
}
