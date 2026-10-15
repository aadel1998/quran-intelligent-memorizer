package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * The phase/type of an active memorization step.
 */
enum class StepType {
  SOLO,
  CONNECT,
  PAUSE_FOR_RECITATION,
  BRIDGE
}

/**
 * Represents a single atomic execution step in the memorization plan.
 *
 * @param stepIndex 0-based sequential index within the entire session plan.
 * @param windowIndex 0-based index of the current window chunk.
 * @param totalWindows Total count of windows in the plan.
 * @param stepType Type of execution ('SOLO', 'CONNECT', 'PAUSE_FOR_RECITATION', 'BRIDGE').
 * @param ayahs Ordered list of Ayahs to be played in this step.
 * @param targetRepeatCount Number of times this step sequence should repeat before advancing.
 * @param delayMode Delay mode configured for this step.
 * @param isMaskedByDefault Whether the Ayah text should be blurred/masked during this step in Memory Test Mode.
 * @param description Human-readable label for HUD display (e.g. "Connecting Ayah 1–3 (Repeat 1/2)").
 */
@Parcelize
data class MemorizationStep(
  val stepIndex: Int,
  val windowIndex: Int,
  val totalWindows: Int,
  val stepType: StepType,
  val ayahs: List<SuraAyah>,
  val targetRepeatCount: Int,
  val delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
  val isMaskedByDefault: Boolean = false,
  val description: String = ""
) : Parcelable
