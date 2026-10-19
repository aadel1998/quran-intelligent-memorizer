package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.audio.memorization.MemorizationState
import com.quran.labs.androidquran.common.audio.memorization.StepType
import com.quran.labs.androidquran.common.ui.core.LocalQuranColors
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.mobile.common.ui.core.R as CoreR

/**
 * Active HUD / Player Bar for the Intelligent Quran Memorization Mode.
 *
 * Displays:
 * - Active window index and total windows (e.g. "Window 1/3")
 * - Active step description & repeat counter (e.g. "Connecting Ayah 1–3 (Repeat 2/2)")
 * - Play/Pause, Previous Step, Skip Step, Speed, Bookmark, and Exit controls
 * - Peek/Hide text toggle for blind self-recitation test
 */
@Composable
fun MemorizationHud(
  state: MemorizationState,
  onPreviousStep: () -> Unit,
  onPlayPause: () -> Unit,
  onSkipStep: () -> Unit,
  onStop: () -> Unit,
  onToggleMask: () -> Unit,
  playbackSpeed: Float = 1.0f,
  onCycleSpeed: (() -> Unit)? = null,
  isCurrentAyahFlagged: Boolean = false,
  onToggleFlagAyah: (() -> Unit)? = null,
  onReplayRecitation: (() -> Unit)? = null,
  onOpenSettings: (() -> Unit)? = null,
  onEnterPip: (() -> Unit)? = null,
  onOpenTracker: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val colors = LocalQuranColors.current

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      // Top status row: Window and Step Indicator
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Step Type Badge
        val badgeLabel = when {
          state.isRecordingRecitation -> "🔴 REC"
          state.stepType == StepType.SOLO -> "🎯 SOLO"
          state.stepType == StepType.CONNECT -> "🔗 CONNECT"
          state.stepType == StepType.PAUSE_FOR_RECITATION -> "🎙️ RECITE"
          state.stepType == StepType.BRIDGE -> "🌉 BRIDGE"
          else -> ""
        }
        val badgeColor = when {
          state.isRecordingRecitation -> MaterialTheme.colorScheme.error
          state.stepType == StepType.CONNECT -> MaterialTheme.colorScheme.primary
          state.stepType == StepType.PAUSE_FOR_RECITATION -> MaterialTheme.colorScheme.tertiary
          state.stepType == StepType.BRIDGE -> MaterialTheme.colorScheme.secondary
          else -> MaterialTheme.colorScheme.outline
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = if (onOpenSettings != null) Modifier.clickable { onOpenSettings() } else Modifier
        ) {
          Surface(
            color = badgeColor,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Text(
              text = badgeLabel,
              color = MaterialTheme.colorScheme.onPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Text(
            text = stringResource(CoreR.string.memorize_window_indicator, state.currentWindowIndex + 1, state.totalWindows),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.defaultTextColor
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (onOpenTracker != null) {
            TextButton(
              onClick = onOpenTracker,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
              Icon(
                QuranIcons.List,
                contentDescription = stringResource(CoreR.string.memorize_tracker_btn),
                modifier = Modifier
                  .size(16.dp)
                  .padding(end = 2.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Text(
                text = stringResource(CoreR.string.memorize_tracker_btn),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          if (onOpenSettings != null) {
            TextButton(
              onClick = onOpenSettings,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
              Icon(
                QuranIcons.Tune,
                contentDescription = stringResource(CoreR.string.memorize_tune_btn),
                modifier = Modifier
                  .size(16.dp)
                  .padding(end = 2.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Text(
                text = stringResource(CoreR.string.memorize_tune_btn),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          if (onEnterPip != null) {
            TextButton(
              onClick = onEnterPip,
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
              Text(
                text = stringResource(CoreR.string.memorize_float_btn),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          if (state.lastRecitationAudioPath != null && onReplayRecitation != null) {
            TextButton(onClick = onReplayRecitation) {
              Icon(
                QuranIcons.Mic,
                contentDescription = stringResource(CoreR.string.memorize_replay_voice_btn),
                modifier = Modifier.padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Text(
                text = stringResource(CoreR.string.memorize_replay_voice_btn),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          // Tap-to-peek toggle for blind self-recitation test
          TextButton(
            onClick = onToggleMask,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
          ) {
            Icon(
              imageVector = if (state.isTextMasked) QuranIcons.Visibility else QuranIcons.VisibilityOff,
              contentDescription = null,
              modifier = Modifier.size(15.dp),
              tint = if (state.isTextMasked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = if (state.isTextMasked) stringResource(CoreR.string.memorize_peek_text_btn) else stringResource(CoreR.string.memorize_hide_text_btn),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (state.isTextMasked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      // Center summary text: e.g. "Connecting Ayah 1–3 (Repeat 2/2)"
      Text(
        text = state.hudSummary,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.defaultTextColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = onOpenSettings != null) { onOpenSettings?.invoke() }
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Playback Controls Row
      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Exit / Stop Button
        IconButton(onClick = onStop) {
          Icon(
            QuranIcons.Close,
            contentDescription = stringResource(CoreR.string.memorize_exit_btn),
            tint = colors.defaultTextColor
          )
        }

        // Previous Step Button
        IconButton(onClick = onPreviousStep) {
          Icon(
            QuranIcons.FastRewind,
            contentDescription = stringResource(CoreR.string.memorize_prev_step_btn),
            tint = colors.defaultTextColor
          )
        }

        // Play / Pause Action Button
        IconButton(onClick = onPlayPause) {
          Icon(
            if (state.isPaused) QuranIcons.PlayArrow else QuranIcons.Pause,
            contentDescription = if (state.isPaused) stringResource(CoreR.string.memorize_resume) else stringResource(CoreR.string.memorize_pause),
            tint = MaterialTheme.colorScheme.primary
          )
        }

        // Skip Step Button
        IconButton(onClick = onSkipStep) {
          Icon(
            QuranIcons.FastForward,
            contentDescription = stringResource(CoreR.string.memorize_skip_step_btn),
            tint = colors.defaultTextColor
          )
        }

        // Flag Difficult Ayah Button
        if (onToggleFlagAyah != null) {
          IconButton(onClick = onToggleFlagAyah) {
            Icon(
              imageVector = QuranIcons.Flag,
              contentDescription = if (isCurrentAyahFlagged) {
                stringResource(CoreR.string.memorize_flagged_difficult_btn)
              } else {
                stringResource(CoreR.string.memorize_flag_difficult_btn)
              },
              tint = if (isCurrentAyahFlagged) Color(0xFFEF4444) else colors.defaultTextColor
            )
          }
        }

        // Playback Speed Cycle Button
        if (onCycleSpeed != null) {
          TextButton(
            onClick = onCycleSpeed,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
          ) {
            Text(
              text = "⚡ ${playbackSpeed}x",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        // Live Settings Tuning Button
        if (onOpenSettings != null) {
          IconButton(onClick = onOpenSettings) {
            Icon(
              QuranIcons.Tune,
              contentDescription = "Tuning Memorization Settings",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }
    }
  }
}
