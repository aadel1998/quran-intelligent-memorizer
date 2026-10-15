package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Represents a single sequence of Ayahs from a Surah in a custom playlist.
 *
 * @param sura The Surah number (1-114).
 * @param startAyah The starting Ayah number in the segment.
 * @param endAyah The ending Ayah number in the segment.
 * @param isFullSurah True if this segment spans the full Surah.
 * @param customLabel Optional display label (e.g. "Al-Hamd", "Al-Baqarah 1–7").
 */
@Parcelize
data class PlaylistSegment(
  val sura: Int,
  val startAyah: Int = 1,
  val endAyah: Int = 1,
  val isFullSurah: Boolean = true,
  val customLabel: String = ""
) : Parcelable {

  init {
    require(sura in 1..114) { "sura must be between 1 and 114" }
    require(startAyah in 1..endAyah) { "startAyah ($startAyah) must be <= endAyah ($endAyah)" }
  }

  /**
   * Generates all [SuraAyah] instances in this segment in sequential order.
   */
  fun toAyahs(): List<SuraAyah> {
    return (startAyah..endAyah).map { SuraAyah(sura, it) }
  }

  /**
   * Serializes to a compact delimiter string: "sura:startAyah-endAyah:isFullSurah:customLabel"
   */
  fun serialize(): String {
    val escapedLabel = customLabel.replace(":", " ").replace("|", " ").replace(";", " ")
    return "$sura:$startAyah-$endAyah:$isFullSurah:$escapedLabel"
  }

  companion object {
    fun createFullSurah(sura: Int, quranInfo: QuranInfo, label: String = ""): PlaylistSegment {
      val totalAyahs = quranInfo.getNumberOfAyahs(sura).coerceAtLeast(1)
      return PlaylistSegment(
        sura = sura,
        startAyah = 1,
        endAyah = totalAyahs,
        isFullSurah = true,
        customLabel = label
      )
    }

    fun deserialize(value: String): PlaylistSegment? {
      return try {
        val parts = value.split(":")
        if (parts.size < 3) return null
        val sura = parts[0].toInt()
        val range = parts[1].split("-")
        val start = range[0].toInt()
        val end = range[1].toInt()
        val isFull = parts[2].toBoolean()
        val label = if (parts.size > 3) parts[3] else ""
        PlaylistSegment(
          sura = sura,
          startAyah = start,
          endAyah = end,
          isFullSurah = isFull,
          customLabel = label
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}
