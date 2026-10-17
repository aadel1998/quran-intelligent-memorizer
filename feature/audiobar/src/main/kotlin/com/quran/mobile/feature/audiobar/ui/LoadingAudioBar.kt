package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.mobile.feature.audiobar.state.AudioBarState
import com.quran.mobile.feature.audiobar.state.AudioBarUiEvent

@Composable
internal fun LoadingAudioBar(
  state: AudioBarState.Loading,
  eventSink: (AudioBarUiEvent.CancelablePlaybackEvent) -> Unit,
  isModernUi: Boolean = false,
  modifier: Modifier = Modifier
) {
  ProgressAudioBar(
    progress = state.progress,
    messageResource = state.messageResource,
    onClick = { eventSink(AudioBarUiEvent.CancelablePlaybackEvent.Cancel) },
    isModernUi = isModernUi,
    modifier = modifier
  )
}

@Composable
internal fun DownloadingAudioBar(
  state: AudioBarState.Downloading,
  eventSink: (AudioBarUiEvent.DownloadingPlaybackEvent) -> Unit,
  isModernUi: Boolean = false,
  modifier: Modifier = Modifier
) {
  ProgressAudioBar(
    progress = state.progress,
    messageResource = state.messageResource,
    onClick = { eventSink(AudioBarUiEvent.DownloadingPlaybackEvent.Cancel) },
    isModernUi = isModernUi,
    modifier = modifier
  )
}

@Composable
internal fun ProgressAudioBar(
  progress: Int,
  messageResource: Int,
  onClick: () -> Unit,
  isModernUi: Boolean = false,
  modifier: Modifier = Modifier
) {
  if (isModernUi) {
    ModernProgressAudioBar(
      progress = progress,
      messageResource = messageResource,
      onClick = onClick,
      modifier = modifier
    )
  } else {
    ClassicProgressAudioBar(
      progress = progress,
      messageResource = messageResource,
      onClick = onClick,
      modifier = modifier
    )
  }
}

@Composable
private fun ModernProgressAudioBar(
  progress: Int,
  messageResource: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .clickable { onClick() }
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = QuranIcons.Close,
          contentDescription = stringResource(id = android.R.string.cancel),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    Column(modifier = Modifier.weight(1f)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = stringResource(id = messageResource),
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface
        )
        if (progress != -1) {
          Text(
            text = "$progress%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      if (progress == -1) {
        LinearProgressIndicator(
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .height(6.dp)
        )
      } else {
        LinearProgressIndicator(
          progress = { progress.toFloat() / 100f },
          gapSize = 0.dp,
          drawStopIndicator = {},
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .height(6.dp)
        )
      }
    }
  }
}

@Composable
private fun ClassicProgressAudioBar(
  progress: Int,
  messageResource: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.height(IntrinsicSize.Min)
  ) {
    IconButton(onClick = { onClick() }) {
      Icon(QuranIcons.Close, contentDescription = stringResource(id = android.R.string.cancel))
    }

    Column(modifier = Modifier.weight(1f)) {
      if (progress == -1) {
        LinearProgressIndicator(
          modifier = Modifier.fillMaxWidth()
            .padding(end = 8.dp)
        )
      } else {
        LinearProgressIndicator(
          progress = { progress.toFloat() / 100f },
          gapSize = 0.dp,
          drawStopIndicator = {},
          modifier = Modifier.fillMaxWidth()
            .padding(end = 8.dp)
        )
      }

      Text(text = stringResource(id = messageResource))
    }
  }
}

@Preview
@Composable
private fun LoadingAudioBarPreview() {
  QuranTheme {
    LoadingAudioBar(
      state = AudioBarState.Loading(
        progress = 50,
        messageResource = com.quran.mobile.common.download.R.string.downloading
      ),
      eventSink = {}
    )
  }
}

@Preview
@Composable
private fun LoadingAudioBarIndeterminatePreview() {
  QuranTheme {
    LoadingAudioBar(
      state = AudioBarState.Loading(
        progress = -1,
        messageResource = com.quran.mobile.common.ui.core.R.string.loading
      ),
      eventSink = {}
    )
  }
}
