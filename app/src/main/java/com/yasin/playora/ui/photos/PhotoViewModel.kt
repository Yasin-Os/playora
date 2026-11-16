package com.yasin.playora.ui.photos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yasin.playora.media.LocalMedia
import com.yasin.playora.media.MediaStoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreRepository(application)

    private val _photos = MutableStateFlow<List<LocalMedia>>(emptyList())
    val photos: StateFlow<List<LocalMedia>> = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _photos.value = repository.loadImages()
            _isLoading.value = false
        }
    }
}
