package com.animehub.ui.screens.player

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.animehub.player.SystemUiController
import com.animehub.ui.components.ErrorState
import com.animehub.ui.components.LoadingState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    episodeId: String,
    animeId: String,
    sourceId: String,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    // Enter immersive mode
    DisposableEffect(Unit) {
        activity?.let { act ->
            SystemUiController.enterImmersive(act)
            SystemUiController.setDecorFitsSystemWindows(act, false)
        }
        onDispose {
            activity?.let { act ->
                SystemUiController.exitImmersive(act)
                SystemUiController.setDecorFitsSystemWindows(act, true)
            }
        }
    }

    LaunchedEffect(episodeId, animeId, sourceId) {
        viewModel.load(episodeId, animeId, sourceId)
    }

    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    // Poll playback position every 2 seconds during playback
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            delay(2000)
            val pos = p.currentPosition
            val dur = p.duration
            if (p.isPlaying && dur > 0L) {
                viewModel.onProgressChanged(pos, dur)
            }
        }
    }

    // Back press saves progress
    BackHandler {
        viewModel.saveCurrentProgress()
        player?.stop()
        player?.release()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveCurrentProgress()
            player?.stop()
            player?.release()
        }
    }

    when (val state = uiState) {
        is PlayerUiState.Loading -> {
            LoadingState()
        }
        is PlayerUiState.Error -> {
            ErrorState(
                message = state.message,
                onRetry = { viewModel.load(episodeId, animeId, sourceId) }
            )
        }
        is PlayerUiState.Ready -> {
            val media = state.media
            val initialProgress = state.initialProgressMs

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = ExoPlayer.Builder(ctx).build().also { exo ->
                                val mediaItem = MediaItem.fromUri(Uri.parse(media.uri))
                                exo.setMediaItem(mediaItem)
                                if (initialProgress > 0L) {
                                    exo.seekTo(initialProgress)
                                }
                                exo.prepare()
                                exo.playWhenReady = true
                                exo.addListener(object : Player.Listener {
                                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                                        if (isPlaying) viewModel.startAutoSave()
                                    }
                                    override fun onPlayerError(error: PlaybackException) {
                                        val msg = when {
                                            error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
                                            error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "视频文件未找到"
                                            error.errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "无权限访问视频文件"
                                            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED -> "视频解码失败，格式可能不支持"
                                            else -> "播放出错: ${error.localizedMessage ?: "未知错误"}"
                                        }
                                        viewModel.onPlayerError(msg)
                                    }
                                })
                            }
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                )
            }
        }
    }
}
