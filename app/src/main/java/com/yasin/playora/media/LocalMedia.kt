package com.yasin.playora.media

/** One local media file found on the device (video, audio or image). */
enum class MediaKind { VIDEO, AUDIO, IMAGE }

data class LocalMedia(
    val id: Long,
    val uri: String,
    val displayName: String,
    /** Duration in milliseconds. 0 for images. */
    val durationMs: Long,
    val sizeBytes: Long,
    /** Seconds since epoch, from MediaStore. */
    val dateAdded: Long,
    val mimeType: String,
    val kind: MediaKind
)
