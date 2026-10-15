package com.quran.labs.androidquran.common.audio.memorization

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import org.junit.Before
import org.junit.Test

class PlaylistRepositoryTest {

  private lateinit var fakePrefs: FakeSharedPreferences
  private lateinit var repository: PlaylistRepository
  private lateinit var quranInfo: QuranInfo

  @Before
  fun setUp() {
    fakePrefs = FakeSharedPreferences()
    repository = PlaylistRepository(fakePrefs)
    quranInfo = QuranInfo(MadaniDataSource())
  }

  @Test
  fun `initial state has no playlists`() {
    assertThat(repository.getAllPlaylists()).isEmpty()
  }

  @Test
  fun `save and retrieve single playlist`() {
    val seg1 = PlaylistSegment.createFullSurah(1, quranInfo, "Al-Hamd")
    val seg2 = PlaylistSegment(sura = 2, startAyah = 1, endAyah = 7, isFullSurah = false, customLabel = "Al-Baqarah 1-7")
    val playlist = CustomPlaylist(
      id = "test-pl-1",
      name = "Daily Recitation",
      segments = listOf(seg1, seg2),
      createdAt = 1000L,
      lastPlayedAt = 2000L
    )

    repository.savePlaylist(playlist)

    val all = repository.getAllPlaylists()
    assertThat(all).hasSize(1)
    val retrieved = all[0]
    assertThat(retrieved.id).isEqualTo("test-pl-1")
    assertThat(retrieved.name).isEqualTo("Daily Recitation")
    assertThat(retrieved.segments).hasSize(2)
    assertThat(retrieved.segments[0].sura).isEqualTo(1)
    assertThat(retrieved.segments[0].startAyah).isEqualTo(1)
    assertThat(retrieved.segments[0].endAyah).isEqualTo(7)
    assertThat(retrieved.segments[0].isFullSurah).isTrue()
    assertThat(retrieved.segments[1].sura).isEqualTo(2)
    assertThat(retrieved.segments[1].startAyah).isEqualTo(1)
    assertThat(retrieved.segments[1].endAyah).isEqualTo(7)
    assertThat(retrieved.segments[1].isFullSurah).isFalse()

    val byId = repository.getPlaylistById("test-pl-1")
    assertThat(byId).isNotNull()
    assertThat(byId?.name).isEqualTo("Daily Recitation")
  }

  @Test
  fun `update existing playlist`() {
    val playlist = CustomPlaylist(
      id = "test-pl-1",
      name = "Original Name",
      segments = listOf(PlaylistSegment.createFullSurah(1, quranInfo))
    )
    repository.savePlaylist(playlist)

    val updated = playlist.copy(name = "Updated Name")
    repository.savePlaylist(updated)

    val all = repository.getAllPlaylists()
    assertThat(all).hasSize(1)
    assertThat(all[0].name).isEqualTo("Updated Name")
  }

  @Test
  fun `delete playlist`() {
    val pl1 = CustomPlaylist(id = "1", name = "First")
    val pl2 = CustomPlaylist(id = "2", name = "Second")
    repository.savePlaylist(pl1)
    repository.savePlaylist(pl2)

    assertThat(repository.getAllPlaylists()).hasSize(2)

    repository.deletePlaylist("1")
    val remaining = repository.getAllPlaylists()
    assertThat(remaining).hasSize(1)
    assertThat(remaining[0].id).isEqualTo("2")
  }

  @Test
  fun `updateLastPlayed changes lastPlayedAt and ordering`() {
    val pl1 = CustomPlaylist(id = "1", name = "First", createdAt = 100L, lastPlayedAt = 100L)
    val pl2 = CustomPlaylist(id = "2", name = "Second", createdAt = 200L, lastPlayedAt = 200L)
    repository.savePlaylist(pl1)
    repository.savePlaylist(pl2)

    // Initially pl2 has higher lastPlayedAt
    assertThat(repository.getAllPlaylists()[0].id).isEqualTo("2")

    // Update pl1 to higher lastPlayedAt
    repository.updateLastPlayed("1", 500L)
    assertThat(repository.getAllPlaylists()[0].id).isEqualTo("1")
  }

  @Test
  fun `clearAllPlaylists empties repository`() {
    repository.savePlaylist(CustomPlaylist(id = "1", name = "First"))
    assertThat(repository.getAllPlaylists()).isNotEmpty()

    repository.clearAllPlaylists()
    assertThat(repository.getAllPlaylists()).isEmpty()
  }

  private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()
    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
      @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
      private val pending = mutableMapOf<String, Any>()
      private val removed = mutableSetOf<String>()
      private var clear = false
      override fun putString(k: String?, v: String?) = apply { if (k != null && v != null) { pending[k] = v; removed.remove(k) } }
      override fun putStringSet(k: String?, v: MutableSet<String>?) = apply { if (k != null && v != null) { pending[k] = v; removed.remove(k) } }
      override fun putInt(k: String?, v: Int) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putLong(k: String?, v: Long) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putFloat(k: String?, v: Float) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putBoolean(k: String?, v: Boolean) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun remove(k: String?) = apply { if (k != null) { pending.remove(k); removed.add(k) } }
      override fun clear() = apply { clear = true }
      override fun commit(): Boolean { apply(); return true }
      override fun apply() { if (clear) values.clear(); for (k in removed) values.remove(k); values.putAll(pending) }
    }
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
  }
}
