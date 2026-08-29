package com.aku.anice.ui.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aku.anice.data.local.AppDatabase
import com.aku.anice.data.local.FavoriteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {
    private val favoriteDao = AppDatabase.getDatabase(application).favoriteDao()

    val favoriteList: StateFlow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(url: String) {
        viewModelScope.launch {
            favoriteDao.delete(url)
        }
    }
}
