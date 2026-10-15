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
  private val onDismissed: () -> Unit
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
