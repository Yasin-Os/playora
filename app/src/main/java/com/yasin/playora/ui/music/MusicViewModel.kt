package com.yasin.playora.ui.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yasin.playora.media.LocalMedia
import com.yasin.playora.media.MediaStoreRepository
import com.yasin.playora.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application)
    val playerManager = PlayerManager(application)

    private val _songs = MutableStateFlow<List<LocalMedia>>(emptyList())
    val songs: StateFlow<List<LocalMedia>> = _songs.asStateFlow()

    private val _current = MutableStateFlow<LocalMedia?>(null)
    val current: StateFlow<LocalMedia?> = _current.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _songs.value = repository.loadAudio()
            _isLoading.value = false
        }
    }

    fun play(song: LocalMedia) {
        _current.value = song
        playerManager.play(song.uri)
    }

    fun togglePlayPause() {
        val player = playerManager.exoPlayer
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun onCleared() {
        playerManager.release()
        super.onCleared()
    }
}
