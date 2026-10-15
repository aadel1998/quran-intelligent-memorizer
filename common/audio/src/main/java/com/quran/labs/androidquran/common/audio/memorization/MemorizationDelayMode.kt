package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Delay mode between Ayah repetitions allowing silent recitation or pause.
 */
@Parcelize
sealed class MemorizationDelayMode : Parcelable {
  /**
   * No pause between repetitions.
   */
  @Parcelize
  data object None : MemorizationDelayMode()

  /**
   * Fixed pause duration in seconds after each repetition.
   */
  @Parcelize
  data class FixedSeconds(val seconds: Int) : MemorizationDelayMode()

  /**
   * Wait for the exact playback duration of the just-played Ayah
   * so the user can recite it silently with matching timing.
   */
  @Parcelize
  data object MatchAyahDuration : MemorizationDelayMode()
}
