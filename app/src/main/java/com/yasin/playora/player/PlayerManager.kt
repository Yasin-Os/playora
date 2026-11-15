package com.yasin.playora.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * Thin wrapper around Media3 ExoPlayer.
 * The UI reads [exoPlayer] directly (for PlayerView), while playback
 * commands go through the small API below.
 *
 * TODO (Phase 2): move playback into a MediaSessionService so audio keeps
 * playing in the background with notification controls.
 */
class PlayerManager(context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    /** Exposed for the Compose PlayerView in the video screen. */
    val exoPlayer: ExoPlayer get() = player

    fun play(uri: String) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.play()
    }

    fun playAll(uris: List<String>, startIndex: Int = 0) {
        val items = uris.map { MediaItem.fromUri(it) }
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    fun release() {
        player.release()
    }
}
