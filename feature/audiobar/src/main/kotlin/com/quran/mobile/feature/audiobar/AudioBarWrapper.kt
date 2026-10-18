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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationEngine
import com.quran.labs.androidquran.common.audio.memorization.MemorizationPreferences
import com.quran.mobile.feature.audiobar.state.AudioBarState
import com.quran.mobile.feature.audiobar.state.AudioBarUiEvent
import com.quran.mobile.feature.audiobar.ui.MemorizationHud
import com.quran.mobile.feature.audiobar.ui.MemorizationLiveSettingsSheet
import com.quran.mobile.feature.audiobar.ui.MemorizationTrackerSheet

class AudioBarWrapper @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

  @Inject
  lateinit var audioBarPresenter: AudioBarPresenter

  @Inject
  lateinit var memorizationEngine: MemorizationEngine

  @Inject
  lateinit var memorizationPreferences: MemorizationPreferences

  var onEnterPip: (() -> Unit)? = null
  var onResumeSuraSession: ((MemorizationConfig) -> Unit)? = null
  var onStartSuraSession: ((MemorizationConfig) -> Unit)? = null
  var onRestartCurrentSession: (() -> Unit)? = null

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
      var showTrackerSheet by remember { mutableStateOf(false) }
      var flaggedAyahsVersion by remember { mutableIntStateOf(0) }

      val currentAyah = memorizationState.currentPlayingAyah
      val isCurrentAyahFlagged = remember(currentAyah, flaggedAyahsVersion) {
        if (currentAyah != null) memorizationPreferences.isAyahFlagged(currentAyah.sura, currentAyah.ayah) else false
      }
      val onToggleFlagAyah: (() -> Unit)? = remember(currentAyah) {
        if (currentAyah != null) {
          {
            memorizationPreferences.toggleAyahFlagged(currentAyah.sura, currentAyah.ayah)
            flaggedAyahsVersion++
          }
        } else null
      }

      val currentSpeed = when (val s = audioBarState) {
        is AudioBarState.ActivePlayback -> s.speed
        else -> memorizationState.config?.playbackSpeed ?: 1.0f
      }
      val onCycleSpeed: () -> Unit = {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 0.75f)
        val currentIdx = speeds.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.05f }
        val nextIdx = if (currentIdx in speeds.indices) (currentIdx + 1) % speeds.size else 0
        val nextSpeed = speeds[nextIdx]
        eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.SetSpeed(nextSpeed))
        memorizationState.config?.let { cfg ->
          memorizationEngine.updateConfig(cfg.copy(playbackSpeed = nextSpeed))
        }
      }

      val isModernUi = remember {
        try {
          val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
          prefs.getBoolean("pref_use_modern_ui", true)
        } catch (_: Exception) {
          true
        }
      }

      if (isModernUi) {
        Surface(
          shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
          ),
          shadowElevation = 14.dp,
          modifier = Modifier.fillMaxWidth()
        ) {
          if (memorizationState.isActive && !isDownloading) {
            MemorizationHud(
              state = memorizationState,
              onPreviousStep = {
                eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.Rewind)
              },
              onPlayPause = {
                if (memorizationState.isPaused) {
                  memorizationEngine.resume()
                  eventListeners.pausedPlaybackEventSink(AudioBarUiEvent.PausedPlaybackEvent.Play)
                } else {
                  memorizationEngine.pause()
                  eventListeners.playingPlaybackEventSink(AudioBarUiEvent.PlayingPlaybackEvent.Pause)
                }
              },
              onSkipStep = {
                eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.FastForward)
              },
              onStop = {
                memorizationEngine.stopMemorization()
                eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.Stop)
              },
              onToggleMask = { memorizationEngine.toggleTextMask() },
              playbackSpeed = currentSpeed,
              onCycleSpeed = onCycleSpeed,
              isCurrentAyahFlagged = isCurrentAyahFlagged,
              onToggleFlagAyah = onToggleFlagAyah,
              onOpenSettings = { showLiveSettingsSheet = true },
              onOpenTracker = { showTrackerSheet = true },
              onEnterPip = onEnterPip,
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
              flow = flow,
              eventListeners = eventListeners,
              isModernUi = true,
              modifier = Modifier
                .padding(
                  WindowInsets.navigationBars.add(WindowInsets.displayCutout)
                      .only(WindowInsetsSides.Horizontal.plus(WindowInsetsSides.Bottom))
                    .asPaddingValues()
                )
                .padding(top = 8.dp, bottom = 4.dp)
                .padding(horizontal = 14.dp)
                .heightIn(min = dimensionResource(id = R.dimen.audiobar_height))
            )
          }
        }
      } else {
        Card(
          shape = CardDefaults.shape.topOnly(),
          modifier = Modifier.fillMaxWidth()
        ) {
          if (memorizationState.isActive && !isDownloading) {
            MemorizationHud(
              state = memorizationState,
              onPreviousStep = {
                eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.Rewind)
              },
              onPlayPause = {
                if (memorizationState.isPaused) {
                  memorizationEngine.resume()
                  eventListeners.pausedPlaybackEventSink(AudioBarUiEvent.PausedPlaybackEvent.Play)
                } else {
                  memorizationEngine.pause()
                  eventListeners.playingPlaybackEventSink(AudioBarUiEvent.PlayingPlaybackEvent.Pause)
                }
              },
              onSkipStep = {
                eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.FastForward)
              },
              onStop = {
                memorizationEngine.stopMemorization()
                eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.Stop)
              },
              onToggleMask = { memorizationEngine.toggleTextMask() },
              playbackSpeed = currentSpeed,
              onCycleSpeed = onCycleSpeed,
              isCurrentAyahFlagged = isCurrentAyahFlagged,
              onToggleFlagAyah = onToggleFlagAyah,
              onOpenSettings = { showLiveSettingsSheet = true },
              onOpenTracker = { showTrackerSheet = true },
              onEnterPip = onEnterPip,
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
              flow = flow,
              eventListeners = eventListeners,
              isModernUi = false,
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
      }

      val currentConfig = memorizationState.config
      if (showLiveSettingsSheet && currentConfig != null) {
        MemorizationLiveSettingsSheet(
          currentConfig = currentConfig,
          onApplySettings = { updated ->
            memorizationEngine.updateConfig(updated)
            if (kotlin.math.abs(updated.playbackSpeed - currentSpeed) > 0.01f) {
              eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.SetSpeed(updated.playbackSpeed))
            }
            showLiveSettingsSheet = false
          },
          onDismissRequest = { showLiveSettingsSheet = false },
          isCurrentAyahFlagged = isCurrentAyahFlagged,
          onToggleFlagAyah = onToggleFlagAyah,
          currentAyahNumber = currentAyah?.ayah
        )
      }

      if (showTrackerSheet) {
        val savedSurahs = remember(showTrackerSheet) {
          memorizationPreferences.getAllSavedSurahSessions()
        }
        MemorizationTrackerSheet(
          savedSurahSessions = savedSurahs,
          currentActiveState = memorizationState,
          onResumeSession = { config ->
            showTrackerSheet = false
            onResumeSuraSession?.invoke(config)
          },
          onStartFromBeginning = { config ->
            showTrackerSheet = false
            onStartSuraSession?.invoke(config)
          },
          onClearSession = { sura ->
            memorizationPreferences.clearSessionForSura(sura)
          },
          onRestartCurrentSession = {
            showTrackerSheet = false
            onRestartCurrentSession?.invoke()
          },
          onDismissRequest = { showTrackerSheet = false }
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
