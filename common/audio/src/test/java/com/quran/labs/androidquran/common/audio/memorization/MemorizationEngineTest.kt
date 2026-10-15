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

  @Test
  fun `progressive masking keeps text visible on solo repeat 1 and masks on repeat 2 and connect`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 2,
      connectRepeat = 1,
      maskingLevel = MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.startMemorization(config)

    // Step 0 (Solo A1), Repeat 1: visible
    assertThat(engine.state.value.stepType).isEqualTo(StepType.SOLO)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Repeat 1 completes -> advances to Repeat 2: masked!
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(2)
    assertThat(engine.state.value.isTextMasked).isTrue()

    // Solo A1 completes -> Step 1 (Solo A2), Repeat 1: visible
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(engine.state.value.currentStepIndex).isEqualTo(1)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Repeat 1 completes -> Repeat 2: masked!
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(2)
    assertThat(engine.state.value.isTextMasked).isTrue()

    // Solo A2 completes -> Step 2 (Connect A1..A2): always masked!
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    assertThat(engine.state.value.stepType).isEqualTo(StepType.CONNECT)
    assertThat(engine.state.value.isTextMasked).isTrue()
  }

  @Test
  fun `recitation recording state and pause remaining update correctly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 1),
      singleRepeat = 1,
      delayMode = MemorizationDelayMode.FixedSeconds(5)
    )

    engine.startMemorization(config)
    assertThat(engine.state.value.isRecordingRecitation).isFalse()
    assertThat(engine.state.value.lastRecitationAudioPath).isNull()

    // Solo A1 completes -> advances to PAUSE_FOR_RECITATION step
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(engine.state.value.stepType).isEqualTo(StepType.PAUSE_FOR_RECITATION)

    engine.setRecordingRecitation(true)
    assertThat(engine.state.value.isRecordingRecitation).isTrue()

    engine.updatePauseRemaining(5)
    assertThat(engine.state.value.pauseRemainingSeconds).isEqualTo(5)
    assertThat(engine.state.value.hudSummary).contains("5s")
    assertThat(engine.state.value.hudSummary).contains("Recording Recitation")

    engine.updateLastRecitationPath("/cache/test.m4a")
    assertThat(engine.state.value.lastRecitationAudioPath).isEqualTo("/cache/test.m4a")
  }

  @Test
  fun `session completion logs mastered range to preferences`() {
    val prefs = MemorizationPreferences(FakeTestSharedPreferences())
    val engineWithPrefs = MemorizationEngine(quranInfo, prefs)

    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 1),
      windowSize = 1,
      singleRepeat = 1,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engineWithPrefs.startMemorization(config)
    assertThat(prefs.getMasteredRanges()).isEmpty()

    // Solo A1 finishes -> session completes!
    val next = engineWithPrefs.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(next).isNull()
    assertThat(engineWithPrefs.state.value.isSessionCompleted).isTrue()

    val mastered = prefs.getMasteredRanges()
    assertThat(mastered).hasSize(1)
    assertThat(mastered[0].startAyah).isEqualTo(SuraAyah(1, 1))
    assertThat(mastered[0].endAyah).isEqualTo(SuraAyah(1, 1))
    assertThat(mastered[0].repetitions).isEqualTo(2)
  }

  private class FakeTestSharedPreferences : android.content.SharedPreferences {
    private val values = mutableMapOf<String, Any>()
    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
      @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): android.content.SharedPreferences.Editor = object : android.content.SharedPreferences.Editor {
      private val pending = mutableMapOf<String, Any>()
      private val removed = mutableSetOf<String>()
      private var clear = false
      override fun putString(k: String?, v: String?) = apply { if (k != null && v != null) { pending[k] = v; removed.remove(k) } }
      override fun putStringSet(k: String?, v: MutableSet<String>?) = apply { if (k != null && v != null) { pending[k] = v; removed.remove(k) } }
      override fun putInt(k: String?, v: Int) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putLong(k: String?, v: Long) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putFloat(k: String?, v: Float) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putBoolean(k: String?, v: Boolean) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun remove(k: String?) = apply { if (k != null) { pending.remove(k); removed.add(k) } }
      override fun clear() = apply { clear = true }
      override fun commit(): Boolean { apply(); return true }
      override fun apply() { if (clear) values.clear(); for (k in removed) values.remove(k); values.putAll(pending) }
    }
    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
  }
}
