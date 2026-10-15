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
   * If [config.segments] is populated, flattens all segments across Surahs.
   */
  fun buildPlan(config: MemorizationConfig, quranInfo: QuranInfo): List<MemorizationStep> {
    val allAyahs = if (config.segments.isNotEmpty()) {
      config.segments.flatMap { it.toAyahs() }
    } else {
      getAyahsInRange(config.startAyah, config.endAyah, quranInfo)
    }
    if (allAyahs.isEmpty()) return emptyList()

    val windows = allAyahs.chunked(config.windowSize)
    val totalWindows = windows.size
    val steps = mutableListOf<MemorizationStep>()
    var stepIndex = 0

    for (wIdx in windows.indices) {
      val windowAyahs = windows[wIdx]

      for (j in windowAyahs.indices) {
        val currentAyah = windowAyahs[j]
        val ayahLabel = "Surah ${currentAyah.sura}:${currentAyah.ayah}"

        // 1. Solo Step for current Ayah
        steps.add(
          MemorizationStep(
            stepIndex = stepIndex++,
            windowIndex = wIdx,
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
              windowIndex = wIdx,
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

        // 2. Connect Step if 2 or more Ayahs accumulated in current window
        if (j > 0 && config.connectRepeat > 0) {
          val connectedAyahs = windowAyahs.subList(0, j + 1)
          val first = connectedAyahs.first()
          val last = connectedAyahs.last()
          val connectLabel = if (first.sura == last.sura) {
            "Connecting Ayah ${first.ayah}–${last.ayah} (Surah ${first.sura})"
          } else {
            "Connecting ${first.sura}:${first.ayah} ➔ ${last.sura}:${last.ayah}"
          }

          steps.add(
            MemorizationStep(
              stepIndex = stepIndex++,
              windowIndex = wIdx,
              totalWindows = totalWindows,
              stepType = StepType.CONNECT,
              ayahs = connectedAyahs,
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
                windowIndex = wIdx,
                totalWindows = totalWindows,
                stepType = StepType.PAUSE_FOR_RECITATION,
                ayahs = connectedAyahs,
                targetRepeatCount = 1,
                delayMode = config.delayMode,
                isMaskedByDefault = config.memoryTestMode,
                description = "Recitation Pause ($connectLabel)"
              )
            )
          }
        }
      }

      // 3. Sliding Window Bridge Step (between current window and next window)
      if (config.enableBridge && wIdx < totalWindows - 1) {
        val nextWindow = windows[wIdx + 1]
        val bridgeAyahs = listOf(windowAyahs.last(), nextWindow.first())
        val bFirst = bridgeAyahs[0]
        val bSecond = bridgeAyahs[1]
        val bridgeLabel = if (bFirst.sura == bSecond.sura) {
          "Bridge Ayah ${bFirst.ayah} ➔ ${bSecond.ayah}"
        } else {
          "Bridge ${bFirst.sura}:${bFirst.ayah} ➔ ${bSecond.sura}:${bSecond.ayah}"
        }

        steps.add(
          MemorizationStep(
            stepIndex = stepIndex++,
            windowIndex = wIdx,
            totalWindows = totalWindows,
            stepType = StepType.BRIDGE,
            ayahs = bridgeAyahs,
            targetRepeatCount = 1,
            delayMode = MemorizationDelayMode.None,
            isMaskedByDefault = config.memoryTestMode,
            description = bridgeLabel
          )
        )
      }
    }

    return steps
  }
}
