package com.quran.mobile.feature.audiobar

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.mobile.feature.audiobar.presenter.AudioBarPresenter
import com.quran.mobile.feature.audiobar.ui.AudioBar
import dev.zacsweers.metro.Inject

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quran.labs.androidquran.common.audio.memorization.MemorizationEngine
import com.quran.mobile.feature.audiobar.state.AudioBarState
import com.quran.mobile.feature.audiobar.state.AudioBarUiEvent
import com.quran.mobile.feature.audiobar.ui.MemorizationHud
import com.quran.mobile.feature.audiobar.ui.MemorizationLiveSettingsSheet

class AudioBarWrapper @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

  @Inject
  lateinit var audioBarPresenter: AudioBarPresenter

  @Inject
  lateinit var memorizationEngine: MemorizationEngine

  init {
    (context as? AudioBarInjector)?.inject(this)
  }

  @Composable
  override fun Content() {
    QuranTheme {
      val scope = rememberCoroutineScope()
      val flow = remember {
        scope.launchMolecule(mode = RecompositionMode.ContextClock) {
          audioBarPresenter.audioBarPresenter()
        }
      }
      val eventListeners = remember(audioBarPresenter) { audioBarPresenter.eventListeners() }
      val memorizationState by memorizationEngine.state.collectAsState()
      val audioBarState by flow.collectAsState()
      val isDownloading = audioBarState is AudioBarState.Downloading ||
          audioBarState is AudioBarState.Prompt ||
          audioBarState is AudioBarState.Loading
      var showLiveSettingsSheet by remember { mutableStateOf(false) }

      Card(
        shape = CardDefaults.shape.topOnly(),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (memorizationState.isActive && !isDownloading) {
          MemorizationHud(
            state = memorizationState,
            onPreviousStep = { memorizationEngine.previousStep() },
            onPlayPause = {
              if (memorizationState.isPaused) {
                memorizationEngine.resume()
                eventListeners.pausedPlaybackEventSink(AudioBarUiEvent.PausedPlaybackEvent.Play)
              } else {
                memorizationEngine.pause()
                eventListeners.playingPlaybackEventSink(AudioBarUiEvent.PlayingPlaybackEvent.Pause)
              }
            },
            onSkipStep = { memorizationEngine.skipCurrentStep() },
            onStop = {
              memorizationEngine.stopMemorization()
              eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.Stop)
            },
            onToggleMask = { memorizationEngine.toggleTextMask() },
            onOpenSettings = { showLiveSettingsSheet = true },
            onReplayRecitation = {
              val path = memorizationState.lastRecitationAudioPath
              if (path != null) {
                try {
                  android.media.MediaPlayer().apply {
                    setDataSource(path)
                    setOnCompletionListener { it.release() }
                    prepare()
                    start()
                  }
                } catch (e: Exception) {
                  android.util.Log.e("AudioBarWrapper", "Error playing recorded recitation", e)
                }
              }
            },
            modifier = Modifier
              .padding(
                WindowInsets.navigationBars.add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal.plus(WindowInsetsSides.Bottom))
                  .asPaddingValues()
              )
              .padding(top = 4.dp)
              .padding(horizontal = 8.dp)
          )
        } else {
          AudioBar(
            flow,
            eventListeners,
            modifier = Modifier
              .padding(
                WindowInsets.navigationBars.add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal.plus(WindowInsetsSides.Bottom))
                  .asPaddingValues()
              )
              .padding(top = 8.dp)
              .padding(horizontal = 16.dp)
              .heightIn(min = dimensionResource(id = R.dimen.audiobar_height))
          )
        }
      }

      val currentConfig = memorizationState.config
      if (showLiveSettingsSheet && currentConfig != null) {
        MemorizationLiveSettingsSheet(
          currentConfig = currentConfig,
          onApplySettings = { updated ->
            memorizationEngine.updateConfig(updated)
            showLiveSettingsSheet = false
          },
          onDismissRequest = { showLiveSettingsSheet = false }
        )
      }
    }
  }

  private fun Shape.topOnly(): Shape {
    return if (this is RoundedCornerShape) {
      this.copy(
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp)
      )
    } else {
      this
    }
  }
}
