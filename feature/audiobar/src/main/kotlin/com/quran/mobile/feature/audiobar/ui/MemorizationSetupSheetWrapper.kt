package com.quran.mobile.feature.audiobar.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.MaskingLevel
import com.quran.labs.androidquran.common.audio.memorization.MasteredRange
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDefaultSettings
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDelayMode
import com.quran.labs.androidquran.common.audio.memorization.SavedSuraSession
import com.quran.labs.androidquran.common.ui.core.QuranTheme

/**
 * FrameLayout wrapper hosting the Compose [MemorizationSetupSheet]
 * for display inside PagerActivity's overlay container.
 */
class MemorizationSetupSheetWrapper(
  context: Context,
  private val initialStartAyah: SuraAyah,
  private val initialEndAyah: SuraAyah,
  private val onStart: (MemorizationConfig) -> Unit,
  private val onDismissed: () -> Unit,
  private val savedSessionConfig: MemorizationConfig? = null,
  savedSessionStep: Int = 0,
  private val onResume: (() -> Unit)? = null,
  private val onStartFromBeginning: (() -> Unit)? = null,
  private val onResetSurah: (() -> Unit)? = null,
  private val defaultSettings: MemorizationDefaultSettings = MemorizationDefaultSettings(),
  private val allSavedSurahs: List<SavedSuraSession> = emptyList(),
  private val onResumeOtherSura: ((SavedSuraSession) -> Unit)? = null,
  private val onStartOtherSura: ((SavedSuraSession) -> Unit)? = null,
  private val onClearOtherSura: ((Int) -> Unit)? = null,
  private val masteredRanges: List<MasteredRange> = emptyList(),
  private val onReview: ((MasteredRange) -> Unit)? = null,
  private val savedPlaylists: List<CustomPlaylist> = emptyList(),
  private val initialPlaylist: CustomPlaylist? = null,
  private val maxAyahsForSura: Int = 286
) : FrameLayout(context) {

  init {
    val composeView = ComposeView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setContent {
        QuranTheme {
          MemorizationSetupSheet(
            initialStartAyah = initialStartAyah,
            initialEndAyah = initialEndAyah,
            savedSessionConfig = savedSessionConfig,
            savedSessionStep = savedSessionStep,
            onResumeSession = {
              onResume?.invoke()
              onDismissed()
            },
            onStartFromBeginning = {
              onStartFromBeginning?.invoke()
              onDismissed()
            },
            onResetSurah = onResetSurah,
            defaultSettings = defaultSettings,
            allSavedSurahs = allSavedSurahs,
            onResumeOtherSura = { s ->
              onResumeOtherSura?.invoke(s)
              onDismissed()
            },
            onStartOtherSura = { s ->
              onStartOtherSura?.invoke(s)
              onDismissed()
            },
            onClearOtherSura = onClearOtherSura,
            masteredRanges = masteredRanges,
            savedPlaylists = savedPlaylists,
            initialPlaylist = initialPlaylist,
            maxAyahsForSura = maxAyahsForSura,
            onReviewRange = { range ->
              if (onReview != null) {
                onReview.invoke(range)
              } else {
                val reviewConfig = MemorizationConfig(
                  startAyah = range.startAyah,
                  endAyah = range.endAyah,
                  windowSize = 4,
                  singleRepeat = 1,
                  connectRepeat = 2,
                  delayMode = MemorizationDelayMode.None,
                  memoryTestMode = true,
                  maskingLevel = MaskingLevel.MASK_ON_CONNECT,
                  recordRecitation = false,
                  enableBridge = false
                )
                onStart(reviewConfig)
              }
              onDismissed()
            },
            onStartMemorization = { config ->
              onStart(config)
              onDismissed()
            },
            onDismissRequest = onDismissed
          )
        }
      }
    }
    addView(composeView)
  }
}
