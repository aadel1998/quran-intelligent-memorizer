package com.quran.mobile.feature.audiobar.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
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
  private val savedSessionStep: Int = 0,
  private val onResume: (() -> Unit)? = null
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
