package com.quran.labs.androidquran.common.audio.memorization

import android.content.Context
import android.content.SharedPreferences
import com.quran.data.di.AppScope
import com.quran.mobile.di.qualifier.ApplicationContext
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Repository managing persistent storage, retrieval, and updating of [CustomPlaylist]s.
 */
@SingleIn(AppScope::class)
class PlaylistRepository(
  private val prefs: SharedPreferences
) {

  @Inject
  constructor(@ApplicationContext context: Context) : this(
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  )

  /**
   * Retrieves all saved playlists in descending order of last played (or created).
   */
  fun getAllPlaylists(): List<CustomPlaylist> {
    val raw = prefs.getString(KEY_ALL_PLAYLISTS, null) ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw.split(DELIMITER_PLAYLIST)
      .mapNotNull { CustomPlaylist.deserialize(it) }
      .sortedWith(compareByDescending<CustomPlaylist> { it.lastPlayedAt }.thenByDescending { it.createdAt })
  }

  /**
   * Finds a playlist by its unique [id].
   */
  fun getPlaylistById(id: String): CustomPlaylist? {
    return getAllPlaylists().firstOrNull { it.id == id }
  }

  /**
   * Saves or updates a [playlist].
   */
  fun savePlaylist(playlist: CustomPlaylist) {
    val existing = getAllPlaylists().toMutableList()
    val index = existing.indexOfFirst { it.id == playlist.id }
    if (index >= 0) {
      existing[index] = playlist
    } else {
      existing.add(0, playlist)
    }
    persistPlaylists(existing)
  }

  /**
   * Deletes a playlist by its [id].
   */
  fun deletePlaylist(id: String) {
    val existing = getAllPlaylists().toMutableList()
    existing.removeAll { it.id == id }
    persistPlaylists(existing)
  }

  /**
   * Updates the `lastPlayedAt` timestamp of a playlist when it is started.
   */
  fun updateLastPlayed(id: String, timestamp: Long = System.currentTimeMillis()) {
    val playlist = getPlaylistById(id) ?: return
    savePlaylist(playlist.copy(lastPlayedAt = timestamp))
  }

  /**
   * Clears all playlists from storage.
   */
  fun clearAllPlaylists() {
    prefs.edit().remove(KEY_ALL_PLAYLISTS).apply()
  }

  private fun persistPlaylists(playlists: List<CustomPlaylist>) {
    val serialized = playlists.joinToString(DELIMITER_PLAYLIST) { it.serialize() }
    prefs.edit().putString(KEY_ALL_PLAYLISTS, serialized).apply()
  }

  companion object {
    private const val PREFS_NAME = "quran_custom_playlists_prefs"
    private const val KEY_ALL_PLAYLISTS = "custom_playlists_data"
    private const val DELIMITER_PLAYLIST = ";;;"
  }
}
