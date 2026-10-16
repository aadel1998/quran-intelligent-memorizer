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
  fun `buildPlan connects across sliding window boundaries naturally without gaps`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 4),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = true
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)

    // With a true sliding window of size 2 across 4 ayahs:
    // j = 0 (A1): Solo A1
    // j = 1 (A2): Solo A2, Connect A1..A2
    // j = 2 (A3): Solo A3, Connect A2..A3 (slid forward!)
    // j = 3 (A4): Solo A4, Connect A3..A4 (slid forward!)
    // Total steps: 1 + 2 + 2 + 2 = 7 steps
    assertThat(plan).hasSize(7)

    val soloSteps = plan.filter { it.stepType == StepType.SOLO }
    assertThat(soloSteps).hasSize(4)

    val connectSteps = plan.filter { it.stepType == StepType.CONNECT }
    assertThat(connectSteps).hasSize(3)

    // Connect step 1: A1, A2
    assertThat(connectSteps[0].ayahs).containsExactly(SuraAyah(1, 1), SuraAyah(1, 2)).inOrder()
    // Connect step 2: A2, A3 (bridges window across A2 and A3!)
    assertThat(connectSteps[1].ayahs).containsExactly(SuraAyah(1, 2), SuraAyah(1, 3)).inOrder()
    // Connect step 3: A3, A4 (bridges window across A3 and A4!)
    assertThat(connectSteps[2].ayahs).containsExactly(SuraAyah(1, 3), SuraAyah(1, 4)).inOrder()
  }

  @Test
  fun `buildPlan with custom cross-surah segments flattens across multiple surahs`() {
    // User's exact example:
    // 1: Al-Hamd (1:1-7)
    // 2: Al-Baqarah (2:1-7)
    // 3: Al-Ikhlas (112:1-4)
    // 4: Al-Kafirun (109:1-6)
    // 5: Al-Falaq (113:1-5)
    // 6: An-Nas (114:1-6)
    // 7: Al-Baqarah (2:270-286)
    val segments = listOf(
      PlaylistSegment.createFullSurah(1, quranInfo, "Al-Hamd"),
      PlaylistSegment(sura = 2, startAyah = 1, endAyah = 7, isFullSurah = false, customLabel = "Al-Baqarah 1-7"),
      PlaylistSegment.createFullSurah(112, quranInfo, "Al-Ikhlas"),
      PlaylistSegment.createFullSurah(109, quranInfo, "Al-Kafirun"),
      PlaylistSegment.createFullSurah(113, quranInfo, "Al-Falaq"),
      PlaylistSegment.createFullSurah(114, quranInfo, "An-Nas"),
      PlaylistSegment(sura = 2, startAyah = 270, endAyah = 286, isFullSurah = false, customLabel = "Al-Baqarah 270-286")
    )

    val playlist = CustomPlaylist(
      id = "daily-adhkar",
      name = "Daily Recitation",
      segments = segments
    )

    // Expected total ayahs: 7 + 7 + 4 + 6 + 5 + 6 + 17 = 52
    assertThat(playlist.totalAyahsCount).isEqualTo(52)
    val flattened = playlist.toFlattenedAyahs()
    assertThat(flattened).hasSize(52)
    assertThat(flattened.first()).isEqualTo(SuraAyah(1, 1))
    assertThat(flattened[7]).isEqualTo(SuraAyah(2, 1)) // Transition from Hamd to Baqarah
    assertThat(flattened.last()).isEqualTo(SuraAyah(2, 286))

    val config = MemorizationConfig(
      windowSize = 4,
      singleRepeat = 2,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = true,
      segments = segments,
      playlistId = playlist.id,
      playlistName = playlist.name
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)
    assertThat(plan).isNotEmpty()

    // 52 ayahs with sliding window
    val soloSteps = plan.filter { it.stepType == StepType.SOLO }
    assertThat(soloSteps).hasSize(52)

    // First solo step is 1:1, 8th solo step is 2:1
    assertThat(soloSteps[0].ayahs.first()).isEqualTo(SuraAyah(1, 1))
    assertThat(soloSteps[7].ayahs.first()).isEqualTo(SuraAyah(2, 1))

    // 51 connect steps for 52 ayahs (starting from j=1)
    val connectSteps = plan.filter { it.stepType == StepType.CONNECT }
    assertThat(connectSteps).hasSize(51)

    // At j=7 (where 2:1 is introduced), windowStart = maxOf(0, 7 - 4 + 1) = 4
    // Ayahs at 4, 5, 6, 7 are 1:5, 1:6, 1:7, 2:1!
    // This connects across the surah boundary between 1:7 and 2:1!
    val crossSurahConnect = plan.first { it.windowIndex == 7 && it.stepType == StepType.CONNECT }
    assertThat(crossSurahConnect.ayahs).containsExactly(
      SuraAyah(1, 5),
      SuraAyah(1, 6),
      SuraAyah(1, 7),
      SuraAyah(2, 1)
    ).inOrder()
  }
}
