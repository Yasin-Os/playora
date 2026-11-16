package com.yasin.playora.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

/**
 * System audio effects attached to a player's audio session.
 * Works with any ExoPlayer instance (video player or the background service).
 * Every call is guarded: some devices have no equalizer, and an audio
 * session id of 0 means no audio has been set up yet.
 */
class EqualizerManager(audioSessionId: Int) {

    private val equalizer: Equalizer? = try {
        Equalizer(0, audioSessionId)
    } catch (_: Exception) {
        null
    }
    private val bassBoost: BassBoost? = try {
        BassBoost(0, audioSessionId)
    } catch (_: Exception) {
        null
    }

    val isSupported: Boolean get() = equalizer != null

    val bandCount: Int get() = equalizer?.numberOfBands?.toInt() ?: 0

    /** Min/max band level in milliBels. */
    val bandRange: Pair<Short, Short>
        get() = equalizer?.let { it.bandLevelRange[0] to it.bandLevelRange[1] }
            ?: (0.toShort() to 0.toShort())

    /** Center frequency of a band, in milliHertz. */
    fun bandCenterFreq(band: Short): Int = equalizer?.getCenterFreq(band) ?: 0

    fun getBandLevel(band: Short): Short = equalizer?.getBandLevel(band) ?: 0

    fun setBandLevel(band: Short, level: Short) {
        equalizer?.setBandLevel(band, level)
    }

    fun setBassBoost(strength: Short) {
        bassBoost?.setStrength(strength)
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
    }

    /** Applies a preset. Levels are -1..1 per band (5-band presets). */
    fun applyPreset(levels: List<Float>) {
        val (min, max) = bandRange
        val count = bandCount
        if (count == 0) return
        for (band in 0 until count) {
            val presetIndex = (band * levels.size / count).coerceIn(0, levels.size - 1)
            val normalized = levels[presetIndex].coerceIn(-1f, 1f)
            val level = if (normalized >= 0) {
                (normalized * max).toInt()
            } else {
                (normalized * -min).toInt()
            }
            setBandLevel(band.toShort(), level.toShort())
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
    }

    companion object {
        val PRESET_NORMAL = listOf(0f, 0f, 0f, 0f, 0f)
        val PRESET_BASS = listOf(0.8f, 0.6f, 0.2f, -0.2f, -0.3f)
        val PRESET_TREBLE = listOf(-0.4f, -0.2f, 0.1f, 0.5f, 0.8f)
        val PRESET_VOCAL = listOf(-0.3f, 0f, 0.5f, 0.4f, 0f)
    }
}
