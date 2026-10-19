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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.PlaylistRepository
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.mobile.common.ui.core.R as UiCoreR

/**
 * Bottom sheet displaying all user-created Quran playlists with options to Play, Memorize, Edit, or Delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListSheet(
  playlistRepository: PlaylistRepository,
  quranInfo: QuranInfo,
  onPlayPlaylist: (CustomPlaylist) -> Unit,
  onMemorizePlaylist: (CustomPlaylist) -> Unit,
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

  fun getSuraShortName(sura: Int): String {
    return if (sura - 1 in suraNames.indices) suraNames[sura - 1] else "Surah $sura"
  }

  var playlists by remember { mutableStateOf(playlistRepository.getAllPlaylists()) }
  var playlistToEdit by remember { mutableStateOf<CustomPlaylist?>(null) }
  var isEditorOpen by remember { mutableStateOf(false) }

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
        Column {
          Text(
            text = "My Quran Playlists",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Custom sequences across Surahs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        FilledTonalButton(
          onClick = {
            playlistToEdit = null
            isEditorOpen = true
          }
        ) {
          Icon(QuranIcons.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
          Text(stringResource(UiCoreR.string.playlist_new))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      if (playlists.isEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              QuranIcons.MenuBook,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.height(40.dp).width(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No playlists created yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Create custom sequences of full Surahs or specific Ayah ranges for continuous listening or memorization.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = {
                playlistToEdit = null
                isEditorOpen = true
              }
            ) {
              Icon(QuranIcons.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
              Text(stringResource(UiCoreR.string.playlist_create_first))
            }
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          playlists.forEach { playlist ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
              )
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = playlist.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = playlist.summary,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }

                  Row {
                    IconButton(
                      onClick = {
                        playlistToEdit = playlist
                        isEditorOpen = true
                      }
                    ) {
                      Icon(QuranIcons.Edit, contentDescription = "Edit")
                    }

                    IconButton(
                      onClick = {
                        playlistRepository.deletePlaylist(playlist.id)
                        playlists = playlistRepository.getAllPlaylists()
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

                Spacer(modifier = Modifier.height(8.dp))

                // Sequence summary preview
                val previewText = playlist.segments.joinToString(" ➔ ") { seg ->
                  if (seg.isFullSurah) {
                    getSuraShortName(seg.sura)
                  } else {
                    "${getSuraShortName(seg.sura)} (${seg.startAyah}–${seg.endAyah})"
                  }
                }
                Text(
                  text = previewText,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons: Play & Memorize
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  FilledTonalButton(
                    onClick = {
                      playlistRepository.updateLastPlayed(playlist.id)
                      onPlayPlaylist(playlist)
                      onDismissRequest()
                    },
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(QuranIcons.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(UiCoreR.string.playlist_play_all))
                  }

                  Button(
                    onClick = {
                      playlistRepository.updateLastPlayed(playlist.id)
                      onMemorizePlaylist(playlist)
                      onDismissRequest()
                    },
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(QuranIcons.MenuBook, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(UiCoreR.string.playlist_memorize), fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Nested Editor Sheet when creating/editing
  if (isEditorOpen) {
    PlaylistEditorSheet(
      playlistToEdit = playlistToEdit,
      quranInfo = quranInfo,
      onSavePlaylist = { saved ->
        playlistRepository.savePlaylist(saved)
        playlists = playlistRepository.getAllPlaylists()
        isEditorOpen = false
      },
      onDismissRequest = {
        isEditorOpen = false
      }
    )
  }
}
