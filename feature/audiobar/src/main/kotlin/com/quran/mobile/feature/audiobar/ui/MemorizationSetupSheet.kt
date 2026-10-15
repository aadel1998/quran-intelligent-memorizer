package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDelayMode

/**
 * Bottom sheet modal dialog for configuring and launching an Intelligent Quran Memorization session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationSetupSheet(
  initialStartAyah: SuraAyah,
  initialEndAyah: SuraAyah,
  onStartMemorization: (MemorizationConfig) -> Unit,
  onDismissRequest: () -> Unit,
  savedSessionConfig: MemorizationConfig? = null,
  savedSessionStep: Int = 0,
  onResumeSession: (() -> Unit)? = null,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  var startAyahNum by remember { mutableIntStateOf(initialStartAyah.ayah) }
  var endAyahNum by remember { mutableIntStateOf(initialEndAyah.ayah.coerceAtLeast(initialStartAyah.ayah + 3)) }
  var windowSize by remember { mutableIntStateOf(4) }
  var singleRepeat by remember { mutableIntStateOf(3) }
  var connectRepeat by remember { mutableIntStateOf(2) }
  var selectedDelayOption by remember { mutableIntStateOf(0) } // 0: None, 1: 3s, 2: 5s, 3: Match Duration
  var memoryTestMode by remember { mutableStateOf(false) }
  var enableBridge by remember { mutableStateOf(true) }

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
      Text(
        text = "Intelligent Memorization",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = "Surah ${initialStartAyah.sura} • Progressive Sliding Window Learning",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (savedSessionConfig != null && onResumeSession != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Resume Last Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Surah ${savedSessionConfig.startAyah.sura}:${savedSessionConfig.startAyah.ayah}–${savedSessionConfig.endAyah.ayah} • Step ${savedSessionStep + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onResumeSession,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              )
            ) {
              Text("Resume")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Ayah Range Selector
      Text(
        text = "Ayah Range",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        NumberStepper(
          label = "From Ayah",
          value = startAyahNum,
          onValueChange = {
            startAyahNum = it.coerceAtLeast(1)
            if (endAyahNum < startAyahNum) endAyahNum = startAyahNum
          }
        )

        NumberStepper(
          label = "To Ayah",
          value = endAyahNum,
          onValueChange = {
            endAyahNum = it.coerceAtLeast(startAyahNum)
          }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Parameters: Window Size, Single Repeat, Connect Repeat
      Text(
        text = "Memorization Parameters",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        NumberStepper(
          label = "Window Size",
          value = windowSize,
          onValueChange = { windowSize = it.coerceIn(1, 10) }
        )

        NumberStepper(
          label = "Solo Repeat",
          value = singleRepeat,
          onValueChange = { singleRepeat = it.coerceIn(1, 10) }
        )

        NumberStepper(
          label = "Connect Repeat",
          value = connectRepeat,
          onValueChange = { connectRepeat = it.coerceIn(1, 10) }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Delay Mode (Recitation Pause)
      Text(
        text = "Pause Between Repetitions (Silent Recitation)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = selectedDelayOption == 0,
          onClick = { selectedDelayOption = 0 },
          label = { Text("None") }
        )
        FilterChip(
          selected = selectedDelayOption == 1,
          onClick = { selectedDelayOption = 1 },
          label = { Text("3 sec") }
        )
        FilterChip(
          selected = selectedDelayOption == 2,
          onClick = { selectedDelayOption = 2 },
          label = { Text("5 sec") }
        )
        FilterChip(
          selected = selectedDelayOption == 3,
          onClick = { selectedDelayOption = 3 },
          label = { Text("Match Ayah") }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Memory Test Mode Toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Memory Test Mode",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "Hide/Blur text during connection steps (tap to peek)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = memoryTestMode,
          onCheckedChange = { memoryTestMode = it }
        )
      }

      // Bridge Step Toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Connect Windows with Bridge Step",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "Recites transition between consecutive windows",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = enableBridge,
          onCheckedChange = { enableBridge = it }
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = onDismissRequest) {
          Text("Cancel")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = {
            val delayMode = when (selectedDelayOption) {
              1 -> MemorizationDelayMode.FixedSeconds(3)
              2 -> MemorizationDelayMode.FixedSeconds(5)
              3 -> MemorizationDelayMode.MatchAyahDuration
              else -> MemorizationDelayMode.None
            }

            val config = MemorizationConfig(
              startAyah = SuraAyah(initialStartAyah.sura, startAyahNum),
              endAyah = SuraAyah(initialStartAyah.sura, endAyahNum),
              windowSize = windowSize,
              singleRepeat = singleRepeat,
              connectRepeat = connectRepeat,
              delayMode = delayMode,
              memoryTestMode = memoryTestMode,
              enableBridge = enableBridge
            )

            onStartMemorization(config)
          }
        ) {
          Text("Start Memorizing", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun NumberStepper(
  label: String,
  value: Int,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      FilledTonalButton(
        onClick = { onValueChange(value - 1) },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
      ) {
        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      Text(
        text = value.toString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 12.dp)
      )

      FilledTonalButton(
        onClick = { onValueChange(value + 1) },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
      ) {
        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
