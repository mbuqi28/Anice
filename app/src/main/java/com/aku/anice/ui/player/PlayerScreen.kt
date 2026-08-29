package com.aku.anice.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.aku.anice.data.model.Anime
import com.aku.anice.data.model.Episode
import com.aku.anice.ui.player.components.EmbedPlayer
import com.aku.anice.ui.player.components.EpisodeListSheet
import com.aku.anice.ui.player.components.GestureControls
import com.aku.anice.ui.player.components.PlayerOverlay
import com.aku.anice.ui.player.components.ServerListSheet
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    episode: Episode,
    allEpisodes: List<Episode>,
    anime: Anime,
    viewModel: PlayerViewModel,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var isDirectVideo by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    
    var isControllerVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isControllerVisible, uiState.isPlaying, lastInteractionTime) {
        if (isControllerVisible && uiState.isPlaying) {
            delay(3500)
            isControllerVisible = false
        }
    }

    val resetTimer = {
        lastInteractionTime = System.currentTimeMillis()
        isControllerVisible = true
    }

    fun toggleFullscreen() {
        resetTimer()
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    LaunchedEffect(episode, allEpisodes, anime) {
        viewModel.initPlayer(episode, allEpisodes, anime)
    }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    viewModel.updatePlaybackStatus(isPlaying)
                    if (!isPlaying) resetTimer()
                }
            })
        }
    }

    // HANDLER PEMUTARAN: Cek apakah link berhasil diekstrak atau harus WebView
    LaunchedEffect(uiState.videoStream, uiState.currentSource, uiState.isLoading) {
        if (uiState.isLoading) return@LaunchedEffect
        
        val stream = uiState.videoStream
        if (stream != null) {
            isDirectVideo = true
            
            // Gabungkan header default dengan header dari extractor
            val allHeaders = mutableMapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9",
                "Connection" to "keep-alive"
            )
            allHeaders.putAll(stream.headers)

            val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(allHeaders)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true)
            
            val mediaItem = androidx.media3.common.MediaItem.fromUri(stream.url)
            
            // MediaSource adaptif
            val mediaSource = if (stream.isHls) {
                androidx.media3.exoplayer.hls.HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.play()
            android.util.Log.d("Player", "Playing Native Pro: ${stream.url}")
        } else if (uiState.currentSource != null) {
            isDirectVideo = false
            exoPlayer.pause()
            isControllerVisible = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            activity?.let { act ->
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                act.window?.let { window ->
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isDirectVideo) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            uiState.currentSource?.let { source ->
                EmbedPlayer(
                    url = source.url,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (isDirectVideo) {
            GestureControls(
                onSingleTap = {
                    if (isControllerVisible) isControllerVisible = false else resetTimer()
                },
                onDoubleTap = { isForward ->
                    resetTimer()
                    val seekTime = if (isForward) 10000L else -10000L
                    exoPlayer.seekTo(exoPlayer.currentPosition + seekTime)
                },
                onVerticalSwipe = { isLeft, delta ->
                    resetTimer()
                    if (isLeft) {
                        val lp = activity?.window?.attributes
                        lp?.screenBrightness = ((lp?.screenBrightness ?: 0.5f) + delta / 1000f).coerceIn(0f, 1f)
                        activity?.window?.attributes = lp
                    }
                },
                onHorizontalSwipe = { delta ->
                    resetTimer()
                    exoPlayer.seekTo(exoPlayer.currentPosition + (delta * 100).toLong())
                },
                onDragEnd = { resetTimer() }
            ) {
                AnimatedVisibility(visible = isControllerVisible, enter = fadeIn(), exit = fadeOut()) {
                    PlayerOverlay(
                        currentEpisodeTitle = uiState.currentEpisode?.title ?: "Loading...",
                        isFullscreen = isFullscreen,
                        onShowEpisodeList = { resetTimer(); viewModel.toggleEpisodeList(true) },
                        onShowServerList = { resetTimer(); viewModel.toggleServerList(true) },
                        onFullscreenToggle = { toggleFullscreen() },
                        onPrevEpisode = { resetTimer(); viewModel.playPrevious() },
                        onNextEpisode = { resetTimer(); viewModel.playNext() },
                        onBack = onBackPressed
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isControllerVisible) {
                    IconButton(
                        onClick = { resetTimer() },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(end = 16.dp)
                            .align(Alignment.CenterEnd)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Menu, "Menu", tint = Color.White)
                    }
                }

                AnimatedVisibility(visible = isControllerVisible, enter = fadeIn(), exit = fadeOut()) {
                    PlayerOverlay(
                        currentEpisodeTitle = uiState.currentEpisode?.title ?: "Embed Mode",
                        isFullscreen = isFullscreen,
                        onShowEpisodeList = { resetTimer(); viewModel.toggleEpisodeList(true) },
                        onShowServerList = { resetTimer(); viewModel.toggleServerList(true) },
                        onFullscreenToggle = { toggleFullscreen() },
                        onPrevEpisode = { resetTimer(); viewModel.playPrevious() },
                        onNextEpisode = { resetTimer(); viewModel.playNext() },
                        onBack = onBackPressed,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (uiState.showEpisodeList) {
            EpisodeListSheet(
                episodes = uiState.episodes,
                currentEpisode = uiState.currentEpisode,
                lastEpisodeUrl = uiState.lastEpisodeUrlFromHistory,
                onEpisodeClick = { resetTimer(); viewModel.selectEpisode(it) },
                onDismiss = { resetTimer(); viewModel.toggleEpisodeList(false) }
            )
        }

        if (uiState.showServerList) {
            ServerListSheet(
                sources = uiState.sources,
                currentSource = uiState.currentSource,
                onSourceClick = { resetTimer(); viewModel.selectSource(it) },
                onDismiss = { resetTimer(); viewModel.toggleServerList(false) }
            )
        }
    }
}
