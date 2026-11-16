package com.yasin.playora.player

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Background audio playback. The [MediaSession] makes the system media
 * notification (play/pause/next/previous) appear automatically, and keeps
 * music playing when the app goes to the background.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        audioSessionId = player.audioSessionId
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        audioSessionId = 0
        super.onDestroy()
    }

    companion object {
        /** Audio session id of the service player (0 until the player exists). */
        @Volatile
        var audioSessionId: Int = 0
            private set
    }
}
