package com.example.basededatos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basededatos.network.AssignmentRecord
import com.example.basededatos.network.StudyData
import com.example.basededatos.network.UserStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class StudyUiState(
    val isLoading: Boolean = false,
    val subjects: List<String> = emptyList(),
    val assignments: List<AssignmentRecord> = emptyList(),
    val message: String? = null,
    val accountDeleted: Boolean = false
)

class StudyViewModel(
    private val userStore: UserStore
) : ViewModel() {

    private val _state = MutableLiveData(StudyUiState())
    val state: LiveData<StudyUiState> = _state
    private var currentEmail: String? = null

    fun loadForUser(email: String) {
        currentEmail = email.trim().lowercase()
        val account = currentEmail.orEmpty()
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true, message = null)
            try {
                publish(userStore.getStudyData(account))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value?.copy(
                    isLoading = false,
                    message = "No se pudieron cargar tus materias y asignaciones."
                )
            }
        }
    }

    fun addSubject(subject: String) = updateStudy(
        successMessage = "Materia agregada.",
        failureMessage = "Esa materia ya existe o no se pudo guardar."
    ) { email ->
        userStore.addSubject(email, subject)
    }

    fun removeSubject(subject: String) = updateStudy(
        successMessage = "Materia eliminada junto con sus asignaciones.",
        failureMessage = "No se pudo eliminar la materia."
    ) { email ->
        userStore.removeSubject(email, subject)
    }

    fun addAssignment(subject: String, title: String, dueAt: Long) = updateStudy(
        successMessage = "Asignación guardada.",
        failureMessage = "No se pudo guardar la asignación."
    ) { email ->
        userStore.addAssignment(email, subject, title, dueAt)
    }

    fun setAssignmentCompleted(assignment: AssignmentRecord, completed: Boolean) =
        updateStudy(
            successMessage = if (completed) "Asignación marcada como completada." else "Asignación pendiente.",
            failureMessage = "No se pudo actualizar la asignación."
        ) { email ->
            userStore.setAssignmentCompleted(email, assignment.id, completed)
        }

    fun deleteAssignment(assignment: AssignmentRecord) = updateStudy(
        successMessage = "Asignación eliminada.",
        failureMessage = "No se pudo eliminar la asignación."
    ) { email ->
        userStore.deleteAssignment(email, assignment.id)
    }

    fun deleteAccount(password: String) {
        val account = currentEmail ?: return
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true, message = null)
            try {
                val deleted = userStore.deleteAccount(account, password)
                if (deleted) {
                    _state.value = StudyUiState(accountDeleted = true)
                } else {
                    _state.value = _state.value?.copy(
                        isLoading = false,
                        message = "La contraseña no coincide. La cuenta sigue activa."
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value?.copy(
                    isLoading = false,
                    message = "No se pudo eliminar la cuenta. Inténtalo de nuevo."
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value?.copy(message = null)
    }

    private fun updateStudy(
        successMessage: String,
        failureMessage: String,
        action: suspend (String) -> Boolean
    ) {
        val account = currentEmail ?: return
        viewModelScope.launch {
            _state.value = _state.value?.copy(isLoading = true, message = null)
            try {
                val success = action(account)
                val data = userStore.getStudyData(account)
                publish(data, if (success) successMessage else failureMessage)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = _state.value?.copy(
                    isLoading = false,
                    message = "No se pudo guardar el cambio. Inténtalo de nuevo."
                )
            }
        }
    }

    private fun publish(data: StudyData, message: String? = null) {
        _state.value = StudyUiState(
            subjects = data.subjects,
            assignments = data.assignments,
            message = message
        )
    }
}
