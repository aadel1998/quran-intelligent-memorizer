package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationState
import com.quran.labs.androidquran.common.audio.memorization.SavedSuraSession
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.mobile.common.ui.core.R as UiCoreR

/**
 * Bottom sheet modal displaying the Surah Memorization Tracker.
 * Allows the user to view all in-progress Surahs, resume where they left off,
 * or restart any Surah from the beginning — even while playback is actively running.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationTrackerSheet(
  savedSurahSessions: List<SavedSuraSession>,
  onResumeSession: (MemorizationConfig) -> Unit,
  onStartFromBeginning: (MemorizationConfig) -> Unit,
  onClearSession: (Int) -> Unit,
  onDismissRequest: () -> Unit,
  currentActiveState: MemorizationState? = null,
  onRestartCurrentSession: (() -> Unit)? = null,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  val context = LocalContext.current
  val suraNames = remember {
    try {
      context.resources.getStringArray(UiCoreR.array.sura_names)
    } catch (e: Exception) {
      emptyArray()
    }
  }

  fun getSuraTitle(sura: Int): String {
    val name = if (sura in 1..suraNames.size) suraNames[sura - 1] else "Surah $sura"
    return "$sura. $name"
  }

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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Surah Memorization Tracker",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Manage your progress across multiple Surahs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = onDismissRequest) {
          Icon(QuranIcons.Close, contentDescription = "Close")
        }
      }

      // If an active session is currently playing/paused
      val currentConfig = currentActiveState?.config
      if (currentActiveState != null && currentActiveState.isActive && currentConfig != null) {
        Spacer(modifier = Modifier.height(14.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Step ${currentActiveState.currentStepIndex + 1} of ${currentActiveState.totalSteps}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = getSuraTitle(currentConfig.startAyah.sura),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
              text = "Ayah ${currentConfig.startAyah.ayah}–${currentConfig.endAyah.ayah} • Window ${currentActiveState.currentWindowIndex + 1}/${currentActiveState.totalWindows}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            if (onRestartCurrentSession != null) {
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedButton(
                onClick = onRestartCurrentSession,
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(QuranIcons.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(UiCoreR.string.memo_setup_restart_surah))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "In-Progress Surahs (${savedSurahSessions.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      if (savedSurahSessions.isEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "No other Surahs in progress yet",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "When you memorize any Surah, your exact position and settings will be tracked here so you can switch or resume anytime.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        Spacer(modifier = Modifier.height(8.dp))
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          savedSurahSessions.forEach { session ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
              )
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = getSuraTitle(session.sura),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "Ayah ${session.config.startAyah.ayah}–${session.config.endAyah.ayah} • Step ${session.stepIndex + 1}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  IconButton(
                    onClick = { onClearSession(session.sura) }
                  ) {
                    Icon(
                      QuranIcons.Close,
                      contentDescription = "Remove Surah Progress",
                      modifier = Modifier.size(18.dp),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  FilledTonalButton(
                    onClick = { onStartFromBeginning(session.config) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                  ) {
                    Text(stringResource(UiCoreR.string.memorizer_from_beginning), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                  }

                  Spacer(modifier = Modifier.width(8.dp))

                  Button(
                    onClick = { onResumeSession(session.config) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                  ) {
                    Text(stringResource(UiCoreR.string.memorize_resume), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
