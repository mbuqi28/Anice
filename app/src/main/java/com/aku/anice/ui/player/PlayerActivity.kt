package com.aku.anice.ui.player

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.widget.ImageButton
import android.widget.TextView
import com.aku.anice.R
import com.aku.anice.data.extractor.ServerPriorityManager
import com.aku.anice.data.remote.AnichinParser
import com.aku.anice.data.model.VideoSource
import com.aku.anice.data.remote.NetworkClient
import com.aku.anice.databinding.ActivityPlayerBinding
import kotlinx.coroutines.*
import java.util.Queue

@OptIn(UnstableApi::class)
class PlayerActivity : AppCompatActivity(), PlayerGestureController.GestureListener {

    private lateinit var binding: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var serverQueue: Queue<VideoSource> = java.util.LinkedList()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    
    private lateinit var audioManager: AudioManager
    private lateinit var gestureController: PlayerGestureController
    private lateinit var parser: AnichinParser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        gestureController = PlayerGestureController(this, this)
        binding.playerView.setOnTouchListener(gestureController)
        parser = AnichinParser(this)

        val episodeUrl = intent.getStringExtra("EPISODE_URL") ?: ""
        val episodeTitle = intent.getStringExtra("EPISODE_TITLE") ?: ""
        val animeTitle = intent.getStringExtra("ANIME_TITLE") ?: ""

        val tvTitle = binding.playerView.findViewById<TextView>(R.id.tvPlayerTitle)
        val btnBack = binding.playerView.findViewById<ImageButton>(R.id.btnBack)
        
        tvTitle?.text = if (animeTitle.isNotEmpty()) "$animeTitle - $episodeTitle" else episodeTitle
        btnBack?.setOnClickListener { finish() }

        setupPlayer()
        fetchServersAndPlay(episodeUrl)
    }

    private fun setupPlayer() {
        val okHttpClient = NetworkClient.getClient(this)
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(NetworkClient.USER_AGENT)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        playNextAvailableServer()
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        binding.playerLoading.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                    }
                })
            }
        
        binding.playerView.player = player
    }

    private fun fetchServersAndPlay(url: String) {
        binding.playerLoading.visibility = View.VISIBLE
        scope.launch {
            val servers = parser.getVideoServers(url)
            if (servers.isNotEmpty()) {
                serverQueue = ServerPriorityManager.getPriorityQueue(servers)
                playNextAvailableServer()
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PlayerActivity, "No servers found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun playNextAvailableServer() {
        val nextServer = serverQueue.poll() ?: run {
            Toast.makeText(this, "All servers failed", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.playerLoading.visibility = View.VISIBLE
        
        scope.launch {
            // Using direct URL if it's already a stream or needs more extraction
            // For simplicity, we assume the URL from parser is usable or add more extraction here
            val mediaItem = MediaItem.Builder()
                .setUri(nextServer.url)
                .build()
            
            val dataSourceFactory = OkHttpDataSource.Factory(NetworkClient.getClient(this@PlayerActivity))
                .setUserAgent(NetworkClient.USER_AGENT)
            
            // Try HLS first, then fallback to progressive
            val mediaSource = if (nextServer.url.contains(".m3u8")) {
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            } else {
                DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
            }

            withContext(Dispatchers.Main) {
                player?.setMediaSource(mediaSource)
                player?.prepare()
            }
        }
    }

    override fun onBrightnessChange(delta: Float) {
        val layoutParams = window.attributes
        val newBrightness = (layoutParams.screenBrightness.coerceAtLeast(0f) + delta).coerceIn(0.01f, 1.0f)
        layoutParams.screenBrightness = newBrightness
        window.attributes = layoutParams
        showGestureStatus("Brightness: ${(newBrightness * 100).toInt()}%")
    }

    override fun onVolumeChange(delta: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (currentVolume + (delta * maxVolume)).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        showGestureStatus("Volume: ${(newVolume.toFloat() / maxVolume * 100).toInt()}%")
    }

    override fun onSeekChange(delta: Float) {
        player?.let {
            val duration = it.duration
            if (duration > 0) {
                val seekAmount = (delta * duration).toLong()
                val newPosition = (it.currentPosition + seekAmount).coerceIn(0, duration)
                it.seekTo(newPosition)
                showGestureStatus("Seek: ${newPosition / 1000}s / ${duration / 1000}s")
            }
        }
    }

    override fun onSeekEnd() {
        binding.tvGestureStatus.visibility = View.GONE
    }

    override fun onDoubleTapLeft() {
        player?.let { it.seekTo((it.currentPosition - 10000).coerceAtLeast(0)) }
        showGestureStatus("-10s")
        scope.launch {
            delay(500)
            binding.tvGestureStatus.visibility = View.GONE
        }
    }

    override fun onDoubleTapRight() {
        player?.let { it.seekTo((it.currentPosition + 10000).coerceAtMost(it.duration)) }
        showGestureStatus("+10s")
        scope.launch {
            delay(500)
            binding.tvGestureStatus.visibility = View.GONE
        }
    }

    override fun onSingleTap() {
        if (binding.playerView.isControllerFullyVisible) {
            binding.playerView.hideController()
        } else {
            binding.playerView.showController()
        }
    }

    private fun showGestureStatus(text: String) {
        binding.tvGestureStatus.text = text
        binding.tvGestureStatus.visibility = View.VISIBLE
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        player?.release()
    }
}
