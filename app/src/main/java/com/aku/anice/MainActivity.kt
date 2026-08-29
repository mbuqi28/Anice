package com.aku.anice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aku.anice.data.model.Anime
import com.aku.anice.data.model.Episode
import com.aku.anice.ui.home.HomeScreen
import com.aku.anice.ui.home.HomeViewModel
import com.aku.anice.ui.detail.DetailScreen
import com.aku.anice.ui.detail.DetailViewModel
import com.aku.anice.ui.history.HistoryScreen
import com.aku.anice.ui.history.HistoryViewModel
import com.aku.anice.ui.favorite.FavoriteScreen
import com.aku.anice.ui.favorite.FavoriteViewModel
import com.aku.anice.ui.player.PlayerScreen
import com.aku.anice.ui.player.PlayerViewModel
import com.aku.anice.ui.theme.AniceTheme

enum class Screen { Home, History, Favorite }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AniceTheme {
                var currentScreen by rememberSaveable { mutableStateOf(Screen.Home) }
                var selectedAnime by rememberSaveable { mutableStateOf<Anime?>(null) }
                var selectedEpisode by rememberSaveable { mutableStateOf<Episode?>(null) }
                var episodeList by rememberSaveable { mutableStateOf<List<Episode>>(emptyList()) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        selectedEpisode != null -> {
                            val playerViewModel: PlayerViewModel = viewModel()
                            PlayerScreen(
                                episode = selectedEpisode!!,
                                allEpisodes = episodeList,
                                anime = selectedAnime!!,
                                viewModel = playerViewModel,
                                onBackPressed = { selectedEpisode = null }
                            )
                            BackHandler { selectedEpisode = null }
                        }
                        selectedAnime != null -> {
                            val detailViewModel: DetailViewModel = viewModel()
                            DetailScreen(
                                anime = selectedAnime!!,
                                viewModel = detailViewModel,
                                onEpisodeClick = { episode, list ->
                                    selectedEpisode = episode
                                    episodeList = list
                                },
                                onBackPressed = { selectedAnime = null }
                            )
                            BackHandler { selectedAnime = null }
                        }
                        else -> {
                            Scaffold(
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 0.dp,
                                        modifier = Modifier.navigationBarsPadding()
                                    ) {
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.Home,
                                            onClick = { currentScreen = Screen.Home },
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                            label = { Text("Beranda") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.Favorite,
                                            onClick = { currentScreen = Screen.Favorite },
                                            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Favorite") },
                                            label = { Text("Favorit") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = currentScreen == Screen.History,
                                            onClick = { currentScreen = Screen.History },
                                            icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                            label = { Text("Riwayat") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(modifier = Modifier.padding(innerPadding)) {
                                    when (currentScreen) {
                                        Screen.Home -> {
                                            val homeViewModel: HomeViewModel = viewModel()
                                            HomeScreen(
                                                viewModel = homeViewModel,
                                                onAnimeClick = { anime -> selectedAnime = anime }
                                            )
                                        }
                                        Screen.Favorite -> {
                                            val favoriteViewModel: FavoriteViewModel = viewModel()
                                            FavoriteScreen(
                                                viewModel = favoriteViewModel,
                                                onAnimeClick = { anime -> selectedAnime = anime }
                                            )
                                        }
                                        Screen.History -> {
                                            val historyViewModel: HistoryViewModel = viewModel()
                                            HistoryScreen(
                                                viewModel = historyViewModel,
                                                onAnimeClick = { anime -> selectedAnime = anime }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
