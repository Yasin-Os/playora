package com.yasin.playora.data

import android.content.Context
import com.yasin.playora.media.LocalMedia
import kotlinx.coroutines.flow.Flow

/** Playlists and favorites, backed by Room. */
class MusicLibraryRepository(context: Context) {
    private val db = PlayoraDatabase.get(context)
    private val playlistDao = db.playlistDao()
    private val favoriteDao = db.favoriteDao()

    val playlists: Flow<List<Playlist>> = playlistDao.getPlaylists()
    val favorites: Flow<List<Favorite>> = favoriteDao.getFavorites()

    suspend fun createPlaylist(name: String): Long =
        playlistDao.insertPlaylist(Playlist(name = name))

    suspend fun deletePlaylist(id: Long) = playlistDao.deletePlaylist(id)

    fun playlistSongs(playlistId: Long): Flow<List<PlaylistSong>> =
        playlistDao.getSongs(playlistId)

    suspend fun addSongToPlaylist(playlistId: Long, song: LocalMedia) {
        val position = playlistDao.songCount(playlistId)
        playlistDao.addSong(
            PlaylistSong(
                playlistId = playlistId,
                songUri = song.uri,
                songTitle = song.displayName,
                durationMs = song.durationMs,
                position = position
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, uri: String) =
        playlistDao.removeSong(playlistId, uri)

    suspend fun toggleFavorite(song: LocalMedia) {
        if (favoriteDao.isFavoriteNow(song.uri)) {
            favoriteDao.removeFavorite(song.uri)
        } else {
            favoriteDao.addFavorite(
                Favorite(
                    songUri = song.uri,
                    songTitle = song.displayName,
                    durationMs = song.durationMs
                )
            )
        }
    }

    fun isFavorite(uri: String): Flow<Boolean> = favoriteDao.isFavorite(uri)
}
