package com.aku.anice.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aku.anice.data.model.Anime
import com.aku.anice.data.remote.JsoupScraper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val animeList: List<Anime> = emptyList(),
    val isLoading: Boolean = false,
    val isPaginationLoading: Boolean = false,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val endOfList: Boolean = false,
    val filter: FilterState = FilterState(),
    val isFilterOpen: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val scraper = JsoupScraper()
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAnime()
    }

    fun loadAnime(query: String? = null, filter: FilterState? = null) {
        viewModelScope.launch {
            val currentFilter = filter ?: _uiState.value.filter
            _uiState.value = _uiState.value.copy(
                isLoading = true, 
                searchQuery = query ?: "",
                filter = currentFilter,
                currentPage = 1,
                animeList = emptyList(),
                endOfList = false
            )
            val list = scraper.getLatestAnime(
                query = _uiState.value.searchQuery,
                page = 1,
                status = currentFilter.status,
                order = currentFilter.order
            )
            _uiState.value = _uiState.value.copy(animeList = list, isLoading = false)
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState.isPaginationLoading || currentState.endOfList || currentState.isLoading) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isPaginationLoading = true)
            val nextPage = currentState.currentPage + 1
            val newList = scraper.getLatestAnime(
                query = currentState.searchQuery,
                page = nextPage,
                status = currentState.filter.status,
                order = currentState.filter.order
            )
            
            if (newList.isEmpty()) {
                _uiState.value = _uiState.value.copy(isPaginationLoading = false, endOfList = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    animeList = currentState.animeList + newList,
                    currentPage = nextPage,
                    isPaginationLoading = false
                )
            }
        }
    }

    fun toggleFilter(open: Boolean) {
        _uiState.value = _uiState.value.copy(isFilterOpen = open)
    }
}
