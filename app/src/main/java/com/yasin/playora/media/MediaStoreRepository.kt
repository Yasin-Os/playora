package com.yasin.playora.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads the device's videos, music and photos through MediaStore. */
class MediaStoreRepository(private val context: Context) {

    suspend fun loadVideos(): List<LocalMedia> = withContext(Dispatchers.IO) {
        query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, MediaKind.VIDEO)
    }

    suspend fun loadAudio(): List<LocalMedia> = withContext(Dispatchers.IO) {
        query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, MediaKind.AUDIO)
    }

    suspend fun loadImages(): List<LocalMedia> = withContext(Dispatchers.IO) {
        query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaKind.IMAGE)
    }

    private fun query(uri: Uri, kind: MediaKind): List<LocalMedia> {
        // Images have no DURATION column, so the projection differs per kind.
        val projection = if (kind == MediaKind.IMAGE) {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE
            )
        } else {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DURATION,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.MIME_TYPE
            )
        }
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        val items = mutableListOf<LocalMedia>()
        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                items += LocalMedia(
                    id = id,
                    uri = ContentUris.withAppendedId(uri, id).toString(),
                    displayName = cursor.getString(nameCol) ?: "",
                    durationMs = if (durationCol >= 0) cursor.getLong(durationCol) else 0L,
                    sizeBytes = cursor.getLong(sizeCol),
                    dateAdded = cursor.getLong(dateCol),
                    mimeType = cursor.getString(mimeCol) ?: "",
                    kind = kind
                )
            }
        }
        return items
    }
}
