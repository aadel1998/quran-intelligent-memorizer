package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import com.quran.labs.androidquran.common.audio.memorization.MaskingLevel
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDelayMode
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.mobile.common.ui.core.R as CoreR

/**
 * Bottom sheet allowing live on-the-fly adjustment of memorization parameters
 * during an active session without resetting current Ayah progress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationLiveSettingsSheet(
  currentConfig: MemorizationConfig,
  onApplySettings: (MemorizationConfig) -> Unit,
  onDismissRequest: () -> Unit,
  isCurrentAyahFlagged: Boolean = false,
  onToggleFlagAyah: (() -> Unit)? = null,
  currentAyahNumber: Int? = null,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  var isIntelliJMode by remember { mutableStateOf(currentConfig.isIntelliJMode) }
  var rangeRepeat by remember { mutableIntStateOf(currentConfig.rangeRepeat) }
  var windowSize by remember { mutableIntStateOf(currentConfig.windowSize) }
  var singleRepeat by remember { mutableIntStateOf(currentConfig.singleRepeat) }
  var connectRepeat by remember { mutableIntStateOf(currentConfig.connectRepeat) }
  var playbackSpeed by remember { mutableFloatStateOf(currentConfig.playbackSpeed) }
  var selectedDelayOption by remember {
    mutableIntStateOf(
      when (val delay = currentConfig.delayMode) {
        is MemorizationDelayMode.None -> 0
        is MemorizationDelayMode.FixedSeconds -> when (delay.seconds) {
          1 -> 1
          2 -> 2
          3 -> 3
          else -> 4
        }
        is MemorizationDelayMode.MatchAyahDuration -> 5
      }
    )
  }
  var maskingLevel by remember { mutableStateOf(currentConfig.maskingLevel) }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = QuranIcons.Tune,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = stringResource(CoreR.string.memorize_live_tuning_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = stringResource(CoreR.string.memorize_live_tuning_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // IntelliJ Mode Toggle Card
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = if (isIntelliJMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                           else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "IntelliJ Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isIntelliJMode) "ON" else "OFF",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIntelliJMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = if (isIntelliJMode) "Auto-connects verses in a sliding window with progressive repeats"
                     else "Linear sequential repetition across the selected range",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = isIntelliJMode,
            onCheckedChange = { isIntelliJMode = it }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Parameters
      Text(
        text = if (isIntelliJMode) "Window & Repetition Parameters" else "Repetition Controls",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      if (isIntelliJMode) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          NumberStepper(
            label = stringResource(CoreR.string.memo_setup_window_size),
            value = windowSize,
            onValueChange = { windowSize = it.coerceIn(1, 10) }
          )

          NumberStepper(
            label = stringResource(CoreR.string.memo_setup_solo_repeat),
            value = singleRepeat,
            onValueChange = { singleRepeat = it.coerceIn(1, 20) }
          )

          NumberStepper(
            label = stringResource(CoreR.string.memo_setup_connect),
            value = connectRepeat,
            onValueChange = { connectRepeat = it.coerceIn(0, 20) }
          )

          NumberStepper(
            label = stringResource(CoreR.string.memo_setup_range),
            value = rangeRepeat,
            onValueChange = { rangeRepeat = it.coerceIn(1, 20) }
          )
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          NumberStepper(
            label = stringResource(CoreR.string.memo_setup_verse_repeat),
            value = singleRepeat,
            onValueChange = { singleRepeat = it.coerceIn(1, 50) }
          )

          NumberStepper(
            label = stringResource(CoreR.string.memo_setup_range_repeat),
            value = rangeRepeat,
            onValueChange = { rangeRepeat = it.coerceIn(1, 50) }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(14.dp))

      // Pause Between Repetitions (Silent Recitation)
      Text(
        text = "Pause Between Repetitions (Silent Recitation)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = selectedDelayOption == 0,
          onClick = { selectedDelayOption = 0 },
          label = { Text(stringResource(CoreR.string.memo_setup_delay_none)) }
        )
        FilterChip(
          selected = selectedDelayOption == 1,
          onClick = { selectedDelayOption = 1 },
          label = { Text("1s") }
        )
        FilterChip(
          selected = selectedDelayOption == 2,
          onClick = { selectedDelayOption = 2 },
          label = { Text("2s") }
        )
        FilterChip(
          selected = selectedDelayOption == 3,
          onClick = { selectedDelayOption = 3 },
          label = { Text("3s") }
        )
        FilterChip(
          selected = selectedDelayOption == 4,
          onClick = { selectedDelayOption = 4 },
          label = { Text("5s") }
        )
        FilterChip(
          selected = selectedDelayOption == 5,
          onClick = { selectedDelayOption = 5 },
          label = { Text(stringResource(CoreR.string.memo_setup_delay_echo)) }
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(14.dp))

      // Reciter Audio Speed
      Text(
        text = "Reciter Audio Speed",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { speed ->
          FilterChip(
            selected = playbackSpeed == speed,
            onClick = { playbackSpeed = speed },
            label = { Text(if (speed == 1.0f) "1.0x" else "${speed}x") }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(14.dp))

      // Progressive Masking Level
      Text(
        text = "Memory Test Masking Level",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = maskingLevel == MaskingLevel.OFF,
          onClick = { maskingLevel = MaskingLevel.OFF },
          label = { Text(stringResource(CoreR.string.memo_setup_blur_off)) }
        )
        FilterChip(
          selected = maskingLevel == MaskingLevel.MASK_ON_CONNECT,
          onClick = { maskingLevel = MaskingLevel.MASK_ON_CONNECT },
          label = { Text(stringResource(CoreR.string.memo_setup_blur_connect)) }
        )
        FilterChip(
          selected = maskingLevel == MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT,
          onClick = { maskingLevel = MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT },
          label = { Text(stringResource(CoreR.string.memo_setup_blur_progressive)) }
        )
      }

      if (onToggleFlagAyah != null && currentAyahNumber != null) {
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Difficult Verse Bookmark",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "Mark Ayah $currentAyahNumber as difficult for mistake review",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = isCurrentAyahFlagged,
            onCheckedChange = { onToggleFlagAyah() }
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onDismissRequest,
          modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(CoreR.string.cancel))
        }

        Button(
          onClick = {
            val delayMode = when (selectedDelayOption) {
              1 -> MemorizationDelayMode.FixedSeconds(1)
              2 -> MemorizationDelayMode.FixedSeconds(2)
              3 -> MemorizationDelayMode.FixedSeconds(3)
              4 -> MemorizationDelayMode.FixedSeconds(5)
              5 -> MemorizationDelayMode.MatchAyahDuration
              else -> MemorizationDelayMode.None
            }
            val memoryTest = maskingLevel != MaskingLevel.OFF
            val updated = currentConfig.copy(
              isIntelliJMode = isIntelliJMode,
              rangeRepeat = rangeRepeat,
              windowSize = windowSize,
              singleRepeat = singleRepeat,
              connectRepeat = connectRepeat,
              delayMode = delayMode,
              memoryTestMode = memoryTest,
              maskingLevel = maskingLevel,
              playbackSpeed = playbackSpeed
            )
            onApplySettings(updated)
          },
          modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(CoreR.string.memo_setup_apply_changes), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
