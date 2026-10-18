package com.quran.labs.androidquran.common.audio.memorization

import com.quran.data.core.QuranInfo
import com.quran.data.di.AppScope
import com.quran.data.model.SuraAyah
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core state machine for the Intelligent Quran Memorization Mode.
 *
 * Implements a progressive sliding-window learning algorithm:
 * - Repeats each new Ayah individually (Rs)
 * - Consecutively connects learned Ayahs in the window (Rc)
 * - Optionally triggers silent recitation pauses
 * - Slides window across the selected range with an optional connecting bridge step
 * - Supports Memory Test Mode (blurring/masking text with tap-to-peek)
 */
@SingleIn(AppScope::class)
class MemorizationEngine @Inject constructor(
  private val quranInfo: QuranInfo,
  private val memorizationPreferences: MemorizationPreferences? = null
) {

  private val _state = MutableStateFlow(MemorizationState())
  val state: StateFlow<MemorizationState> = _state.asStateFlow()

  private var plan: List<MemorizationStep> = emptyList()

  /**
   * Initializes and starts a new memorization session.
   */
  fun startMemorization(config: MemorizationConfig) {
    plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)
    if (plan.isEmpty()) {
      _state.value = MemorizationState(isActive = false)
      return
    }

    val firstStep = plan[0]
    val firstAyah = firstStep.ayahs.firstOrNull()

    memorizationPreferences?.saveSession(config, 0, 1)

    _state.value = MemorizationState(
      isActive = true,
      isPaused = false,
      config = config,
      currentWindowIndex = firstStep.windowIndex,
      totalWindows = firstStep.totalWindows,
      currentStepIndex = 0,
      totalSteps = plan.size,
      currentStep = firstStep,
      currentRepeatIteration = 1,
      totalRepeatIterations = firstStep.targetRepeatCount,
      activeAyahsInStep = firstStep.ayahs,
      currentPlayingAyah = firstAyah,
      currentSubAyahIndex = 0,
      isTextMasked = shouldMaskText(firstStep, 1, config),
      pauseRemainingSeconds = 0,
      isRecordingRecitation = false,
      lastRecitationAudioPath = null,
      isSessionCompleted = false
    )
  }

  /**
   * Resumes the previously saved memorization session if one exists.
   */
  fun resumeSavedSession(): Boolean {
    val savedConfig = memorizationPreferences?.getSavedConfig() ?: return false
    val savedStepIdx = memorizationPreferences.getSavedStepIndex()

    startMemorization(savedConfig)
    if (savedStepIdx in plan.indices) {
      goToStep(savedStepIdx)
    }
    return true
  }

  /**
   * Resumes the previously saved memorization session for a specific Surah if one exists.
   */
  fun resumeSavedSessionForSura(sura: Int): Boolean {
    val suraSession = memorizationPreferences?.getSavedSessionForSura(sura) ?: return false
    startMemorization(suraSession.config)
    if (suraSession.stepIndex in plan.indices) {
      goToStep(suraSession.stepIndex)
    }
    return true
  }

  /**
   * Pauses the active memorization session.
   */
  fun pause() {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isPaused = true)
  }

  /**
   * Resumes playback in the active memorization session.
   */
  fun resume() {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isPaused = false)
  }

  /**
   * Skips directly to the next memorization step in the plan.
   */
  fun skipCurrentStep() {
    val current = _state.value
    if (!current.isActive || plan.isEmpty()) return

    val nextStepIdx = current.currentStepIndex + 1
    if (nextStepIdx >= plan.size) {
      completeSession()
    } else {
      goToStep(nextStepIdx)
    }
  }

  /**
   * Returns to the previous memorization step in the plan.
   */
  fun previousStep() {
    val current = _state.value
    if (!current.isActive || plan.isEmpty()) return

    val prevStepIdx = (current.currentStepIndex - 1).coerceAtLeast(0)
    goToStep(prevStepIdx)
  }

  /**
   * Updates the session configuration live during playback without losing the current Ayah position.
   */
  fun updateConfig(newConfig: MemorizationConfig) {
    val current = _state.value
    if (!current.isActive) return

    val currentAyah = current.currentPlayingAyah ?: current.activeAyahsInStep.firstOrNull()
    val newPlan = MemorizationPlanBuilder.buildPlan(newConfig, quranInfo)
    if (newPlan.isEmpty()) return

    plan = newPlan
    val targetIndex = if (currentAyah != null) {
      val sameStepAndAyah = newPlan.indexOfFirst { step ->
        step.stepType == current.currentStep?.stepType && currentAyah in step.ayahs
      }
      if (sameStepAndAyah >= 0) {
        sameStepAndAyah
      } else {
        val anyWithAyah = newPlan.indexOfFirst { step -> currentAyah in step.ayahs }
        if (anyWithAyah >= 0) anyWithAyah else 0
      }
    } else {
      current.currentStepIndex.coerceIn(newPlan.indices)
    }

    val step = newPlan[targetIndex]
    memorizationPreferences?.saveSession(newConfig, targetIndex, 1)

    _state.value = current.copy(
      config = newConfig,
      currentWindowIndex = step.windowIndex,
      totalWindows = step.totalWindows,
      currentStepIndex = targetIndex,
      totalSteps = newPlan.size,
      currentStep = step,
      currentRepeatIteration = 1,
      totalRepeatIterations = step.targetRepeatCount,
      activeAyahsInStep = step.ayahs,
      currentPlayingAyah = currentAyah ?: step.ayahs.firstOrNull(),
      isTextMasked = shouldMaskText(step, 1, newConfig)
    )
  }

  /**
   * Terminates the memorization session and returns to idle state.
   */
  fun stopMemorization() {
    memorizationPreferences?.clearSession()
    plan = emptyList()
    _state.value = MemorizationState(isActive = false)
  }

  /**
   * Toggles text masking/blurring during memory test mode (tap-to-peek).
   */
  fun toggleTextMask() {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isTextMasked = !current.isTextMasked)
  }

  /**
   * Sets text mask visibility explicitly.
   */
  fun setTextMasked(masked: Boolean) {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isTextMasked = masked)
  }

  /**
   * Updates the remaining countdown seconds during a recitation pause step.
   */
  fun updatePauseRemaining(seconds: Int) {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(pauseRemainingSeconds = seconds)
  }

  /**
   * Sets whether user recitation is actively being recorded.
   */
  fun setRecordingRecitation(isRecording: Boolean) {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isRecordingRecitation = isRecording)
  }

  /**
   * Updates the file path of the last recorded recitation.
   */
  fun updateLastRecitationPath(path: String?) {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(lastRecitationAudioPath = path)
  }

  /**
   * Determines whether Quranic text should be visually masked based on configuration and iteration.
   */
  private fun shouldMaskText(step: MemorizationStep, repeatIteration: Int, config: MemorizationConfig?): Boolean {
    if (config == null) return false
    return when (config.maskingLevel) {
      MaskingLevel.OFF -> false
      MaskingLevel.MASK_ON_CONNECT -> step.isMaskedByDefault
      MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT -> {
        if (step.stepType == StepType.SOLO) {
          repeatIteration > 1
        } else {
          true
        }
      }
    }
  }

  /**
   * Called when audio playback of an Ayah finishes.
   * Advances sub-ayah index within a CONNECT step, or advances repeat iteration,
   * or transitions to the next step.
   *
   * @param completedAyah The Ayah that just finished playing.
   * @return The next [SuraAyah] to play, or null if the session is paused/completed.
   */
  fun onAyahPlaybackCompleted(completedAyah: SuraAyah): SuraAyah? {
    val current = _state.value
    if (!current.isActive || current.isPaused || current.currentStep == null) return null

    val step = current.currentStep
    val subIndex = current.currentSubAyahIndex

    // Check if there are remaining Ayahs in the current step sequence (e.g. CONNECT sequence)
    if (subIndex + 1 < step.ayahs.size) {
      val nextSubIndex = subIndex + 1
      val nextAyah = step.ayahs[nextSubIndex]
      _state.value = current.copy(
        currentSubAyahIndex = nextSubIndex,
        currentPlayingAyah = nextAyah
      )
      return nextAyah
    }

    // Finished a full sequence of this step. Check repetitions.
    if (current.currentRepeatIteration < current.totalRepeatIterations) {
      val nextRepeat = current.currentRepeatIteration + 1
      val firstAyah = step.ayahs.first()
      _state.value = current.copy(
        currentRepeatIteration = nextRepeat,
        currentSubAyahIndex = 0,
        currentPlayingAyah = firstAyah,
        isTextMasked = shouldMaskText(step, nextRepeat, current.config)
      )
      current.config?.let {
        memorizationPreferences?.saveSession(it, current.currentStepIndex, nextRepeat)
      }
      return firstAyah
    }

    // Step fully completed! Advance to next step in plan.
    val nextStepIndex = current.currentStepIndex + 1
    if (nextStepIndex >= plan.size) {
      completeSession()
      return null
    }

    goToStep(nextStepIndex)
    return _state.value.currentPlayingAyah
  }

  private fun goToStep(stepIndex: Int) {
    if (stepIndex !in plan.indices) return
    val nextStep = plan[stepIndex]
    val firstAyah = nextStep.ayahs.firstOrNull()
    val config = _state.value.config

    if (config != null) {
      memorizationPreferences?.saveSession(config, stepIndex, 1)
    }

    _state.value = _state.value.copy(
      currentWindowIndex = nextStep.windowIndex,
      totalWindows = nextStep.totalWindows,
      currentStepIndex = stepIndex,
      currentStep = nextStep,
      currentRepeatIteration = 1,
      totalRepeatIterations = nextStep.targetRepeatCount,
      activeAyahsInStep = nextStep.ayahs,
      currentPlayingAyah = firstAyah,
      currentSubAyahIndex = 0,
      isTextMasked = shouldMaskText(nextStep, 1, config),
      pauseRemainingSeconds = 0,
      isSessionCompleted = false
    )
  }

  private fun completeSession() {
    val current = _state.value
    current.config?.let { config ->
      memorizationPreferences?.saveMasteredRange(
        MasteredRange(
          startAyah = config.startAyah,
          endAyah = config.endAyah,
          timestamp = System.currentTimeMillis(),
          repetitions = config.singleRepeat + config.connectRepeat
        )
      )
      val sura = config.startAyah.sura
      memorizationPreferences?.setLastRevised(sura, System.currentTimeMillis())
      if (config.startAyah.ayah == 1 && config.endAyah.ayah >= quranInfo.getNumberOfAyahs(sura)) {
        memorizationPreferences?.setSurahMemorized(sura, true)
      }
      memorizationPreferences?.clearSessionForSura(sura)
    }
    memorizationPreferences?.clearSession()
    _state.value = _state.value.copy(
      isActive = false,
      isSessionCompleted = true,
      currentStep = null,
      currentPlayingAyah = null,
      activeAyahsInStep = emptyList(),
      isTextMasked = false,
      isRecordingRecitation = false,
      lastRecitationAudioPath = null
    )
  }
}
