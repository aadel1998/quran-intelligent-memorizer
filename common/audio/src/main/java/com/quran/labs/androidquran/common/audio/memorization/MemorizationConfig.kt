package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Defines progressive masking levels for memory scaffolding.
 */
enum class MaskingLevel {
  OFF,
  MASK_ON_CONNECT,
  PROGRESSIVE_SOLO_AND_CONNECT
}

/**
 * Configuration options for the Intelligent Quran Memorization Mode.
 *
 * @param startAyah The first Ayah in the memorization session (or first ayah of first segment).
 * @param endAyah The last Ayah in the memorization session (or last ayah of last segment).
 * @param windowSize Number of Ayahs in each active memorization chunk (default 4).
 * @param singleRepeat Number of times each new Ayah is repeated individually (Rs, default 3).
 * @param connectRepeat Number of times accumulated Ayahs in the window are repeated together (Rc, default 2).
 * @param delayMode Pause between repetitions (None, FixedSeconds, or MatchAyahDuration).
 * @param memoryTestMode When true, masks/blurs Ayah text during CONNECT and recitation steps.
 * @param maskingLevel Progressive masking difficulty (OFF, MASK_ON_CONNECT, PROGRESSIVE_SOLO_AND_CONNECT).
 * @param recordRecitation When true, records user's voice recitation during pauses for self-check replay.
 * @param enableBridge When true, plays a bridge step connecting the previous window with the new window before advancing.
 * @param segments Optional list of [PlaylistSegment]s if this session is driven by a custom cross-Surah playlist.
 * @param playlistId Optional ID of the custom playlist being memorized.
 * @param playlistName Optional name of the custom playlist being memorized.
 */
@Parcelize
data class MemorizationConfig(
  val startAyah: SuraAyah = SuraAyah(1, 1),
  val endAyah: SuraAyah = SuraAyah(1, 1),
  val windowSize: Int = 4,
  val singleRepeat: Int = 3,
  val connectRepeat: Int = 2,
  val delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
  val memoryTestMode: Boolean = false,
  val maskingLevel: MaskingLevel = if (memoryTestMode) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF,
  val recordRecitation: Boolean = false,
  val enableBridge: Boolean = true,
  val segments: List<PlaylistSegment> = emptyList(),
  val playlistId: String? = null,
  val playlistName: String? = null,
  val isIntelliJMode: Boolean = true,
  val rangeRepeat: Int = 1
) : Parcelable {
  init {
    require(windowSize >= 1) { "windowSize must be at least 1" }
    require(singleRepeat >= 1) { "singleRepeat must be at least 1" }
    require(connectRepeat >= 0) { "connectRepeat must be at least 0" }
    if (segments.isEmpty()) {
      require(startAyah <= endAyah) { "startAyah ($startAyah) must be <= endAyah ($endAyah)" }
    }
  }

  /**
   * Returns true if this configuration is driven by a cross-Surah custom playlist.
   */
  val isCustomPlaylist: Boolean
    get() = segments.isNotEmpty()
}
