package com.yasin.playora.ui.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yasin.playora.R
import com.yasin.playora.player.EqualizerManager
import com.yasin.playora.player.PlaybackService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val sessionId = PlaybackService.audioSessionId
    val manager = remember(sessionId) {
        if (sessionId != 0) EqualizerManager(sessionId) else null
    }
    DisposableEffect(Unit) {
        onDispose { manager?.release() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.equalizer)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
        if (manager == null || !manager.isSupported) {
            androidx.compose.foundation.layout.Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.eq_play_first),
                    modifier = Modifier.padding(24.dp)
                )
            }
            return@Column
        }
        EqualizerContent(manager = manager)
    }
}

@Composable
private fun EqualizerContent(manager: EqualizerManager) {
    var enabled by remember { mutableStateOf(true) }
    var bassBoost by remember { mutableStateOf(0f) }
    val (minLevel, maxLevel) = manager.bandRange
    var levels by remember {
        mutableStateOf(List(manager.bandCount) { manager.getBandLevel(it.toShort()) })
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.enabled),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    manager.setEnabled(it)
                }
            )
        }
        Spacer(Modifier.height(16.dp))
        // Presets.
        Row {
            PresetChip(R.string.preset_normal) {
                manager.applyPreset(EqualizerManager.PRESET_NORMAL)
                levels = List(manager.bandCount) { manager.getBandLevel(it.toShort()) }
            }
            PresetChip(R.string.preset_bass) {
                manager.applyPreset(EqualizerManager.PRESET_BASS)
                levels = List(manager.bandCount) { manager.getBandLevel(it.toShort()) }
            }
            PresetChip(R.string.preset_treble) {
                manager.applyPreset(EqualizerManager.PRESET_TREBLE)
                levels = List(manager.bandCount) { manager.getBandLevel(it.toShort()) }
            }
            PresetChip(R.string.preset_vocal) {
                manager.applyPreset(EqualizerManager.PRESET_VOCAL)
                levels = List(manager.bandCount) { manager.getBandLevel(it.toShort()) }
            }
        }
        Spacer(Modifier.height(16.dp))
        for (band in 0 until manager.bandCount) {
            val freqHz = manager.bandCenterFreq(band.toShort()) / 1000
            val freqText = if (freqHz >= 1000) "%.1f kHz".format(freqHz / 1000f)
            else "$freqHz Hz"
            val level = levels.getOrNull(band) ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(freqText, modifier = Modifier.padding(end = 8.dp))
                Slider(
                    value = level.toFloat(),
                    onValueChange = { value ->
                        val newLevel = value.toInt().toShort()
                        manager.setBandLevel(band.toShort(), newLevel)
                        levels = levels.toMutableList().also { it[band] = newLevel }
                    },
                    valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.bass_boost), style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = bassBoost,
            onValueChange = {
                bassBoost = it
                manager.setBassBoost(it.toInt().toShort())
            },
            valueRange = 0f..1000f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PresetChip(labelRes: Int, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(stringResource(labelRes)) },
        modifier = Modifier.padding(end = 8.dp)
    )
}
