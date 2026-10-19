package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.BuildConfig
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip

private val ScreenBg = Color(0xFF09090B)
private val CardBg = Color(0xFF18181B)
private val CardBorder = Color(0xFF27272A)
private val EmeraldAccent = Color(0xFF10B981)
private val TextMuted = Color(0xFFA1A1AA)

/**
 * Modern Material 3 Advanced Settings & Storage Screen.
 * Provides backup import/export, CSV export, storage management, and diagnostics.
 */
@Composable
fun ModernAdvancedSettingsScreen(
  onBackClick: () -> Unit,
  onImportBookmarks: () -> Unit,
  onExportBookmarks: () -> Unit,
  onExportCsv: () -> Unit,
  onSendLogs: () -> Unit,
  currentStorageLocation: String,
  storageSpaceSummary: String? = null,
  isMultiStorageAvailable: Boolean = false,
  onStorageLocationClick: () -> Unit = {},
  isExporting: Boolean = false
) {
  val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = ScreenBg
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = statusBarPadding)
    ) {
      // Top Navigation Bar
      ModernAdvancedTopBar(onBackClick = onBackClick)

      // Content List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 8.dp,
          bottom = navBarPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Section 1: Backup & Restore
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.prefs_category_advanced),
            icon = QuranIcons.Sync
          ) {
            // Import Bookmarks
            ModernActionItem(
              title = stringResource(R.string.prefs_import_title),
              subtitle = stringResource(R.string.prefs_import_summary),
              valuePill = "JSON",
              icon = QuranIcons.Add,
              onClick = onImportBookmarks
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              thickness = 0.5.dp,
              color = CardBorder
            )

            // Export Bookmarks
            ModernActionItem(
              title = stringResource(R.string.prefs_export_title),
              subtitle = stringResource(R.string.prefs_export_summary),
              valuePill = if (isExporting) "..." else "JSON",
              icon = QuranIcons.Share,
              isLoading = isExporting,
              onClick = onExportBookmarks
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              thickness = 0.5.dp,
              color = CardBorder
            )

            // Export CSV
            ModernActionItem(
              title = stringResource(R.string.prefs_export_csv_title),
              subtitle = stringResource(R.string.prefs_export_csv_summary),
              valuePill = "CSV",
              icon = QuranIcons.MenuBook,
              onClick = onExportCsv
            )
          }
        }

        // Section 2: Storage & Files
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.prefs_app_location_title),
            icon = QuranIcons.Settings
          ) {
            ModernActionItem(
              title = stringResource(R.string.prefs_app_location_title),
              subtitle = storageSpaceSummary ?: currentStorageLocation,
              valuePill = if (isMultiStorageAvailable) "Change" else "Active",
              icon = QuranIcons.Settings,
              onClick = onStorageLocationClick
            )
          }
        }

        // Section 3: System & Diagnostics
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.advanced_diagnostics_specs),
            icon = QuranIcons.Settings
          ) {
            // App Spec Info
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text(
                  text = "Quran for Android",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Build ${BuildConfig.VERSION_NAME} • Flavor ${BuildConfig.FLAVOR}",
                  fontSize = 12.sp,
                  color = TextMuted
                )
              }

              ModernBadgeChip(
                text = "v${BuildConfig.VERSION_NAME}",
                backgroundColor = EmeraldAccent.copy(alpha = 0.15f),
                contentColor = EmeraldAccent
              )
            }

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              thickness = 0.5.dp,
              color = CardBorder
            )

            // Send Logs
            ModernActionItem(
              title = stringResource(R.string.prefs_send_logs_title),
              subtitle = stringResource(R.string.prefs_send_logs_summary),
              valuePill = "Logs",
              icon = QuranIcons.Chat,
              onClick = onSendLogs
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ModernAdvancedTopBar(
  onBackClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onBackClick,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(CardBg)
      ) {
        Icon(
          imageVector = QuranIcons.ArrowBack,
          contentDescription = "Back",
          tint = Color.White
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Text(
        text = stringResource(R.string.prefs_category_advanced),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }

    ModernBadgeChip(
      text = "Data & Storage",
      leadingDotColor = EmeraldAccent
    )
  }
}

@Composable
private fun ModernSettingsGroup(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 4.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(EmeraldAccent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = EmeraldAccent,
          modifier = Modifier.size(14.dp)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = EmeraldAccent
      )
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = CardBg,
      border = BorderStroke(0.5.dp, CardBorder)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        content()
      }
    }
  }
}

@Composable
private fun ModernActionItem(
  title: String,
  subtitle: String?,
  valuePill: String?,
  icon: ImageVector? = null,
  isLoading: Boolean = false,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (icon != null) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF27272A)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          color = Color.White
        )
        if (!subtitle.isNullOrEmpty()) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = subtitle,
            fontSize = 12.sp,
            color = TextMuted,
            lineHeight = 16.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = EmeraldAccent
      )
    } else {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (!valuePill.isNullOrEmpty()) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(EmeraldAccent.copy(alpha = 0.15f))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = valuePill,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = EmeraldAccent
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
          imageVector = QuranIcons.ChevronRight,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
