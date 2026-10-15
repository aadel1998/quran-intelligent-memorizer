package com.quran.labs.androidquran.common.audio.memorization

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MemorizationEngineTest {

  private lateinit var quranInfo: QuranInfo
  private lateinit var engine: MemorizationEngine

  @Before
  fun setup() {
    quranInfo = QuranInfo(MadaniDataSource())
    engine = MemorizationEngine(quranInfo)
  }

  @Test
  fun `initial state is inactive`() {
    val state = engine.state.value
    assertThat(state.isActive).isFalse()
    assertThat(state.currentStep).isNull()
    assertThat(state.isSessionCompleted).isFalse()
  }

  @Test
  fun `startMemorization transitions to active state at first step`() = runTest {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 2,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.state.test {
      val initial = awaitItem()
      assertThat(initial.isActive).isFalse()

      engine.startMemorization(config)

      val activeState = awaitItem()
      assertThat(activeState.isActive).isTrue()
      assertThat(activeState.currentWindowIndex).isEqualTo(0)
      assertThat(activeState.currentStepIndex).isEqualTo(0)
      assertThat(activeState.stepType).isEqualTo(StepType.SOLO)
      assertThat(activeState.currentPlayingAyah).isEqualTo(SuraAyah(1, 1))
      assertThat(activeState.currentRepeatIteration).isEqualTo(1)
      assertThat(activeState.totalRepeatIterations).isEqualTo(2)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onAyahPlaybackCompleted iterates repeats before advancing step`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 2,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.startMemorization(config)

    // Step 0 is A1 Solo (repeat 2 times). Current iteration is 1.
    assertThat(engine.state.value.currentStepIndex).isEqualTo(0)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)

    // Complete play 1 of Step 0 -> should repeat A1 (iteration 2)
    val nextAfterPlay1 = engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(nextAfterPlay1).isEqualTo(SuraAyah(1, 1))
    assertThat(engine.state.value.currentStepIndex).isEqualTo(0)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(2)

    // Complete play 2 of Step 0 -> Step 0 finished! Advance to Step 1 (A2 Solo)
    val nextAfterPlay2 = engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(nextAfterPlay2).isEqualTo(SuraAyah(1, 2))
    assertThat(engine.state.value.currentStepIndex).isEqualTo(1)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)
    assertThat(engine.state.value.stepType).isEqualTo(StepType.SOLO)
  }

  @Test
  fun `CONNECT step sequences through sub-ayahs then repeats`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.startMemorization(config)

    // Complete Step 0 (A1 Solo x 1)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    // Complete Step 1 (A2 Solo x 1)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))

    // Now at Step 2 (Connect A1..A2, repeat 2 times)
    val state = engine.state.value
    assertThat(state.currentStepIndex).isEqualTo(2)
    assertThat(state.stepType).isEqualTo(StepType.CONNECT)
    assertThat(state.activeAyahsInStep).containsExactly(SuraAyah(1, 1), SuraAyah(1, 2)).inOrder()
    assertThat(state.currentPlayingAyah).isEqualTo(SuraAyah(1, 1))
    assertThat(state.currentRepeatIteration).isEqualTo(1)

    // Ayah 1 of Connect finishes -> should advance to Ayah 2 (same repeat iteration 1)
    val nextSubAyah = engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(nextSubAyah).isEqualTo(SuraAyah(1, 2))
    assertThat(engine.state.value.currentSubAyahIndex).isEqualTo(1)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)

    // Ayah 2 of Connect finishes -> Repeat 1 complete! Start Repeat 2 at Ayah 1
    val nextRepeatAyah = engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    assertThat(nextRepeatAyah).isEqualTo(SuraAyah(1, 1))
    assertThat(engine.state.value.currentSubAyahIndex).isEqualTo(0)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(2)
  }

  @Test
  fun `skipCurrentStep and previousStep navigate plan cleanly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 3),
      windowSize = 3,
      singleRepeat = 3,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.startMemorization(config)
    assertThat(engine.state.value.currentStepIndex).isEqualTo(0)

    // Skip to Step 1
    engine.skipCurrentStep()
    assertThat(engine.state.value.currentStepIndex).isEqualTo(1)
    assertThat(engine.state.value.currentPlayingAyah).isEqualTo(SuraAyah(1, 2))

    // Skip to Step 2
    engine.skipCurrentStep()
    assertThat(engine.state.value.currentStepIndex).isEqualTo(2)

    // Previous step back to Step 1
    engine.previousStep()
    assertThat(engine.state.value.currentStepIndex).isEqualTo(1)
    assertThat(engine.state.value.currentPlayingAyah).isEqualTo(SuraAyah(1, 2))
  }

  @Test
  fun `pause and resume update state accordingly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 1
    )

    engine.startMemorization(config)
    assertThat(engine.state.value.isPaused).isFalse()

    engine.pause()
    assertThat(engine.state.value.isPaused).isTrue()

    // When paused, onAyahPlaybackCompleted returns null and does not advance
    val resultWhilePaused = engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(resultWhilePaused).isNull()

    engine.resume()
    assertThat(engine.state.value.isPaused).isFalse()
  }

  @Test
  fun `memoryTestMode enables text masking during connect steps with tap-to-peek`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 1,
      memoryTestMode = true
    )

    engine.startMemorization(config)
    // Step 0: Solo -> text not masked
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Complete Step 0 -> Step 1 (Solo A2)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Complete Step 1 -> Step 2 (Connect A1..A2)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    assertThat(engine.state.value.stepType).isEqualTo(StepType.CONNECT)
    // In Memory Test Mode, Connect step has isTextMasked = true
    assertThat(engine.state.value.isTextMasked).isTrue()

    // Tap to peek (toggle mask)
    engine.toggleTextMask()
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Tap to re-mask
    engine.toggleTextMask()
    assertThat(engine.state.value.isTextMasked).isTrue()
  }

  @Test
  fun `completing all steps marks session completed`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 1,
      enableBridge = false
    )

    engine.startMemorization(config)

    // Complete Step 0 (Solo A1)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    // Complete Step 1 (Solo A2)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    // Step 2 (Connect A1..A2) -> Play A1
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    // Step 2 -> Play A2 -> FINAL STEP COMPLETE!
    val finalResult = engine.onAyahPlaybackCompleted(SuraAyah(1, 2))

    assertThat(finalResult).isNull()
    assertThat(engine.state.value.isSessionCompleted).isTrue()
    assertThat(engine.state.value.isActive).isFalse()
  }

  @Test
  fun `stopMemorization resets engine cleanly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 5)
    )

    engine.startMemorization(config)
    assertThat(engine.state.value.isActive).isTrue()

    engine.stopMemorization()
    assertThat(engine.state.value.isActive).isFalse()
    assertThat(engine.state.value.currentStep).isNull()
  }
}
