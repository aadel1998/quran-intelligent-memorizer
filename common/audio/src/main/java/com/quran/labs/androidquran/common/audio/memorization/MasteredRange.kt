package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Represents a completed memorization range logged in mastery history.
 *
 * @param startAyah The first Ayah in the mastered range.
 * @param endAyah The last Ayah in the mastered range.
 * @param timestamp Epoch milliseconds when the range was completed.
 * @param repetitions Total repetitions completed across the session.
 */
@Parcelize
data class MasteredRange(
  val startAyah: SuraAyah,
  val endAyah: SuraAyah,
  val timestamp: Long,
  val repetitions: Int
) : Parcelable {

  /**
   * Serializes this range to a compact string format:
   * "startSura:startAyah-endSura:endAyah@timestamp#repetitions"
   */
  fun serialize(): String =
    "${startAyah.sura}:${startAyah.ayah}-${endAyah.sura}:${endAyah.ayah}@$timestamp#$repetitions"

  companion object {
    /**
     * Parses a serialized string into a [MasteredRange], or returns null if invalid.
     */
    fun deserialize(value: String): MasteredRange? {
      return try {
        val atParts = value.split("@")
        if (atParts.size != 2) return null
        val rangePart = atParts[0]
        val metaPart = atParts[1]

        val rangeParts = rangePart.split("-")
        if (rangeParts.size != 2) return null

        val startParts = rangeParts[0].split(":")
        val endParts = rangeParts[1].split(":")
        if (startParts.size != 2 || endParts.size != 2) return null

        val metaParts = metaPart.split("#")
        val timestamp = metaParts[0].toLong()
        val reps = if (metaParts.size > 1) metaParts[1].toInt() else 1

        MasteredRange(
          startAyah = SuraAyah(startParts[0].toInt(), startParts[1].toInt()),
          endAyah = SuraAyah(endParts[0].toInt(), endParts[1].toInt()),
          timestamp = timestamp,
          repetitions = reps
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}
