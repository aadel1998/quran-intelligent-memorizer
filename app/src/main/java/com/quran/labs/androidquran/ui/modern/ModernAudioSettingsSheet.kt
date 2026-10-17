package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.ui.core.QuranIcons

/**
 * Modern Material 3 Bottom Sheet for Audio Playback Range, Repeats, and Speed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModernAudioSettingsSheet(
  visible: Boolean,
  startSura: Int,
  startAyah: Int,
  endSura: Int,
  endAyah: Int,
  suraNames: List<String>,
  verseRepeat: Int,
  rangeRepeat: Int,
  restrictToRange: Boolean,
  currentSpeed: Float,
  isPlaying: Boolean,
  numberOfAyahsForSura: (Int) -> Int,
  onApply: (
    newStart: SuraAyah,
    newEnd: SuraAyah,
    newVerseRepeat: Int,
    newRangeRepeat: Int,
    newRestrictToRange: Boolean,
    newSpeed: Float
  ) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedStartSura by remember(startSura, visible) { mutableIntStateOf(startSura) }
  var selectedStartAyah by remember(startAyah, visible) { mutableIntStateOf(startAyah) }
  var selectedEndSura by remember(endSura, visible) { mutableIntStateOf(endSura) }
  var selectedEndAyah by remember(endAyah, visible) { mutableIntStateOf(endAyah) }
  var selectedVerseRepeat by remember(verseRepeat, visible) { mutableIntStateOf(verseRepeat) }
  var selectedRangeRepeat by remember(rangeRepeat, visible) { mutableIntStateOf(rangeRepeat) }
  var selectedRestrictToRange by remember(restrictToRange, visible) { mutableStateOf(restrictToRange) }
  var selectedSpeed by remember(currentSpeed, visible) { mutableFloatStateOf(currentSpeed) }

  var pickingSuraForStart by remember { mutableStateOf<Boolean?>(null) }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut()
  ) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.55f))
        .clickable { onDismiss() }
    ) {
      AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier.align(Alignment.BottomCenter)
      ) {
        Surface(
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 6.dp,
          shadowElevation = 24.dp,
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) {}
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(
                WindowInsets.navigationBars.asPaddingValues()
              )
              .padding(horizontal = 20.dp, vertical = 14.dp)
          ) {
            // Drag Handle
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .width(38.dp)
                  .height(4.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
              )
            }

            // Header Row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                  modifier = Modifier.size(36.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = QuranIcons.Settings,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Column {
                  Text(
                    text = "Audio & Playback Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Configure range, verse repeats, and speed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = QuranIcons.Close,
                  contentDescription = "Close",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
            }

            // Scrollable Content
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              // 1. PLAYBACK RANGE SECTION
              SettingsCard(title = "PLAYBACK RANGE") {
                // Play From
                AyahRangeRow(
                  label = "Play From",
                  suraName = suraNames.getOrNull(selectedStartSura - 1) ?: "Surah $selectedStartSura",
                  suraNumber = selectedStartSura,
                  ayahNumber = selectedStartAyah,
                  maxAyahs = numberOfAyahsForSura(selectedStartSura),
                  onPickSura = { pickingSuraForStart = true },
                  onAyahChange = { newAyah ->
                    selectedStartAyah = newAyah
                    if (selectedStartSura == selectedEndSura && selectedStartAyah > selectedEndAyah) {
                      selectedEndAyah = selectedStartAyah
                    }
                  }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Play To
                AyahRangeRow(
                  label = "Play To",
                  suraName = suraNames.getOrNull(selectedEndSura - 1) ?: "Surah $selectedEndSura",
                  suraNumber = selectedEndSura,
                  ayahNumber = selectedEndAyah,
                  maxAyahs = numberOfAyahsForSura(selectedEndSura),
                  onPickSura = { pickingSuraForStart = false },
                  onAyahChange = { newAyah ->
                    selectedEndAyah = newAyah
                    if (selectedStartSura == selectedEndSura && selectedEndAyah < selectedStartAyah) {
                      selectedStartAyah = selectedEndAyah
                    }
                  }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Restrict to Range Toggle
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { selectedRestrictToRange = !selectedRestrictToRange }
                    .padding(vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = "Restrict to Range",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = "Stop playback when end of range is reached",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  Switch(
                    checked = selectedRestrictToRange,
                    onCheckedChange = { selectedRestrictToRange = it },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = MaterialTheme.colorScheme.primary,
                      checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    )
                  )
                }
              }

              // 2. REPETITION CONTROLS
              SettingsCard(title = "REPETITIONS") {
                // Verse Repeat
                Text(
                  text = "Repeat Each Verse",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  val repeatOptions = listOf(0 to "1x", 1 to "2x", 2 to "3x", 4 to "5x", 9 to "10x", -1 to "∞")
                  repeatOptions.forEach { (count, label) ->
                    val isSelected = selectedVerseRepeat == count
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = if (isSelected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                      border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                      ),
                      modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedVerseRepeat = count }
                    ) {
                      Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Range Repeat
                Text(
                  text = "Repeat Entire Range",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Medium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(bottom = 6.dp)
                )
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  val rangeOptions = listOf(0 to "1x", 1 to "2x", 2 to "3x", 4 to "5x", 9 to "10x", -1 to "∞")
                  rangeOptions.forEach { (count, label) ->
                    val isSelected = selectedRangeRepeat == count
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = if (isSelected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                      border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                      ),
                      modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedRangeRepeat = count }
                    ) {
                      Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                      )
                    }
                  }
                }
              }

              // 3. PLAYBACK SPEED
              SettingsCard(title = "PLAYBACK SPEED") {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
                  speeds.forEach { speed ->
                    val isSelected = selectedSpeed == speed
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = if (isSelected) MaterialTheme.colorScheme.primary
                              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                      border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                      ),
                      modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedSpeed = speed }
                    ) {
                      Text(
                        text = "${speed}x",
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                      )
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Apply / Play Button
            Button(
              onClick = {
                val newStart = SuraAyah(selectedStartSura, selectedStartAyah)
                val newEnd = SuraAyah(selectedEndSura, selectedEndAyah)
                onApply(
                  newStart,
                  newEnd,
                  selectedVerseRepeat,
                  selectedRangeRepeat,
                  selectedRestrictToRange,
                  selectedSpeed
                )
              },
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
              Icon(
                imageVector = if (isPlaying) QuranIcons.Check else QuranIcons.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isPlaying) "Apply Settings" else "Apply & Play",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }
          }
        }
      }
    }
  }

  // Surah Picker Dialog
  if (pickingSuraForStart != null) {
    val forStart = pickingSuraForStart == true
    SuraPickerDialog(
      suraNames = suraNames,
      initialSura = if (forStart) selectedStartSura else selectedEndSura,
      onSelect = { pickedSura ->
        if (forStart) {
          selectedStartSura = pickedSura
          val maxA = numberOfAyahsForSura(pickedSura)
          if (selectedStartAyah > maxA) selectedStartAyah = 1
          if (selectedStartSura > selectedEndSura) {
            selectedEndSura = selectedStartSura
            selectedEndAyah = numberOfAyahsForSura(selectedEndSura)
          }
        } else {
          selectedEndSura = pickedSura
          val maxA = numberOfAyahsForSura(pickedSura)
          if (selectedEndAyah > maxA) selectedEndAyah = maxA
          if (selectedEndSura < selectedStartSura) {
            selectedStartSura = selectedEndSura
            selectedStartAyah = 1
          }
        }
        pickingSuraForStart = null
      },
      onDismiss = { pickingSuraForStart = null }
    )
  }
}

@Composable
private fun SettingsCard(
  title: String,
  content: @Composable () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = androidx.compose.foundation.BorderStroke(
      0.5.dp,
      MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
      )
      content()
    }
  }
}

@Composable
private fun AyahRangeRow(
  label: String,
  suraName: String,
  suraNumber: Int,
  ayahNumber: Int,
  maxAyahs: Int,
  onPickSura: () -> Unit,
  onAyahChange: (Int) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.width(76.dp)
    )

    // Surah selector button
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.surface,
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
      ),
      modifier = Modifier
        .weight(1f)
        .height(36.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable { onPickSura() }
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.padding(horizontal = 10.dp)
      ) {
        Text(
          text = "$suraNumber. $suraName",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )
        Icon(
          imageVector = QuranIcons.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Ayah Stepper: [-] [Ayah] [+]
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      IconButton(
        onClick = { if (ayahNumber > 1) onAyahChange(ayahNumber - 1) },
        enabled = ayahNumber > 1,
        modifier = Modifier.size(32.dp)
      ) {
        Text(
          text = "−",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = if (ayahNumber > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
      }

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
          0.5.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.size(width = 44.dp, height = 32.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "$ayahNumber",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      IconButton(
        onClick = { if (ayahNumber < maxAyahs) onAyahChange(ayahNumber + 1) },
        enabled = ayahNumber < maxAyahs,
        modifier = Modifier.size(32.dp)
      ) {
        Text(
          text = "+",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = if (ayahNumber < maxAyahs) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
      }
    }
  }
}

@Composable
private fun SuraPickerDialog(
  suraNames: List<String>,
  initialSura: Int,
  onSelect: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  var filter by remember { mutableStateOf("") }
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 520.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Select Surah",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(bottom = 10.dp)
        )

        OutlinedTextField(
          value = filter,
          onValueChange = { filter = it },
          placeholder = { Text("Search by name or number...") },
          leadingIcon = { Icon(QuranIcons.Search, contentDescription = null) },
          shape = RoundedCornerShape(12.dp),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
        )

        val filteredSuras = remember(filter, suraNames) {
          val query = filter.trim().lowercase()
          suraNames.mapIndexed { idx, name -> (idx + 1) to name }
            .filter { (num, name) ->
              query.isEmpty() || num.toString() == query || name.lowercase().contains(query)
            }
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          itemsIndexed(filteredSuras) { _, (num, name) ->
            val isCurrent = num == initialSura
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                  if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                  else Color.Transparent
                )
                .clickable { onSelect(num) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "$num. $name",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
              if (isCurrent) {
                Icon(
                  imageVector = QuranIcons.Check,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }

        TextButton(
          onClick = onDismiss,
          modifier = Modifier.align(Alignment.End)
        ) {
          Text("Cancel")
        }
      }
    }
  }
}
