package com.rajeshkavadi.ainews.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajeshkavadi.ainews.data.Article
import com.rajeshkavadi.ainews.data.Category
import com.rajeshkavadi.ainews.data.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Immutable UI state for one tab's headline list. */
data class HeadlinesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val articles: List<Article> = emptyList(),
    val failedSources: List<String> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Owns the selected tab and a per-category cache of [HeadlinesUiState]. Each tab
 * is loaded lazily on first view and kept in memory, so switching tabs is instant
 * and only pull-to-refresh (or the refresh button) refetches the current tab.
 */
class NewsViewModel(
    private val repository: NewsRepository = NewsRepository()
) : ViewModel() {

    private val _selected = MutableStateFlow(Category.AI)
    val selected: StateFlow<Category> = _selected.asStateFlow()

    private val _states = MutableStateFlow<Map<Category, HeadlinesUiState>>(emptyMap())
    val states: StateFlow<Map<Category, HeadlinesUiState>> = _states.asStateFlow()

    init {
        load(Category.AI, initial = true)
    }

    fun selectCategory(category: Category) {
        _selected.value = category
        val existing = _states.value[category]
        // Load the first time a tab is opened, or to recover from a prior failure.
        if (existing == null || (existing.articles.isEmpty() && !existing.isLoading)) {
            load(category, initial = true)
        }
    }

    fun refresh() = load(_selected.value, initial = false)

    private fun load(category: Category, initial: Boolean) {
        setState(category) {
            it.copy(isLoading = initial, isRefreshing = !initial, errorMessage = null)
        }
        viewModelScope.launch {
            try {
                val result = repository.loadTopHeadlines(category, limit = 15)
                setState(category) {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        articles = result.articles,
                        failedSources = result.failedSources,
                        errorMessage = if (result.articles.isEmpty())
                            "Couldn't load any headlines. Check your connection and try again."
                        else null
                    )
                }
            } catch (e: Exception) {
                setState(category) {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.message ?: "Something went wrong while loading headlines."
                    )
                }
            }
        }
    }

    private fun setState(
        category: Category,
        transform: (HeadlinesUiState) -> HeadlinesUiState
    ) {
        _states.update { map ->
            val current = map[category] ?: HeadlinesUiState()
            map + (category to transform(current))
        }
    }
}
