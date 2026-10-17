package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.mobile.feature.audiobar.state.AudioBarState
import com.quran.mobile.feature.audiobar.state.AudioBarUiEvent

@Composable
internal fun PlayingAudioBar(
  state: AudioBarState.Playing,
  eventSink: (AudioBarUiEvent.CommonPlaybackEvent) -> Unit,
  playbackEventSink: (AudioBarUiEvent.PlayingPlaybackEvent) -> Unit,
  isModernUi: Boolean = false,
  modifier: Modifier = Modifier
) {
  if (isModernUi) {
    ModernPlaybackAudioBar(
      state = state,
      isPlaying = true,
      sink = eventSink,
      onPlayPauseToggle = { playbackEventSink(AudioBarUiEvent.PlayingPlaybackEvent.Pause) },
      modifier = modifier
    )
  } else {
    AudioBar(state, eventSink, modifier) {
      IconButton(
        modifier = Modifier
          .width(40.dp)
          .weight(1f),
        onClick = { playbackEventSink(AudioBarUiEvent.PlayingPlaybackEvent.Pause) }) {
        Icon(QuranIcons.Pause, contentDescription = "")
      }
    }
  }
}

@Composable
internal fun PausedAudioBar(
  state: AudioBarState.Paused,
  eventSink: (AudioBarUiEvent.CommonPlaybackEvent) -> Unit,
  pausedEventSink: (AudioBarUiEvent.PausedPlaybackEvent) -> Unit,
  isModernUi: Boolean = false,
  modifier: Modifier = Modifier
) {
  if (isModernUi) {
    ModernPlaybackAudioBar(
      state = state,
      isPlaying = false,
      sink = eventSink,
      onPlayPauseToggle = { pausedEventSink(AudioBarUiEvent.PausedPlaybackEvent.Play) },
      modifier = modifier
    )
  } else {
    AudioBar(state = state, eventSink, modifier = modifier) {
      IconButton(
        modifier = Modifier
          .width(40.dp)
          .weight(1f),
        onClick = { pausedEventSink(AudioBarUiEvent.PausedPlaybackEvent.Play) }) {
        Icon(QuranIcons.PlayArrow, contentDescription = "")
      }
    }
  }
}

