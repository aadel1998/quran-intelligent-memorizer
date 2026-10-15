package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.PlaylistSegment
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.mobile.common.ui.core.R as UiCoreR

/**
 * Bottom sheet modal for creating and editing a custom Quran playlist with multiple Surah segments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistEditorSheet(
  playlistToEdit: CustomPlaylist? = null,
  quranInfo: QuranInfo,
  onSavePlaylist: (CustomPlaylist) -> Unit,
  onDismissRequest: () -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  val context = LocalContext.current
  val suraNames = remember {
    try {
      context.resources.getStringArray(UiCoreR.array.sura_names)
    } catch (e: Exception) {
      Array(114) { "Surah ${it + 1}" }
    }
  }

  fun getSuraTitle(sura: Int): String {
    val name = if (sura - 1 in suraNames.indices) suraNames[sura - 1] else "Surah $sura"
    return "$sura. $name"
  }

  var playlistName by remember { mutableStateOf(playlistToEdit?.name ?: "") }
  var segments by remember { mutableStateOf(playlistToEdit?.segments?.toMutableList() ?: mutableListOf()) }

  // New segment form state
  var selectedSura by remember { mutableIntStateOf(1) }
  var isFullSurah by remember { mutableStateOf(true) }
  val maxAyahsForSelectedSura = remember(selectedSura) {
    quranInfo.getNumberOfAyahs(selectedSura).coerceAtLeast(1)
  }
  var startAyah by remember { mutableIntStateOf(1) }
  var endAyah by remember { mutableIntStateOf(quranInfo.getNumberOfAyahs(1).coerceAtLeast(1)) }

  var suraDropdownExpanded by remember { mutableStateOf(false) }

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
        text = if (playlistToEdit == null) "Create New Playlist" else "Edit Playlist",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Combine Surahs or Ayah sequences into a continuous playlist",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = playlistName,
        onValueChange = { playlistName = it },
        label = { Text("Playlist Name") },
        placeholder = { Text("e.g. Daily Recitation, Morning Adhkar") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Current Segments List
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Playlist Segments (${segments.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        val totalAyahs = segments.sumOf { (it.endAyah - it.startAyah + 1).coerceAtLeast(0) }
        Text(
          text = "$totalAyahs Ayahs total",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (segments.isEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        ) {
          Text(
            text = "No segments added yet. Add your first Surah sequence below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          segments.forEachIndexed { index, segment ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "#${index + 1}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(end = 10.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = getSuraTitle(segment.sura),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  val rangeDesc = if (segment.isFullSurah) {
                    "Full Surah (1–${segment.endAyah})"
                  } else {
                    "Ayah ${segment.startAyah} – ${segment.endAyah} (${segment.endAyah - segment.startAyah + 1} Ayahs)"
                  }
                  Text(
                    text = rangeDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                // Reorder up
                if (index > 0) {
                  IconButton(
                    onClick = {
                      val mutable = segments.toMutableList()
                      val item = mutable.removeAt(index)
                      mutable.add(index - 1, item)
                      segments = mutable
                    }
                  ) {
                    Icon(QuranIcons.KeyboardArrowUp, contentDescription = "Move up")
                  }
                }

                // Reorder down
                if (index < segments.size - 1) {
                  IconButton(
                    onClick = {
                      val mutable = segments.toMutableList()
                      val item = mutable.removeAt(index)
                      mutable.add(index + 1, item)
                      segments = mutable
                    }
                  ) {
                    Icon(QuranIcons.KeyboardArrowDown, contentDescription = "Move down")
                  }
                }

                // Remove
                IconButton(
                  onClick = {
                    val mutable = segments.toMutableList()
                    mutable.removeAt(index)
                    segments = mutable
                  }
                ) {
                  Icon(
                    QuranIcons.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Add New Segment Form
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Add Surah Sequence",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Surah Selector Dropdown
          Box {
            OutlinedButton(
              onClick = { suraDropdownExpanded = true },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = getSuraTitle(selectedSura),
                modifier = Modifier.weight(1f)
              )
              Icon(QuranIcons.ExpandMore, contentDescription = null)
            }

            DropdownMenu(
              expanded = suraDropdownExpanded,
              onDismissRequest = { suraDropdownExpanded = false }
            ) {
              (1..114).forEach { suraNum ->
                DropdownMenuItem(
                  text = { Text(getSuraTitle(suraNum)) },
                  onClick = {
                    selectedSura = suraNum
                    val count = quranInfo.getNumberOfAyahs(suraNum).coerceAtLeast(1)
                    startAyah = 1
                    endAyah = count
                    suraDropdownExpanded = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Full Surah vs Custom Range Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = isFullSurah,
              onClick = {
                isFullSurah = true
                startAyah = 1
                endAyah = maxAyahsForSelectedSura
              },
              label = { Text("Full Surah ($maxAyahsForSelectedSura Ayahs)") }
            )

            FilterChip(
              selected = !isFullSurah,
              onClick = {
                isFullSurah = false
              },
              label = { Text("Custom Range") }
            )
          }

          // Ayah Steppers when Custom Range is active
          if (!isFullSurah) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              NumberStepperCompact(
                label = "From Ayah",
                value = startAyah,
                minValue = 1,
                maxValue = endAyah,
                onValueChange = {
                  startAyah = it.coerceIn(1, endAyah)
                }
              )

              NumberStepperCompact(
                label = "To Ayah",
                value = endAyah,
                minValue = startAyah,
                maxValue = maxAyahsForSelectedSura,
                onValueChange = {
                  endAyah = it.coerceIn(startAyah, maxAyahsForSelectedSura)
                }
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          FilledTonalButton(
            onClick = {
              val newSegment = PlaylistSegment(
                sura = selectedSura,
                startAyah = if (isFullSurah) 1 else startAyah,
                endAyah = if (isFullSurah) maxAyahsForSelectedSura else endAyah,
                isFullSurah = isFullSurah,
                customLabel = getSuraTitle(selectedSura)
              )
              val mutable = segments.toMutableList()
              mutable.add(newSegment)
              segments = mutable

              // Reset to default
              isFullSurah = true
              startAyah = 1
              endAyah = maxAyahsForSelectedSura
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(QuranIcons.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Add to Playlist", fontWeight = FontWeight.SemiBold)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Bottom Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onDismissRequest,
          modifier = Modifier.weight(1f)
        ) {
          Text("Cancel")
        }

        Button(
          onClick = {
            if (playlistName.isNotBlank() && segments.isNotEmpty()) {
              val saved = CustomPlaylist(
                id = playlistToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                name = playlistName.trim(),
                segments = segments,
                createdAt = playlistToEdit?.createdAt ?: System.currentTimeMillis(),
                lastPlayedAt = playlistToEdit?.lastPlayedAt ?: 0L
              )
              onSavePlaylist(saved)
            }
          },
          enabled = playlistName.isNotBlank() && segments.isNotEmpty(),
          modifier = Modifier.weight(1f)
        ) {
          Text("Save Playlist", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun NumberStepperCompact(
  label: String,
  value: Int,
  minValue: Int,
  maxValue: Int,
  onValueChange: (Int) -> Unit
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      FilledTonalButton(
        onClick = { if (value > minValue) onValueChange(value - 1) },
        enabled = value > minValue,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      Text(
        text = value.toString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 10.dp)
      )

      FilledTonalButton(
        onClick = { if (value < maxValue) onValueChange(value + 1) },
        enabled = value < maxValue,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
