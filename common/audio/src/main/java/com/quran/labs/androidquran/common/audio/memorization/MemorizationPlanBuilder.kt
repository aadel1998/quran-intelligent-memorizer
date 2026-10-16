package com.quran.labs.androidquran.common.audio.memorization

import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah

/**
 * Builds a deterministic, progressive sliding-window plan of steps
 * for Quran memorization according to the algorithm specification,
 * supporting both single contiguous ranges and custom cross-Surah playlists.
 */
object MemorizationPlanBuilder {

  /**
   * Generates the ordered list of Ayahs within the specified closed range.
   */
  fun getAyahsInRange(start: SuraAyah, end: SuraAyah, quranInfo: QuranInfo): List<SuraAyah> {
    require(start <= end) { "startAyah ($start) must be <= endAyah ($end)" }
    val ayahs = mutableListOf<SuraAyah>()
    var current: SuraAyah? = start
    while (current != null && current <= end) {
      ayahs.add(current)
      if (current == end) break
      current = current.next(quranInfo)
    }
    return ayahs
  }

  /**
   * Constructs the full sequence of [MemorizationStep]s for the provided [config].
   *
   * ### Algorithm (True Sliding-Window)
   *
   * For N total ayahs and window size W the plan is built one ayah at a time:
   *
   *   for j in 0..<N:
   *     windowStart = max(0, j - W + 1)
   *     window      = allAyahs[windowStart..j]   // grows then slides
   *
   *     emit SOLO(allAyahs[j], Rs)               // new ayah solo
   *     emit PAUSE_FOR_RECITATION?               // if delayMode != None
   *     if window.size > 1 && Rc > 0:
   *       emit CONNECT(window, Rc)               // sliding group
   *       emit PAUSE_FOR_RECITATION?
   *
   * The windowIndex reported to the HUD is j (0-based) so the user sees
   * smooth linear progress ("Window 5/24") with no sudden jumps.
   *
   * This replaces the previous chunked() approach which created
   * non-overlapping fixed blocks and never properly practised cross-chunk
   * transitions (the root cause of "ayahs missed at the beginning").
   *
   * If config.segments is populated, all segments are flattened first.
   */
  fun buildPlan(config: MemorizationConfig, quranInfo: QuranInfo): List<MemorizationStep> {
    val allAyahs: List<SuraAyah> = if (config.segments.isNotEmpty()) {
      config.segments.flatMap { it.toAyahs() }
    } else {
      getAyahsInRange(config.startAyah, config.endAyah, quranInfo)
    }
    if (allAyahs.isEmpty()) return emptyList()

    val n = allAyahs.size
    val W = config.windowSize
    // totalWindows == n so HUD shows "Window j+1 / n" -- smooth linear progress
    val totalWindows = n
    val steps = mutableListOf<MemorizationStep>()
    var stepIndex = 0

    for (j in 0 until n) {
      val currentAyah = allAyahs[j]
      // Sliding window: grows from 1 up to W, then oldest drops off each step
      val windowStart = maxOf(0, j - W + 1)
      // .toList() snapshots the subList so MemorizationStep holds stable data
      val windowAyahs: List<SuraAyah> = allAyahs.subList(windowStart, j + 1).toList()

      val ayahLabel = "Surah ${currentAyah.sura}:${currentAyah.ayah}"

      // 1. SOLO step for the newly introduced Ayah
      steps.add(
        MemorizationStep(
          stepIndex = stepIndex++,
          windowIndex = j,
          totalWindows = totalWindows,
          stepType = StepType.SOLO,
          ayahs = listOf(currentAyah),
          targetRepeatCount = config.singleRepeat,
          delayMode = config.delayMode,
          isMaskedByDefault = false,
          description = "$ayahLabel Solo"
        )
      )

      // 1b. Optional Recitation Pause after Solo
      if (config.delayMode != MemorizationDelayMode.None) {
        steps.add(
          MemorizationStep(
            stepIndex = stepIndex++,
            windowIndex = j,
            totalWindows = totalWindows,
            stepType = StepType.PAUSE_FOR_RECITATION,
            ayahs = listOf(currentAyah),
            targetRepeatCount = 1,
            delayMode = config.delayMode,
            isMaskedByDefault = config.memoryTestMode,
            description = "Recitation Pause ($ayahLabel)"
          )
        )
      }

      // 2. CONNECT step -- the live sliding window of accumulated Ayahs.
      // Activated once 2+ ayahs are in the window and connectRepeat > 0.
      if (windowAyahs.size > 1 && config.connectRepeat > 0) {
        val first = windowAyahs.first()
        val last = windowAyahs.last()
        val connectLabel = if (first.sura == last.sura) {
          "Connecting Ayah ${first.ayah}-${last.ayah} (Surah ${first.sura})"
        } else {
          "Connecting ${first.sura}:${first.ayah} -> ${last.sura}:${last.ayah}"
        }

        steps.add(
          MemorizationStep(
            stepIndex = stepIndex++,
            windowIndex = j,
            totalWindows = totalWindows,
            stepType = StepType.CONNECT,
            ayahs = windowAyahs,
            targetRepeatCount = config.connectRepeat,
            delayMode = config.delayMode,
            isMaskedByDefault = config.memoryTestMode,
            description = connectLabel
          )
        )

        // 2b. Optional Recitation Pause after Connect
        if (config.delayMode != MemorizationDelayMode.None) {
          steps.add(
            MemorizationStep(
              stepIndex = stepIndex++,
              windowIndex = j,
              totalWindows = totalWindows,
              stepType = StepType.PAUSE_FOR_RECITATION,
              ayahs = windowAyahs,
              targetRepeatCount = 1,
              delayMode = config.delayMode,
              isMaskedByDefault = config.memoryTestMode,
              description = "Recitation Pause ($connectLabel)"
            )
          )
        }
      }
    }

    return steps
  }
}
