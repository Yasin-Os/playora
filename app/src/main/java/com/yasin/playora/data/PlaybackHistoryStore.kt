package com.yasin.playora.data

import android.content.Context

/** Remembers the last played position of each video, keyed by its uri. */
class PlaybackHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("playora_history", Context.MODE_PRIVATE)

    fun getPosition(uri: String): Long = prefs.getLong(key(uri), 0L)

    fun savePosition(uri: String, positionMs: Long) {
        prefs.edit().putLong(key(uri), positionMs).apply()
    }

    private fun key(uri: String): String = "pos_" + uri.hashCode()
}
