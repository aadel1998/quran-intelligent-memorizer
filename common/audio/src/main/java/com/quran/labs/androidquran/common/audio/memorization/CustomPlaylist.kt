package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Represents a user-defined playlist of Quran segments across one or more Surahs.
 *
 * @param id Unique identifier (UUID).
 * @param name User-assigned name for the playlist (e.g. "Daily Recitation").
 * @param segments Ordered list of [PlaylistSegment] instances forming this playlist.
 * @param createdAt Creation epoch millisecond timestamp.
 * @param lastPlayedAt Epoch timestamp when this playlist was last played or memorized.
 */
@Parcelize
data class CustomPlaylist(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val segments: List<PlaylistSegment> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val lastPlayedAt: Long = 0L
) : Parcelable {

  /**
   * Flattens all segments into an ordered sequence of [SuraAyah]s.
   */
  fun toFlattenedAyahs(): List<SuraAyah> {
    return segments.flatMap { it.toAyahs() }
  }

  /**
   * Total count of individual Ayahs across all segments.
   */
  val totalAyahsCount: Int
    get() = segments.sumOf { (it.endAyah - it.startAyah + 1).coerceAtLeast(0) }

  /**
   * Display summary string (e.g. "7 segments • 35 Ayahs").
   */
  val summary: String
    get() = "${segments.size} segment${if (segments.size == 1) "" else "s"} • $totalAyahsCount Ayahs"

  /**
   * Serializes playlist to delimiter string format.
   */
  fun serialize(): String {
    val safeName = name.replace("##", " ").replace("\n", " ").replace(";;", " ")
    val segs = segments.joinToString(",") { it.serialize() }
    return "$id##$safeName##$createdAt##$lastPlayedAt##$segs"
  }

  companion object {
    fun deserialize(value: String): CustomPlaylist? {
      return try {
        val parts = value.split("##")
        if (parts.size < 4) return null
        val id = parts[0]
        val name = parts[1]
        val createdAt = parts[2].toLong()
        val lastPlayedAt = parts[3].toLong()
        val segs = if (parts.size > 4 && parts[4].isNotBlank()) {
          parts[4].split(",").mapNotNull { PlaylistSegment.deserialize(it) }
        } else {
          emptyList()
        }
        CustomPlaylist(
          id = id,
          name = name,
          segments = segs,
          createdAt = createdAt,
          lastPlayedAt = lastPlayedAt
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}
