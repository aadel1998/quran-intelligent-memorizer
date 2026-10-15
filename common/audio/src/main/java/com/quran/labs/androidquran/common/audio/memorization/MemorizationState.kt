package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Public state exposed by the MemorizationEngine for UI presentation and observation.
 */
@Parcelize
data class MemorizationState(
  val isActive: Boolean = false,
  val isPaused: Boolean = false,
  val config: MemorizationConfig? = null,
  val currentWindowIndex: Int = 0,
  val totalWindows: Int = 0,
  val currentStepIndex: Int = 0,
  val totalSteps: Int = 0,
  val currentStep: MemorizationStep? = null,
  val currentRepeatIteration: Int = 1,
  val totalRepeatIterations: Int = 1,
  val activeAyahsInStep: List<SuraAyah> = emptyList(),
  val currentPlayingAyah: SuraAyah? = null,
  val currentSubAyahIndex: Int = 0,
  val isTextMasked: Boolean = false,
  val pauseRemainingSeconds: Int = 0,
  val isRecordingRecitation: Boolean = false,
  val lastRecitationAudioPath: String? = null,
  val isSessionCompleted: Boolean = false
) : Parcelable {
  /**
   * The active stepType ('SOLO' | 'CONNECT' | 'PAUSE_FOR_RECITATION' | 'BRIDGE'), or null if idle.
   */
  val stepType: StepType? get() = currentStep?.stepType

  /**
   * Formatted HUD summary string (e.g. "Window 1/3 • Connecting Ayah 1–3 (Repeat 2/2)").
   */
  val hudSummary: String
    get() {
      if (!isActive || currentStep == null) return ""
      val windowPart = "Window ${currentWindowIndex + 1}/$totalWindows"
      val repeatPart = "Repeat $currentRepeatIteration/$totalRepeatIterations"
      val typeLabel = when (stepType) {
        StepType.SOLO -> "Solo Ayah ${currentPlayingAyah?.ayah ?: ""}"
        StepType.CONNECT -> {
          val first = activeAyahsInStep.firstOrNull()?.ayah ?: ""
          val last = activeAyahsInStep.lastOrNull()?.ayah ?: ""
          "Connecting Ayah $first–$last"
        }
        StepType.PAUSE_FOR_RECITATION -> {
          val remaining = if (pauseRemainingSeconds > 0) " (${pauseRemainingSeconds}s)" else ""
          if (isRecordingRecitation) "Recording Recitation$remaining" else "Recite Ayah$remaining"
        }
        StepType.BRIDGE -> "Bridge Window ${currentWindowIndex + 1} ➔ ${currentWindowIndex + 2}"
        null -> ""
      }
      return "$windowPart • $typeLabel ($repeatPart)"
    }
}
