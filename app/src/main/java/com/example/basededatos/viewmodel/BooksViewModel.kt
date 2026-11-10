package com.example.basededatos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.basededatos.model.BookItem
import com.example.basededatos.network.BooksApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import retrofit2.HttpException

data class BooksUiState(
    val isLoading: Boolean = false,
    val books: List<BookItem> = emptyList(),
    val isEmpty: Boolean = false,
    val errorMessage: String? = null
)

private data class BookRequestResult(
    val books: List<BookItem>,
    val error: Exception? = null
)

class BooksViewModel(
    private val booksApi: BooksApi,
    private val googleBooksApiKey: String
) : ViewModel() {

    private val _state = MutableLiveData(BooksUiState())
    val state: LiveData<BooksUiState> = _state
    private var loadJob: Job? = null
    private var loadedSubjects: List<String> = emptyList()

    fun loadSubjects(subjects: List<String>) {
        val normalized = subjects.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (normalized == loadedSubjects &&
            (_state.value?.isLoading == true ||
                _state.value?.books?.isNotEmpty() == true ||
                _state.value?.isEmpty == true)
        ) {
            return
        }
        loadedSubjects = normalized
        loadJob?.cancel()
        if (normalized.isEmpty()) {
            _state.value = BooksUiState(isEmpty = true)
            return
        }

        loadJob = viewModelScope.launch {
            _state.value = BooksUiState(isLoading = true)
            try {
                val results = coroutineScope {
                    normalized.map { subject ->
                        async(Dispatchers.IO) {
                            try {
                                val response = booksApi.getBooks(
                                    query = subject,
                                    maxResults = 10,
                                    apiKey = googleBooksApiKey.takeIf { it.isNotBlank() }
                                )
                                BookRequestResult(response.items.orEmpty())
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                BookRequestResult(emptyList(), error)
                            }
                        }
                    }.awaitAll()
                }

                val books = results.flatMap { it.books }
                    .distinctBy { it.id ?: it.volumeInfo?.title.orEmpty() }
                val errors = results.mapNotNull { it.error }
                val errorMessage = when {
                    errors.isEmpty() -> null
                    books.isNotEmpty() -> "Algunas materias no pudieron cargar sus libros."
                    else -> describeRequestFailure(errors.first())
                }
                _state.value = BooksUiState(
                    books = books,
                    isEmpty = books.isEmpty(),
                    errorMessage = errorMessage
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = BooksUiState(
                    isEmpty = true,
                    errorMessage = describeRequestFailure(error)
                )
            }
        }
    }

    private fun describeRequestFailure(error: Exception): String = when (error) {
        is HttpException -> {
            val missingKey = googleBooksApiKey.isBlank() && error.code() in 400..499
            if (missingKey) {
                "Google Books requiere una API key. Configura googleBooksApiKey en local.properties."
            } else {
                "Google Books respondió HTTP " + error.code() +
                    ". Revisa la API key, su cuota o sus restricciones."
            }
        }
        is UnknownHostException ->
            "No se pudo encontrar el servidor de Google Books. Revisa DNS o el acceso a ese servicio."
        is SocketTimeoutException ->
            "Google Books tardó demasiado en responder. Inténtalo de nuevo."
        is SSLHandshakeException ->
            "No se pudo establecer la conexión segura con Google Books."
        is IOException ->
            "La conexión con Google Books se interrumpió. Inténtalo de nuevo."
        else ->
            "No se pudieron procesar los resultados de Google Books."
    }
}