@Composable
private fun ModernPlaybackAudioBar(
  state: AudioBarState.ActivePlayback,
  isPlaying: Boolean,
  sink: (AudioBarUiEvent.CommonPlaybackEvent) -> Unit,
  onPlayPauseToggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
    ) {
      // Left: Stop button (in subtle red/surface pill)
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(
          0.5.dp,
          MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
        ),
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .clickable { sink(AudioBarUiEvent.CommonPlaybackEvent.Stop) }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = QuranIcons.Stop,
            contentDescription = "Stop",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Center playback cluster: Rewind, Big Play/Pause, FastForward
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Rewind (Previous Ayah)
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable { sink(AudioBarUiEvent.CommonPlaybackEvent.Rewind) }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = QuranIcons.FastRewind,
              contentDescription = "Previous Ayah",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // Play / Pause prominent circular accent button
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          shadowElevation = 6.dp,
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable { onPlayPauseToggle() }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isPlaying) QuranIcons.Pause else QuranIcons.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(26.dp)
            )
          }
        }

        // Fast Forward (Next Ayah)
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable { sink(AudioBarUiEvent.CommonPlaybackEvent.FastForward) }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = QuranIcons.FastForward,
              contentDescription = "Next Ayah",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // Right cluster: Repeat chip, Speed chip, Settings
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Repeat Pill Chip
        val infinity = stringResource(id = com.quran.mobile.common.ui.core.R.string.infinity)
        val repeatText = if (state.repeat > -1) {
          if (state.repeat == 0) "1x" else "${state.repeat}x"
        } else infinity

        val isRepeatActive = state.repeat != 0
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isRepeatActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isRepeatActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
          ),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              val currentIndex = REPEAT_VALUES.indexOf(state.repeat)
              val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % REPEAT_VALUES.size else 0
              sink(AudioBarUiEvent.CommonPlaybackEvent.SetRepeat(REPEAT_VALUES[nextIndex]))
            }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Icon(
              imageVector = QuranIcons.Repeat,
              contentDescription = "Repeat",
              tint = if (isRepeatActive) MaterialTheme.colorScheme.primary
                     else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = repeatText,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (isRepeatActive) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Speed Pill Chip
        val isSpeedNonDefault = state.speed != 1.0f
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSpeedNonDefault) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isSpeedNonDefault) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
          ),
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              val currentIndex = SPEED_VALUES.indexOf(state.speed)
              val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % SPEED_VALUES.size else 0
              sink(AudioBarUiEvent.CommonPlaybackEvent.SetSpeed(SPEED_VALUES[nextIndex]))
            }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
          ) {
            Text(
              text = "${state.speed}x",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (isSpeedNonDefault) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Audio Settings Button
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { sink(AudioBarUiEvent.CommonPlaybackEvent.ShowSettings) }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = QuranIcons.Settings,
              contentDescription = "Settings",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
internal fun AudioBar(
  state: AudioBarState.ActivePlayback,
  sink: (AudioBarUiEvent.CommonPlaybackEvent) -> Unit,
  modifier: Modifier = Modifier,
  actionButton: @Composable RowScope.() -> Unit
) {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier
    ) {
      IconButton(
        modifier = Modifier
          .width(40.dp)
          .weight(1f),
        onClick = { sink(AudioBarUiEvent.CommonPlaybackEvent.Stop) }) {
        Icon(QuranIcons.Stop, contentDescription = "", modifier = Modifier.width(40.dp))
      }

      IconButton(
        modifier = Modifier
          .width(40.dp)
          .weight(1f),
        onClick = { sink(AudioBarUiEvent.CommonPlaybackEvent.Rewind) }) {
        Icon(QuranIcons.FastRewind, contentDescription = "", modifier = Modifier.width(40.dp))
      }

      actionButton()

      IconButton(
        modifier = Modifier
          .width(40.dp)
          .weight(1f),
        onClick = { sink(AudioBarUiEvent.CommonPlaybackEvent.FastForward) }) {
        Icon(QuranIcons.FastForward, contentDescription = "", modifier = Modifier.width(40.dp))
      }

      val infinity = stringResource(id = com.quran.mobile.common.ui.core.R.string.infinity)
      RepeatableButton(
        icon = QuranIcons.Repeat,
        contentDescription = "",
        values = REPEAT_VALUES,
        value = state.repeat,
        defaultValue = 0,
        modifier = Modifier
          .width(40.dp)
          .weight(1f),
        format = {
          if (it > -1) {
            it.toString()
          } else {
            infinity
          }
        }
      ) {
        sink(AudioBarUiEvent.CommonPlaybackEvent.SetRepeat(it))
      }

      RepeatableButton(
        icon = QuranIcons.Speed,
        contentDescription = "",
        values = SPEED_VALUES,
        value = state.speed,
        defaultValue = 1.0f,
        modifier = Modifier
          .width(40.dp)
          .weight(1f),
        format = { it.toString() }
      ) {
        sink(AudioBarUiEvent.CommonPlaybackEvent.SetSpeed(it))
      }

      IconButton(
        modifier = Modifier
          .width(40.dp)
          .weight(1f),
        onClick = { sink(AudioBarUiEvent.CommonPlaybackEvent.ShowSettings) }) {
        Icon(QuranIcons.Settings, contentDescription = "")
      }
    }
  }
}

private val REPEAT_VALUES = listOf(0, 1, 2, 3, -1)
private val SPEED_VALUES = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

@Preview
@Composable
private fun PlayingAudioBarPreview() {
  QuranTheme {
    PlayingAudioBar(
      state = AudioBarState.Playing(
        repeat = 0,
        speed = 1.0f
      ),
      eventSink = {},
      playbackEventSink = {}
    )
  }
}

@Preview
@Composable
private fun PausedAudioBarPreview() {
  QuranTheme {
    PausedAudioBar(
      state = AudioBarState.Paused(
        repeat = 1,
        speed = 0.5f
      ),
      eventSink = {},
      pausedEventSink = {}
    )
  }
}
