package com.quran.labs.androidquran.ui.modern

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernBackupRestoreSheet(
  onExportJson: () -> String,
  onImportJson: (String) -> Boolean,
  onDismissRequest: () -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  val context = LocalContext.current
  val clipboardManager = remember {
    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  }

  var selectedTab by remember { mutableIntStateOf(0) }
  var restoreInputText by remember { mutableStateOf("") }
  var exportJsonText by remember { mutableStateOf(onExportJson()) }
  var exportCopied by remember { mutableStateOf(false) }
  var restoreStatus by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 28.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = QuranIcons.CloudUpload,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Backup & Restore",
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Safeguard & transfer your memorization data",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Box(
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onDismissRequest),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = QuranIcons.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

      // Tab selector: Export vs Restore
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = MaterialTheme.colorScheme.primary
          )
        }
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = {
            Icon(
              imageVector = QuranIcons.CloudUpload,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
          },
          text = {
            Text(
              text = "Export Backup",
              fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = {
            Icon(
              imageVector = QuranIcons.CloudDownload,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
          },
          text = {
            Text(
              text = "Restore Backup",
              fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (selectedTab == 0) {
        // EXPORT VIEW
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          ModernCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            borderWidth = 1.dp,
            contentPadding = 14.dp
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "💾 Export Hifz Data",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Creates a complete snapshot of all your profiles, memorized Surahs, spaced repetition intervals, mistake flags, habit streaks, and active Hifz target plans.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
              )
            }
          }

          // JSON Preview box
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
              .padding(12.dp)
          ) {
            Text(
              text = exportJsonText,
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 14.sp
            )
          }

          Button(
            onClick = {
              val clip = ClipData.newPlainText("Hifz Backup", exportJsonText)
              clipboardManager.setPrimaryClip(clip)
              exportCopied = true
              Toast.makeText(context, "✅ Hifz backup copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            Text(
              text = if (exportCopied) "✓ Backup Copied to Clipboard!" else "📋 Copy Backup to Clipboard",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
        }
      } else {
        // RESTORE VIEW
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          ModernCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            borderWidth = 1.dp,
            contentPadding = 14.dp
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "📥 Restore Hifz Data",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Paste a previously exported Hifz JSON snapshot to restore all memorization records, profiles, and streak history.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
              )
            }
          }

          OutlinedTextField(
            value = restoreInputText,
            onValueChange = {
              restoreInputText = it
              restoreStatus = null
            },
            placeholder = { Text("Paste Hifz JSON backup here...", fontSize = 12.sp) },
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurface
            ),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            )
          )

          val currentStatus = restoreStatus
          if (currentStatus != null) {
            Text(
              text = currentStatus,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (currentStatus.startsWith("✅")) Color(0xFF10B981) else Color(0xFFEF4444)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = {
                val clip = clipboardManager.primaryClip
                if (clip != null && clip.itemCount > 0) {
                  val text = clip.getItemAt(0).text?.toString() ?: ""
                  if (text.isNotBlank()) {
                    restoreInputText = text
                    restoreStatus = null
                  } else {
                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                  }
                } else {
                  Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier
                .weight(1f)
                .height(48.dp),
              shape = RoundedCornerShape(14.dp)
            ) {
              Text("📋 Paste", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
              onClick = {
                if (restoreInputText.isBlank()) {
                  restoreStatus = "⚠️ Please paste backup JSON text first."
                  return@Button
                }
                val success = onImportJson(restoreInputText)
                if (success) {
                  restoreStatus = "✅ Data restored successfully! Refreshing..."
                  Toast.makeText(context, "✅ Hifz data restored successfully!", Toast.LENGTH_LONG).show()
                  onDismissRequest()
                } else {
                  restoreStatus = "❌ Invalid JSON backup format. Please verify your backup string."
                }
              },
              modifier = Modifier
                .weight(1.3f)
                .height(48.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Text("Restore Data", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
