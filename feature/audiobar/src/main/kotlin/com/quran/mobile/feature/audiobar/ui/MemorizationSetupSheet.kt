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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.MaskingLevel
import com.quran.labs.androidquran.common.audio.memorization.MasteredRange
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDefaultSettings
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDelayMode
import com.quran.labs.androidquran.common.audio.memorization.SavedSuraSession
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.mobile.common.ui.core.R as UiCoreR

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
  onStartFromBeginning: (() -> Unit)? = null,
  onResetSurah: (() -> Unit)? = null,
  defaultSettings: MemorizationDefaultSettings = MemorizationDefaultSettings(),
  allSavedSurahs: List<SavedSuraSession> = emptyList(),
  onResumeOtherSura: ((SavedSuraSession) -> Unit)? = null,
  onStartOtherSura: ((SavedSuraSession) -> Unit)? = null,
  onClearOtherSura: ((Int) -> Unit)? = null,
  masteredRanges: List<MasteredRange> = emptyList(),
  onReviewRange: ((MasteredRange) -> Unit)? = null,
  savedPlaylists: List<CustomPlaylist> = emptyList(),
  initialPlaylist: CustomPlaylist? = null,
  maxAyahsForSura: Int = 286,
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

  var activeSavedSessionConfig by remember { mutableStateOf(savedSessionConfig) }
  var activeSavedSessionStep by remember { mutableIntStateOf(savedSessionStep) }

  var selectedPlaylist by remember { mutableStateOf<CustomPlaylist?>(initialPlaylist) }
  var startAyahNum by remember { mutableIntStateOf(initialStartAyah.ayah) }
  var endAyahNum by remember { mutableIntStateOf(initialEndAyah.ayah.coerceIn(initialStartAyah.ayah, maxAyahsForSura)) }
  var isIntelliJMode by remember { mutableStateOf(savedSessionConfig?.isIntelliJMode ?: true) }
  var rangeRepeat by remember { mutableIntStateOf(savedSessionConfig?.rangeRepeat ?: 1) }
  var windowSize by remember { mutableIntStateOf(defaultSettings.windowSize) }
  var singleRepeat by remember { mutableIntStateOf(defaultSettings.singleRepeat) }
  var connectRepeat by remember { mutableIntStateOf(defaultSettings.connectRepeat) }
  val initialDelayOption = when (val dm = defaultSettings.delayMode) {
    is MemorizationDelayMode.FixedSeconds -> when (dm.seconds) {
      1 -> 1
      2 -> 2
      3 -> 3
      else -> 4
    }
    is MemorizationDelayMode.MatchAyahDuration -> 5
    else -> 0
  }
  var selectedDelayOption by remember { mutableIntStateOf(initialDelayOption) }
  var maskingLevel by remember { mutableStateOf(defaultSettings.maskingLevel) }
  var recordRecitation by remember { mutableStateOf(defaultSettings.recordRecitation) }
  var enableBridge by remember { mutableStateOf(defaultSettings.enableBridge) }
  var playbackSpeed by remember { mutableFloatStateOf(defaultSettings.playbackSpeed) }

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
        text = "${getSuraTitle(initialStartAyah.sura)} • Progressive Sliding Window Learning",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      val currentSaved = activeSavedSessionConfig
      if (currentSaved != null && onResumeSession != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Previous Progress",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                  text = "${getSuraTitle(currentSaved.startAyah.sura)} (Ayah ${currentSaved.startAyah.ayah}–${currentSaved.endAyah.ayah}) • Step ${activeSavedSessionStep + 1}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
              }

              if (onResetSurah != null) {
                IconButton(
                  onClick = {
                    onResetSurah.invoke()
                    activeSavedSessionConfig = null
                    startAyahNum = 1
                    endAyahNum = maxAyahsForSura
                    windowSize = defaultSettings.windowSize
                    singleRepeat = defaultSettings.singleRepeat
                    connectRepeat = defaultSettings.connectRepeat
                    selectedDelayOption = initialDelayOption
                    maskingLevel = defaultSettings.maskingLevel
                    recordRecitation = defaultSettings.recordRecitation
                    enableBridge = defaultSettings.enableBridge
                  }
                ) {
                  Icon(
                    QuranIcons.Close,
                    contentDescription = "Clear Progress",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              FilledTonalButton(
                onClick = {
                  if (onStartFromBeginning != null) {
                    onStartFromBeginning.invoke()
                  } else {
                    startAyahNum = 1
                    endAyahNum = maxAyahsForSura
                    onStartMemorization(
                      currentSaved.copy(
                        startAyah = SuraAyah(currentSaved.startAyah.sura, 1),
                        endAyah = SuraAyah(currentSaved.startAyah.sura, maxAyahsForSura)
                      )
                    )
                  }
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Text(stringResource(UiCoreR.string.memo_setup_start_beginning), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }

              Spacer(modifier = Modifier.width(8.dp))

              Button(
                onClick = onResumeSession,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary
                )
              ) {
                Text(stringResource(UiCoreR.string.memorizer_resume_step, activeSavedSessionStep + 1), fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      val otherSurahs = allSavedSurahs.filter { it.sura != initialStartAyah.sura }
      if (otherSurahs.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Surah Memorization Tracker (${otherSurahs.size} active)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          otherSurahs.forEach { s ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              )
            ) {
              Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = getSuraTitle(s.sura),
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "Ayah ${s.config.startAyah.ayah}–${s.config.endAyah.ayah} • Step ${s.stepIndex + 1}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  if (onClearOtherSura != null) {
                    IconButton(onClick = { onClearOtherSura(s.sura) }) {
                      Icon(
                        QuranIcons.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  if (onStartOtherSura != null) {
                    FilledTonalButton(
                      onClick = { onStartOtherSura(s) },
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                      Text(stringResource(UiCoreR.string.memorizer_from_beginning), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                  }

                  if (onResumeOtherSura != null) {
                    Button(
                      onClick = { onResumeOtherSura(s) },
                      contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                      Text(stringResource(UiCoreR.string.memorize_resume), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }

      if (masteredRanges.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Recently Mastered & Quick Review",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          masteredRanges.take(3).forEach { range ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Surah ${range.startAyah.sura}:${range.startAyah.ayah}–${range.endAyah.ayah}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Mastered • ${range.repetitions}x repeats",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                FilledTonalButton(
                  onClick = {
                    if (onReviewRange != null) {
                      onReviewRange(range)
                    } else {
                      onStartMemorization(
                        MemorizationConfig(
                          startAyah = range.startAyah,
                          endAyah = range.endAyah,
                          windowSize = 4,
                          singleRepeat = 1,
                          connectRepeat = 2,
                          delayMode = MemorizationDelayMode.None,
                          memoryTestMode = true,
                          maskingLevel = MaskingLevel.MASK_ON_CONNECT,
                          recordRecitation = false,
                          enableBridge = false
                        )
                      )
                    }
                  },
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                  Text(stringResource(UiCoreR.string.memo_setup_review_now), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      if (savedPlaylists.isNotEmpty() || selectedPlaylist != null) {
        Text(
          text = "Memorization Target",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedPlaylist == null,
            onClick = { selectedPlaylist = null },
            label = { Text(stringResource(UiCoreR.string.memo_setup_current_surah)) }
          )

          savedPlaylists.forEach { pl ->
            FilterChip(
              selected = selectedPlaylist?.id == pl.id,
              onClick = { selectedPlaylist = pl },
              label = { Text(pl.name) }
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      val currentPl = selectedPlaylist
      if (currentPl != null) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
          )
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Playlist: ${currentPl.name}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              text = "${currentPl.summary} across Surahs",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      } else {
        // Ayah Range Selector
        Text(
          text = "Ayah Range",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )

        // Quick Range Presets
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          FilterChip(
            selected = startAyahNum == 1 && endAyahNum == maxAyahsForSura,
            onClick = {
              startAyahNum = 1
              endAyahNum = maxAyahsForSura
            },
            label = { Text(stringResource(UiCoreR.string.memo_setup_full_surah), fontSize = 11.sp) }
          )

          if (maxAyahsForSura >= 5) {
            FilterChip(
              selected = startAyahNum == 1 && endAyahNum == 5,
              onClick = {
                startAyahNum = 1
                endAyahNum = 5
              },
              label = { Text("1–5", fontSize = 11.sp) }
            )
          }

          if (maxAyahsForSura >= 10) {
            FilterChip(
              selected = startAyahNum == 1 && endAyahNum == 10,
              onClick = {
                startAyahNum = 1
                endAyahNum = 10
              },
              label = { Text("1–10", fontSize = 11.sp) }
            )
          }

          if (maxAyahsForSura >= 20) {
            FilterChip(
              selected = startAyahNum == 1 && endAyahNum == 20,
              onClick = {
                startAyahNum = 1
                endAyahNum = 20
              },
              label = { Text("1–20", fontSize = 11.sp) }
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          NumberInputOrStepper(
            label = stringResource(UiCoreR.string.memo_setup_from_ayah),
            value = startAyahNum,
            minValue = 1,
            maxValue = maxAyahsForSura,
            onValueChange = { newStart ->
              startAyahNum = newStart
              if (endAyahNum < newStart) endAyahNum = newStart
            }
          )

          NumberInputOrStepper(
            label = stringResource(UiCoreR.string.memo_setup_to_ayah),
            value = endAyahNum,
            minValue = 1,
            maxValue = maxAyahsForSura,
            onValueChange = { newEnd ->
              endAyahNum = newEnd
              if (startAyahNum > newEnd) startAyahNum = newEnd
            }
          )
        }

        if (maxAyahsForSura > 1) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Slide to adjust: Ayah $startAyahNum – $endAyahNum",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
          RangeSlider(
            value = startAyahNum.toFloat()..endAyahNum.toFloat(),
            onValueChange = { range ->
              val newStart = range.start.toInt().coerceIn(1, maxAyahsForSura)
              val newEnd = range.endInclusive.toInt().coerceIn(newStart, maxAyahsForSura)
              startAyahNum = newStart
              endAyahNum = newEnd
            },
            valueRange = 1f..maxAyahsForSura.toFloat(),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

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

      Spacer(modifier = Modifier.height(14.dp))

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
            label = stringResource(UiCoreR.string.memo_setup_window_size),
            value = windowSize,
            onValueChange = { windowSize = it.coerceIn(1, 10) }
          )

          NumberStepper(
            label = stringResource(UiCoreR.string.memo_setup_solo_repeat),
            value = singleRepeat,
            onValueChange = { singleRepeat = it.coerceIn(1, 20) }
          )

          NumberStepper(
            label = stringResource(UiCoreR.string.memo_setup_connect),
            value = connectRepeat,
            onValueChange = { connectRepeat = it.coerceIn(0, 20) }
          )

          NumberStepper(
            label = stringResource(UiCoreR.string.memo_setup_range),
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
            label = stringResource(UiCoreR.string.memo_setup_verse_repeat),
            value = singleRepeat,
            onValueChange = { singleRepeat = it.coerceIn(1, 50) }
          )

          NumberStepper(
            label = stringResource(UiCoreR.string.memo_setup_range_repeat),
            value = rangeRepeat,
            onValueChange = { rangeRepeat = it.coerceIn(1, 50) }
          )
        }
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
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        FilterChip(
          selected = selectedDelayOption == 0,
          onClick = { selectedDelayOption = 0 },
          label = { Text(stringResource(UiCoreR.string.memo_setup_delay_none)) }
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
          label = { Text(stringResource(UiCoreR.string.memo_setup_delay_echo)) }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

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

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Memory Test Masking Difficulty
      Text(
        text = "Memory Test Masking",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Masks Ayah text so you can test your memory (tap Peek anytime to reveal)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = maskingLevel == MaskingLevel.OFF,
          onClick = { maskingLevel = MaskingLevel.OFF },
          label = { Text(stringResource(UiCoreR.string.memo_setup_blur_off)) }
        )
        FilterChip(
          selected = maskingLevel == MaskingLevel.MASK_ON_CONNECT,
          onClick = { maskingLevel = MaskingLevel.MASK_ON_CONNECT },
          label = { Text(stringResource(UiCoreR.string.memo_setup_connect_steps)) }
        )
        FilterChip(
          selected = maskingLevel == MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT,
          onClick = { maskingLevel = MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT },
          label = { Text(stringResource(UiCoreR.string.memo_setup_blur_progressive)) }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Record & Replay My Recitation Toggle
      val canRecord = selectedDelayOption != 0
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Record & Replay My Recitation",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (canRecord) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
          )
          Text(
            text = if (canRecord) "Records your voice during pauses for instant self-check against the Qari"
                   else "Requires a recitation pause duration (select 3s, 5s, or Match Ayah above)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = recordRecitation && canRecord,
          enabled = canRecord,
          onCheckedChange = { recordRecitation = it }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

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
          Text(stringResource(UiCoreR.string.cancel))
        }

        Spacer(modifier = Modifier.width(8.dp))

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

            val pl = selectedPlaylist
            val config = if (pl != null && pl.segments.isNotEmpty()) {
              val firstAyah = SuraAyah(pl.segments.first().sura, pl.segments.first().startAyah)
              val lastAyah = SuraAyah(pl.segments.last().sura, pl.segments.last().endAyah)
              MemorizationConfig(
                startAyah = firstAyah,
                endAyah = lastAyah,
                isIntelliJMode = isIntelliJMode,
                rangeRepeat = rangeRepeat,
                windowSize = windowSize,
                singleRepeat = singleRepeat,
                connectRepeat = connectRepeat,
                delayMode = delayMode,
                memoryTestMode = maskingLevel != MaskingLevel.OFF,
                maskingLevel = maskingLevel,
                recordRecitation = recordRecitation && canRecord,
                enableBridge = enableBridge,
                segments = pl.segments,
                playlistId = pl.id,
                playlistName = pl.name,
                playbackSpeed = playbackSpeed
              )
            } else {
              MemorizationConfig(
                startAyah = SuraAyah(initialStartAyah.sura, startAyahNum),
                endAyah = SuraAyah(initialStartAyah.sura, endAyahNum),
                isIntelliJMode = isIntelliJMode,
                rangeRepeat = rangeRepeat,
                windowSize = windowSize,
                singleRepeat = singleRepeat,
                connectRepeat = connectRepeat,
                delayMode = delayMode,
                memoryTestMode = maskingLevel != MaskingLevel.OFF,
                maskingLevel = maskingLevel,
                recordRecitation = recordRecitation && canRecord,
                enableBridge = enableBridge,
                playbackSpeed = playbackSpeed
              )
            }

            onStartMemorization(config)
          }
        ) {
          Text(stringResource(UiCoreR.string.memo_setup_start), fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
internal fun NumberStepper(
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
