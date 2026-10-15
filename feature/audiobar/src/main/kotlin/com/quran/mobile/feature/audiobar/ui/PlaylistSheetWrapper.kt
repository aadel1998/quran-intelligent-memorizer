package com.quran.mobile.feature.audiobar.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.PlaylistRepository
import com.quran.labs.androidquran.common.ui.core.QuranTheme

/**
 * FrameLayout wrapper hosting the Compose [PlaylistListSheet] for display inside PagerActivity's overlay container.
 */
class PlaylistSheetWrapper(
  context: Context,
  private val playlistRepository: PlaylistRepository,
  private val quranInfo: QuranInfo,
  private val onPlayPlaylist: (CustomPlaylist) -> Unit,
  private val onMemorizePlaylist: (CustomPlaylist) -> Unit,
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
          PlaylistListSheet(
            playlistRepository = playlistRepository,
            quranInfo = quranInfo,
            onPlayPlaylist = { playlist ->
              onPlayPlaylist(playlist)
              onDismissed()
            },
            onMemorizePlaylist = { playlist ->
              onMemorizePlaylist(playlist)
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
