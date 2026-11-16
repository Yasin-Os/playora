package com.yasin.playora.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songUri"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlaylistSong(
    val playlistId: Long,
    val songUri: String,
    val songTitle: String,
    val durationMs: Long,
    val position: Int
)

@Entity(tableName = "favorites", primaryKeys = ["songUri"])
data class Favorite(
    val songUri: String,
    val songTitle: String,
    val durationMs: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getPlaylists(): Flow<List<Playlist>>

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getSongs(playlistId: Long): Flow<List<PlaylistSong>>

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun songCount(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSong(song: PlaylistSong)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songUri = :uri")
    suspend fun removeSong(playlistId: Long, uri: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<Favorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Query("DELETE FROM favorites WHERE songUri = :uri")
    suspend fun removeFavorite(uri: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songUri = :uri)")
    suspend fun isFavoriteNow(uri: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE songUri = :uri)")
    fun isFavorite(uri: String): Flow<Boolean>
}

@Database(
    entities = [Playlist::class, PlaylistSong::class, Favorite::class],
    version = 1,
    exportSchema = false
)
abstract class PlayoraDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: PlayoraDatabase? = null

        fun get(context: Context): PlayoraDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PlayoraDatabase::class.java,
                    "playora.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
