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
      isTextMasked = firstStep.isMaskedByDefault && config.memoryTestMode,
      pauseRemainingSeconds = 0,
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
        isTextMasked = step.isMaskedByDefault && (current.config?.memoryTestMode == true)
      )
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
      isTextMasked = nextStep.isMaskedByDefault && (config?.memoryTestMode == true),
      pauseRemainingSeconds = 0,
      isSessionCompleted = false
    )
  }

  private fun completeSession() {
    memorizationPreferences?.clearSession()
    _state.value = _state.value.copy(
      isActive = false,
      isSessionCompleted = true,
      currentStep = null,
      currentPlayingAyah = null,
      activeAyahsInStep = emptyList(),
      isTextMasked = false
    )
  }
}
