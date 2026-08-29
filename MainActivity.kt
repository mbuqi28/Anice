package com.aku.anice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aku.anice.data.model.Anime
import com.aku.anice.data.model.Episode
import com.aku.anice.ui.home.HomeScreen
import com.aku.anice.ui.home.HomeViewModel
import com.aku.anice.ui.detail.DetailScreen
import com.aku.anice.ui.detail.DetailViewModel
import com.aku.anice.ui.player.PlayerScreen
import com.aku.anice.ui.player.PlayerViewModel
import com.aku.anice.ui.theme.AniceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AniceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var selectedAnime by remember { mutableStateOf<Anime?>(null) }
                    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
                    var episodeList by remember { mutableStateOf<List<Episode>>(emptyList()) }

                    when {
                        selectedEpisode != null -> {
                            // Layar 3: Player
                            val playerViewModel: PlayerViewModel = viewModel()
                            PlayerScreen(
                                episode = selectedEpisode!!,
                                allEpisodes = episodeList,
                                viewModel = playerViewModel,
                                onBackPressed = { selectedEpisode = null }
                            )
                            BackHandler { selectedEpisode = null }
                        }
                        selectedAnime != null -> {
                            // Layar 2: Detail (Banner + List Episode)
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
                            // Layar 1: Home
                            val homeViewModel: HomeViewModel = viewModel()
                            HomeScreen(
                                viewModel = homeViewModel,
                                onAnimeClick = { anime ->
                                    selectedAnime = anime
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
