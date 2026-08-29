package com.aku.anice.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aku.anice.data.local.AppDatabase
import com.aku.anice.data.local.FavoriteEntity
import com.aku.anice.data.model.Anime
import com.aku.anice.data.model.Episode
import com.aku.anice.data.remote.JsoupScraper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DetailUiState(
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = false,
    val lastViewedEpisodeUrl: String? = null,
    val isFavorite: Boolean = false
)

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val scraper = JsoupScraper()
    private val database = AppDatabase.getDatabase(application)
    private val historyDao = database.historyDao()
    private val favoriteDao = database.favoriteDao()
    
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentDetailUrl: String? = null
    private var favoriteJob: Job? = null

    init {
        viewModelScope.launch {
            historyDao.getAllHistory().collect { historyList ->
                currentDetailUrl?.let { url ->
                    val history = historyList.find { it.detailUrl == url }
                    _uiState.update { it.copy(lastViewedEpisodeUrl = history?.lastEpisodeUrl) }
                }
            }
        }
    }

    fun loadEpisodes(url: String) {
        if (currentDetailUrl == url) return
        currentDetailUrl = url
        
        // Reset state untuk anime baru
        _uiState.update { it.copy(isLoading = true, episodes = emptyList(), isFavorite = false) }

        // Observe favorite status secara bersih
        favoriteJob?.cancel()
        favoriteJob = favoriteDao.isFavorite(url)
            .onEach { isFav -> _uiState.update { it.copy(isFavorite = isFav) } }
            .launchIn(viewModelScope)
        
        viewModelScope.launch {
            val list = scraper.getEpisodeList(url)
            _uiState.update { it.copy(episodes = list, isLoading = false) }
        }
    }

    fun toggleFavorite(anime: Anime) {
        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                favoriteDao.delete(anime.detailUrl)
            } else {
                favoriteDao.insert(
                    FavoriteEntity(
                        detailUrl = anime.detailUrl,
                        title = anime.title,
                        thumbUrl = anime.thumbUrl
                    )
                )
            }
        }
    }
}
