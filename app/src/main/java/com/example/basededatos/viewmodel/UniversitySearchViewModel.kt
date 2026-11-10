package com.example.basededatos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basededatos.model.University
import com.example.basededatos.network.UniversityApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException
import retrofit2.HttpException

data class UniversitySearchUiState(
    val universities: List<University> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class UniversitySearchViewModel(
    private val universityApi: UniversityApi
) : ViewModel() {

    private val _state = MutableLiveData(UniversitySearchUiState())
    val state: LiveData<UniversitySearchUiState> = _state
    private var searchJob: Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        val term = query.trim()
        if (term.length < MIN_QUERY_LENGTH) {
            _state.value = UniversitySearchUiState()
            return
        }

        searchJob = viewModelScope.launch {
            _state.value = UniversitySearchUiState(isLoading = true)
            delay(DEBOUNCE_MS)
            try {
                val apiResults = withContext(Dispatchers.IO) {
                    universityApi.getUniversities(country = MEXICO_COUNTRY, name = term)
                }
                val universities = (apiResults.filter(::isMexicanUniversity) + localUniversities(term))
                    .filter { !it.name.isNullOrBlank() }
                    .distinctBy {
                        it.name.orEmpty().trim().lowercase() + "|" + it.country.orEmpty().trim().lowercase()
                    }
                    .take(MAX_RESULTS)
                _state.value = UniversitySearchUiState(universities = universities)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val fallback = localUniversities(term)
                _state.value = UniversitySearchUiState(
                    universities = fallback,
                    errorMessage = if (fallback.isNotEmpty()) {
                        val detail = when (error) {
                            is HttpException -> "Hipolabs respondió HTTP " + error.code() + "."
                            is UnknownHostException -> "No se encontró el servidor de Hipolabs."
                            is IOException -> "Hipolabs no respondió por red."
                            else -> "La búsqueda de Hipolabs no respondió."
                        }
                        detail + " Mostramos sugerencias locales."
                    } else {
                        when (error) {
                            is HttpException -> "Hipolabs respondió HTTP " + error.code() + "."
                            is UnknownHostException -> "No se encontró el servidor de universidades. Revisa DNS."
                            is IOException -> "No se pudo conectar con Hipolabs. Inténtalo de nuevo."
                            else -> "No fue posible buscar universidades en este momento."
                        }
                    }
                )
            }
        }
    }

    private fun isMexicanUniversity(university: University): Boolean =
        university.alphaTwoCode?.equals(MEXICO_CODE, ignoreCase = true) == true ||
            university.country?.equals(MEXICO_COUNTRY, ignoreCase = true) == true ||
            university.country?.equals("México", ignoreCase = true) == true

    private fun localUniversities(query: String): List<University> =
        LOCAL_UNIVERSITIES.filter { it.name.orEmpty().contains(query, ignoreCase = true) }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val MIN_QUERY_LENGTH = 2
        const val MAX_RESULTS = 10
        const val MEXICO_COUNTRY = "Mexico"
        const val MEXICO_CODE = "MX"

        val LOCAL_UNIVERSITIES = listOf(
            "Universidad Politécnica del Estado de Morelos (UPEMOR)",
            "Universidad Autónoma del Estado de Morelos (UAEM)",
            "Universidad Nacional Autónoma de México (UNAM)",
            "Instituto Politécnico Nacional (IPN)",
            "Universidad Autónoma Metropolitana (UAM)",
            "Universidad de Guadalajara (UDG)",
            "Instituto Tecnológico y de Estudios Superiores de Monterrey (ITESM)",
            "Universidad Anáhuac",
            "Universidad Autónoma de Nuevo León (UANL)",
            "Benemérita Universidad Autónoma de Puebla (BUAP)"
        ).map { name ->
            University(
                name = name,
                country = "México",
                alphaTwoCode = "MX",
                stateProvince = null,
                domains = emptyList(),
                webPages = emptyList()
            )
        }
    }
}
