package com.yasin.playora.ui.photos

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasin.playora.R
import com.yasin.playora.photo.PhotoEditor
import com.yasin.playora.photo.PhotoSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Filter { NONE, GRAYSCALE, SEPIA, WARM, COOL }

class PhotoEditorViewModel(application: Application) : AndroidViewModel(application) {

    private var original: Bitmap? = null
    private var base: Bitmap? = null

    private val _edited = MutableStateFlow<Bitmap?>(null)
    val edited: StateFlow<Bitmap?> = _edited.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    var brightness by mutableStateOf(0f)
        private set
    var contrast by mutableStateOf(1f)
        private set
    var saturation by mutableStateOf(1f)
        private set
    var filter by mutableStateOf(Filter.NONE)
        private set

    fun load(uri: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val bitmap = withContext(Dispatchers.IO) { loadBitmap(uri) }
            original = bitmap
            base = bitmap
            _edited.value = bitmap
            _isLoading.value = false
        }
    }

    fun updateBrightness(value: Float) {
        brightness = value
        recompute()
    }

    fun updateContrast(value: Float) {
        contrast = value
        recompute()
    }

    fun updateSaturation(value: Float) {
        saturation = value
        recompute()
    }

    fun updateFilter(newFilter: Filter) {
        filter = newFilter
        recompute()
    }

    fun rotate() {
        base = base?.let { PhotoEditor.rotate(it, 90f) }
        recompute()
    }

    fun flip() {
        base = base?.let { PhotoEditor.flipHorizontal(it) }
        recompute()
    }

    fun cropSquare() {
        base = base?.let { PhotoEditor.cropCenterSquare(it) }
        recompute()
    }

    fun save() {
        val bitmap = _edited.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val name = "Playora_${System.currentTimeMillis()}.jpg"
            _saved.value = PhotoSaver.save(getApplication(), bitmap, name)
        }
    }

    private fun recompute() {
        val source = base ?: return
        viewModelScope.launch(Dispatchers.Default) {
            var result = source
            result = PhotoEditor.adjustBrightness(result, brightness)
            result = PhotoEditor.adjustContrast(result, contrast)
            result = PhotoEditor.adjustSaturation(result, saturation)
            result = when (filter) {
                Filter.NONE -> result
                Filter.GRAYSCALE -> PhotoEditor.grayscale(result)
                Filter.SEPIA -> PhotoEditor.sepia(result)
                Filter.WARM -> PhotoEditor.warm(result)
                Filter.COOL -> PhotoEditor.cool(result)
            }
            _edited.value = result
        }
    }

    private fun loadBitmap(uri: String): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        val parsed = Uri.parse(uri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(parsed)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxDim / sample > 2048) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(parsed)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(uri: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: PhotoEditorViewModel = viewModel()
    val edited by viewModel.edited.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saved by viewModel.saved.collectAsState()

    LaunchedEffect(uri) { viewModel.load(uri) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.edit)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            actions = {
                IconButton(onClick = { viewModel.save() }) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.save))
                }
            }
        )
        if (isLoading) {
            androidx.compose.foundation.layout.Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            edited?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
        if (saved) {
            Text(
                text = stringResource(R.string.saved),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            EditSlider(
                label = stringResource(R.string.brightness),
                value = viewModel.brightness,
                range = -1f..1f,
                onChange = { viewModel.updateBrightness(it) }
            )
            EditSlider(
                label = stringResource(R.string.contrast),
                value = viewModel.contrast,
                range = 0f..2f,
                onChange = { viewModel.updateContrast(it) }
            )
            EditSlider(
                label = stringResource(R.string.saturation),
                value = viewModel.saturation,
                range = 0f..2f,
                onChange = { viewModel.updateSaturation(it) }
            )
            Row {
                Button(onClick = { viewModel.rotate() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.RotateRight, contentDescription = null)
                    Text(stringResource(R.string.rotate), Modifier.padding(start = 4.dp))
                }
                Spacer(Modifier.padding(4.dp))
                Button(onClick = { viewModel.flip() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Flip, contentDescription = null)
                    Text(stringResource(R.string.flip), Modifier.padding(start = 4.dp))
                }
                Spacer(Modifier.padding(4.dp))
                Button(onClick = { viewModel.cropSquare() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Crop, contentDescription = null)
                    Text(stringResource(R.string.crop), Modifier.padding(start = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow {
                items(Filter.entries) { item ->
                    FilterChip(
                        selected = viewModel.filter == item,
                        onClick = { viewModel.updateFilter(item) },
                        label = { Text(stringResource(filterLabel(item))) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EditSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.padding(end = 8.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun filterLabel(filter: Filter): Int = when (filter) {
    Filter.NONE -> R.string.filter_none
    Filter.GRAYSCALE -> R.string.filter_grayscale
    Filter.SEPIA -> R.string.filter_sepia
    Filter.WARM -> R.string.filter_warm
    Filter.COOL -> R.string.filter_cool
}
