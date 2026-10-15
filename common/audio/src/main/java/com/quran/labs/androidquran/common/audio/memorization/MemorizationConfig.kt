package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Configuration options for the Intelligent Quran Memorization Mode.
 *
 * @param startAyah The first Ayah in the memorization session.
 * @param endAyah The last Ayah in the memorization session.
 * @param windowSize Number of Ayahs in each active memorization chunk (default 4).
 * @param singleRepeat Number of times each new Ayah is repeated individually (Rs, default 3).
 * @param connectRepeat Number of times accumulated Ayahs in the window are repeated together (Rc, default 2).
 * @param delayMode Pause between repetitions (None, FixedSeconds, or MatchAyahDuration).
 * @param memoryTestMode When true, masks/blurs Ayah text during CONNECT and recitation steps.
 * @param enableBridge When true, plays a bridge step connecting the previous window with the new window before advancing.
 */
@Parcelize
data class MemorizationConfig(
  val startAyah: SuraAyah,
  val endAyah: SuraAyah,
  val windowSize: Int = 4,
  val singleRepeat: Int = 3,
  val connectRepeat: Int = 2,
  val delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
  val memoryTestMode: Boolean = false,
  val enableBridge: Boolean = true
) : Parcelable {
  init {
    require(windowSize >= 1) { "windowSize must be at least 1" }
    require(singleRepeat >= 1) { "singleRepeat must be at least 1" }
    require(connectRepeat >= 1) { "connectRepeat must be at least 1" }
    require(startAyah <= endAyah) { "startAyah ($startAyah) must be <= endAyah ($endAyah)" }
  }
}
