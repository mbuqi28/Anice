package com.aku.anice.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aku.anice.data.local.AppDatabase
import com.aku.anice.data.local.HistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val historyDao = AppDatabase.getDatabase(application).historyDao()

    val historyList: StateFlow<List<HistoryEntity>> = historyDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteHistory(detailUrl: String) {
        viewModelScope.launch {
            historyDao.delete(detailUrl)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }
}
