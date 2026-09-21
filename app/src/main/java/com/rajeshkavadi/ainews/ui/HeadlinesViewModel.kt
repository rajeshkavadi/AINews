package com.rajeshkavadi.ainews.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajeshkavadi.ainews.data.Article
import com.rajeshkavadi.ainews.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Immutable UI state for the headlines screen. */
data class HeadlinesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val articles: List<Article> = emptyList(),
    val failedSources: List<String> = emptyList(),
    val errorMessage: String? = null
)

class HeadlinesViewModel(
    private val repository: NewsRepository = NewsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HeadlinesUiState(isLoading = true))
    val uiState: StateFlow<HeadlinesUiState> = _uiState.asStateFlow()

    init {
        load(initial = true)
    }

    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = initial,
                isRefreshing = !initial,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                val result = repository.loadTopHeadlines(limit = 15)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        articles = result.articles,
                        failedSources = result.failedSources,
                        // Only treat "nothing loaded at all" as a hard error.
                        errorMessage = if (result.articles.isEmpty())
                            "Couldn't load any headlines. Check your connection and try again."
                        else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.message ?: "Something went wrong while loading headlines."
                    )
                }
            }
        }
    }
}
