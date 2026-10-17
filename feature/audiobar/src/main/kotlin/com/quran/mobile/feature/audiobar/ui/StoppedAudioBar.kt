package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.LocalQuranColors
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.mobile.feature.audiobar.state.AudioBarState
import com.quran.mobile.feature.audiobar.state.AudioBarUiEvent

@Composable
internal fun StoppedAudioBar(
  state: AudioBarState.Stopped,
  eventSink: (AudioBarUiEvent.StoppedPlaybackEvent) -> Unit,
  isModernUi: Boolean = false,
  modifier: Modifier = Modifier
) {
  if (isModernUi) {
    ModernStoppedAudioBar(state = state, eventSink = eventSink, modifier = modifier)
  } else {
    ClassicStoppedAudioBar(state = state, eventSink = eventSink, modifier = modifier)
  }
}

@Composable
private fun ModernStoppedAudioBar(
  state: AudioBarState.Stopped,
  eventSink: (AudioBarUiEvent.StoppedPlaybackEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    // Play Action Capsule (Circular Accent Button)
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primary,
      shadowElevation = 4.dp,
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .clickable { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Play) }
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = QuranIcons.PlayArrow,
          contentDescription = "Play",
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(24.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Reciter Selector Pill
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
      ),
      modifier = Modifier
        .weight(1f)
        .height(40.dp)
        .clip(RoundedCornerShape(20.dp))
        .clickable { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.ChangeQari) }
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(horizontal = 12.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(
            imageVector = QuranIcons.Mic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
          )
          Text(
            text = stringResource(state.qariNameResource),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          imageVector = QuranIcons.ExpandMore,
          contentDescription = "Change Reciter",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Secondary Actions: Memorize, Playlists, Record
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Memorize Mode Button
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(
          0.5.dp,
          MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .clickable { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Memorize) }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = QuranIcons.MenuBook,
            contentDescription = "Memorization Mode",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp)
          )
        }
      }

      // Playlists Button
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .clickable { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Playlists) }
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = QuranIcons.List,
            contentDescription = "Playlists",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
          )
        }
      }

      if (state.enableRecording) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Record) }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = QuranIcons.Mic,
              contentDescription = "Record Recitation",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(19.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ClassicStoppedAudioBar(
  state: AudioBarState.Stopped,
  eventSink: (AudioBarUiEvent.StoppedPlaybackEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.height(IntrinsicSize.Min)
  ) {
    IconButton(onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Play) }) {
      Icon(QuranIcons.PlayArrow, contentDescription = "")
    }

    IconButton(onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Memorize) }) {
      Icon(QuranIcons.MenuBook, contentDescription = "Memorization Mode")
    }

    IconButton(onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Playlists) }) {
      Icon(QuranIcons.List, contentDescription = "Playlists")
    }

    TextButton(
      modifier = Modifier.weight(1f),
      onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.ChangeQari) }
    ) {
      Text(
        text = stringResource(state.qariNameResource),
        color = LocalQuranColors.current.defaultTextColor
      )
      Spacer(modifier = Modifier.weight(1f))
      Icon(QuranIcons.ExpandMore, contentDescription = "", tint =
        LocalQuranColors.current.defaultTextColor)
    }

    if (state.enableRecording) {
      IconButton(onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Record) }) {
        Icon(QuranIcons.Mic, contentDescription = "")
      }
    }
  }
}

@Preview
@Composable
private fun StoppedAudioBarPreview() {
  QuranTheme {
    StoppedAudioBar(
      state = AudioBarState.Stopped(
        qariNameResource = com.quran.labs.androidquran.common.audio.R.string.qari_dussary,
        enableRecording = false
      ),
      eventSink = {}
    )
  }
}

@Preview
@Composable
private fun StoppedAudioBarWithRecordingPreview() {
  QuranTheme {
    StoppedAudioBar(
      state = AudioBarState.Stopped(
        qariNameResource = com.quran.labs.androidquran.common.audio.R.string.qari_dussary,
        enableRecording = true
      ),
      eventSink = {}
    )
  }
}
