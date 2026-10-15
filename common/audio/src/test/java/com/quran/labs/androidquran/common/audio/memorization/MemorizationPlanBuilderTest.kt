package com.quran.labs.androidquran.common.audio.memorization

import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import org.junit.Before
import org.junit.Test

class MemorizationPlanBuilderTest {

  private lateinit var quranInfo: QuranInfo

  @Before
  fun setup() {
    quranInfo = QuranInfo(MadaniDataSource())
  }

  @Test
  fun `getAyahsInRange returns exact sequence within single sura`() {
    val start = SuraAyah(1, 1)
    val end = SuraAyah(1, 7)

    val ayahs = MemorizationPlanBuilder.getAyahsInRange(start, end, quranInfo)

    assertThat(ayahs).hasSize(7)
    assertThat(ayahs.first()).isEqualTo(SuraAyah(1, 1))
    assertThat(ayahs.last()).isEqualTo(SuraAyah(1, 7))
  }

  @Test
  fun `getAyahsInRange crosses sura boundary correctly`() {
    val start = SuraAyah(113, 4)
    val end = SuraAyah(114, 2)

    val ayahs = MemorizationPlanBuilder.getAyahsInRange(start, end, quranInfo)

    // Sura 113 has 5 ayahs (4, 5) -> 2 ayahs
    // Sura 114 has 6 ayahs (1, 2) -> 2 ayahs
    // Total 4 ayahs
    assertThat(ayahs).containsExactly(
      SuraAyah(113, 4),
      SuraAyah(113, 5),
      SuraAyah(114, 1),
      SuraAyah(114, 2)
    ).inOrder()
  }

  @Test
  fun `buildPlan produces correct sequence for window size 4 without delay`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 4),
      windowSize = 4,
      singleRepeat = 3,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)

    // Window [A1, A2, A3, A4]:
    // 1. A1 Solo (Rs = 3)
    // 2. A2 Solo (Rs = 3)
    // 3. Connect A1..A2 (Rc = 2)
    // 4. A3 Solo (Rs = 3)
    // 5. Connect A1..A3 (Rc = 2)
    // 6. A4 Solo (Rs = 3)
    // 7. Connect A1..A4 (Rc = 2)
    assertThat(plan).hasSize(7)

    // Step 0: A1 Solo
    assertThat(plan[0].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[0].ayahs).containsExactly(SuraAyah(1, 1))
    assertThat(plan[0].targetRepeatCount).isEqualTo(3)

    // Step 1: A2 Solo
    assertThat(plan[1].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[1].ayahs).containsExactly(SuraAyah(1, 2))
    assertThat(plan[1].targetRepeatCount).isEqualTo(3)

    // Step 2: Connect A1..A2
    assertThat(plan[2].stepType).isEqualTo(StepType.CONNECT)
    assertThat(plan[2].ayahs).containsExactly(SuraAyah(1, 1), SuraAyah(1, 2)).inOrder()
    assertThat(plan[2].targetRepeatCount).isEqualTo(2)

    // Step 3: A3 Solo
    assertThat(plan[3].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[3].ayahs).containsExactly(SuraAyah(1, 3))
    assertThat(plan[3].targetRepeatCount).isEqualTo(3)

    // Step 4: Connect A1..A3
    assertThat(plan[4].stepType).isEqualTo(StepType.CONNECT)
    assertThat(plan[4].ayahs).containsExactly(SuraAyah(1, 1), SuraAyah(1, 2), SuraAyah(1, 3)).inOrder()
    assertThat(plan[4].targetRepeatCount).isEqualTo(2)

    // Step 5: A4 Solo
    assertThat(plan[5].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[5].ayahs).containsExactly(SuraAyah(1, 4))
    assertThat(plan[5].targetRepeatCount).isEqualTo(3)

    // Step 6: Connect A1..A4
    assertThat(plan[6].stepType).isEqualTo(StepType.CONNECT)
    assertThat(plan[6].ayahs).containsExactly(
      SuraAyah(1, 1), SuraAyah(1, 2), SuraAyah(1, 3), SuraAyah(1, 4)
    ).inOrder()
    assertThat(plan[6].targetRepeatCount).isEqualTo(2)
  }

  @Test
  fun `buildPlan includes recitation pauses when delayMode is active`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 2,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.FixedSeconds(5),
      memoryTestMode = true,
      enableBridge = false
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)

    // Expected for 2 ayahs with delay:
    // 0: A1 Solo
    // 1: Recitation Pause (A1)
    // 2: A2 Solo
    // 3: Recitation Pause (A2)
    // 4: Connect A1..A2
    // 5: Recitation Pause (A1..A2)
    assertThat(plan).hasSize(6)
    assertThat(plan[0].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[1].stepType).isEqualTo(StepType.PAUSE_FOR_RECITATION)
    assertThat(plan[1].isMaskedByDefault).isTrue()
    assertThat(plan[2].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[3].stepType).isEqualTo(StepType.PAUSE_FOR_RECITATION)
    assertThat(plan[4].stepType).isEqualTo(StepType.CONNECT)
    assertThat(plan[4].isMaskedByDefault).isTrue()
    assertThat(plan[5].stepType).isEqualTo(StepType.PAUSE_FOR_RECITATION)
  }

  @Test
  fun `buildPlan adds Bridge step between multiple windows`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 4),
      windowSize = 2, // 2 windows: [1, 2] and [3, 4]
      singleRepeat = 1,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = true
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)

    // Window 0 has [A1, A2] -> Solo A1, Solo A2, Connect A1..A2
    // Bridge step between Window 0 and Window 1: connects A2 and A3
    // Window 1 has [A3, A4] -> Solo A3, Solo A4, Connect A3..A4
    val bridgeSteps = plan.filter { it.stepType == StepType.BRIDGE }
    assertThat(bridgeSteps).hasSize(1)
    assertThat(bridgeSteps.first().ayahs).containsExactly(SuraAyah(1, 2), SuraAyah(1, 3)).inOrder()
    assertThat(bridgeSteps.first().targetRepeatCount).isEqualTo(1)
  }
}
