package com.aku.anice.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aku.anice.data.local.AppDatabase
import com.aku.anice.data.local.HistoryEntity
import com.aku.anice.data.model.Anime
import com.aku.anice.data.model.Episode
import com.aku.anice.data.model.VideoSource
import com.aku.anice.data.remote.JsoupScraper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class PlayerUiState(
    val anime: Anime? = null,
    val episodes: List<Episode> = emptyList(),
    val currentEpisode: Episode? = null,
    val lastEpisodeUrlFromHistory: String? = null,
    val currentSource: VideoSource? = null,
    val directUrl: String? = null,
    val sources: List<VideoSource> = emptyList(),
    val isLoading: Boolean = false,
    val isPlaying: Boolean = true,
    val showEpisodeList: Boolean = false,
    val showServerList: Boolean = false
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val scraper = JsoupScraper()
    private val historyDao = AppDatabase.getDatabase(application).historyDao()
    
    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    fun initPlayer(episode: Episode, episodes: List<Episode>, anime: Anime) {
        _uiState.value = _uiState.value.copy(
            anime = anime,
            episodes = episodes,
            currentEpisode = episode
        )
        loadVideoSources(episode.url)
        loadHistory(anime.detailUrl)
    }

    private fun loadHistory(detailUrl: String) {
        viewModelScope.launch {
            val history = historyDao.getAllHistory().firstOrNull()?.find { it.detailUrl == detailUrl }
            _uiState.value = _uiState.value.copy(
                lastEpisodeUrlFromHistory = history?.lastEpisodeUrl
            )
        }
    }

    private fun loadVideoSources(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val sources = scraper.getVideoSources(url)
            
            _uiState.value = _uiState.value.copy(
                sources = sources,
                isLoading = false
            )
            
            // Auto-Select server terbaik
            val bestSource = sources.find { it.serverName.contains("Dailymotion", true) } 
                ?: sources.find { it.serverName.contains("Hardsub", true) }
                ?: sources.find { it.serverName.contains("OK.ru", true) }
                ?: sources.firstOrNull()

            bestSource?.let { selectSource(it) }
            saveToHistory()
        }
    }

    fun selectSource(source: VideoSource) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(currentSource = source, isLoading = true, directUrl = null)
            val extracted = scraper.extractDirectLink(source.url)
            _uiState.value = _uiState.value.copy(directUrl = extracted, isLoading = false)
        }
    }

    private suspend fun saveToHistory() {
        val anime = _uiState.value.anime
        val currentEp = _uiState.value.currentEpisode
        if (anime != null && currentEp != null) {
            historyDao.insertOrUpdate(
                HistoryEntity(
                    detailUrl = anime.detailUrl,
                    title = anime.title,
                    thumbUrl = anime.thumbUrl,
                    lastEpisodeTitle = currentEp.title,
                    lastEpisodeUrl = currentEp.url
                )
            )
        }
    }

    fun updatePlaybackStatus(playing: Boolean) {
        _uiState.value = _uiState.value.copy(isPlaying = playing)
    }

    fun selectEpisode(episode: Episode) {
        _uiState.value = _uiState.value.copy(currentEpisode = episode)
        loadVideoSources(episode.url)
    }

    fun playNext() {
        val currentIndex = uiState.value.episodes.indexOf(uiState.value.currentEpisode)
        if (currentIndex < uiState.value.episodes.size - 1) {
            selectEpisode(uiState.value.episodes[currentIndex + 1])
        }
    }

    fun playPrevious() {
        val currentIndex = uiState.value.episodes.indexOf(uiState.value.currentEpisode)
        if (currentIndex > 0) {
            selectEpisode(uiState.value.episodes[currentIndex - 1])
        }
    }

    fun toggleEpisodeList(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEpisodeList = show)
    }

    fun toggleServerList(show: Boolean) {
        _uiState.value = _uiState.value.copy(showServerList = show)
    }
}
