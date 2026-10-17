package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip
import com.quran.labs.androidquran.dao.translation.TranslationItem

private val Emerald = Color(0xFF10B981)
private val DarkBg = Color(0xFF09090B)
private val SurfaceDark = Color(0xFF18181B)
private val BorderDark = Color(0xFF27272A)
private val TextWhite = Color(0xFFF4F4F5)
private val TextMuted = Color(0xFFA1A1AA)

/**
 * Modern Material 3 Translation & Tafsir Manager Screen.
 * Provides live search, filter chips, clean card groupings, priority reordering,
 * and seamless download management.
 */
@Composable
fun ModernTranslationScreen(
  items: List<TranslationItem>,
  isRefreshing: Boolean,
  downloadingItemId: Int?,
  onRefresh: () -> Unit,
  onBack: () -> Unit,
  onDownload: (TranslationItem) -> Unit,
  onRemove: (TranslationItem) -> Unit,
  onMoveUp: (TranslationItem) -> Unit,
  onMoveDown: (TranslationItem) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("All") }
  var itemPendingRemoval by remember { mutableStateOf<TranslationItem?>(null) }

  // Partition into downloaded and available
  val (downloadedList, availableList) = items.partition { it.exists() }
  val sortedDownloads = remember(downloadedList) { downloadedList.sortedBy { it.displayOrder } }

  // Filter logic
  fun matchesSearch(item: TranslationItem): Boolean {
    if (searchQuery.isBlank()) return true
    val q = searchQuery.trim().lowercase()
    val name = item.name().lowercase()
    val translator = (item.translation.translatorNameLocalized ?: item.translation.translator ?: "").lowercase()
    return name.contains(q) || translator.contains(q)
  }

  val filteredDownloaded = sortedDownloads.filter { item ->
    matchesSearch(item) && (selectedFilter == "All" || selectedFilter == "Downloaded")
  }

  val filteredAvailable = availableList.filter { item ->
    matchesSearch(item) && (selectedFilter == "All" || selectedFilter == "Available")
  }

  // Removal confirmation dialog
  itemPendingRemoval?.let { targetItem ->
    AlertDialog(
      onDismissRequest = { itemPendingRemoval = null },
      containerColor = SurfaceDark,
      title = {
        Text(
          text = stringResource(R.string.remove_dlg_title),
          color = TextWhite,
          fontWeight = FontWeight.SemiBold,
          fontSize = 18.sp
        )
      },
      text = {
        Text(
          text = String.format(stringResource(R.string.remove_dlg_msg), targetItem.name()),
          color = TextMuted,
          fontSize = 14.sp
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            onRemove(targetItem)
            itemPendingRemoval = null
          }
        ) {
          Text(
            text = stringResource(com.quran.mobile.common.ui.core.R.string.remove_button),
            color = Color(0xFFEF4444),
            fontWeight = FontWeight.SemiBold
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { itemPendingRemoval = null }) {
          Text(
            text = stringResource(android.R.string.cancel),
            color = TextMuted
          )
        }
      },
      shape = RoundedCornerShape(20.dp)
    )
  }

  Surface(
    color = DarkBg,
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(
        WindowInsets.systemBars
          .union(WindowInsets.displayCutout)
      )
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Navigation Bar
      TranslationTopBar(
        isRefreshing = isRefreshing,
        onBack = onBack,
        onRefresh = onRefresh
      )

      // Search Bar
      TranslationSearchBar(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        onClear = { searchQuery = "" }
      )

      // Filter Chips
      TranslationFilterChips(
        selectedFilter = selectedFilter,
        downloadedCount = downloadedList.size,
        availableCount = availableList.size,
        onFilterSelected = { selectedFilter = it }
      )

      // Content List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Section 1: Downloaded Translations
        if (filteredDownloaded.isNotEmpty()) {
          item(key = "header_downloaded") {
            SectionHeader(
              title = stringResource(R.string.downloaded_translations),
              count = filteredDownloaded.size,
              isEmerald = true
            )
          }

          itemsIndexed(
            items = filteredDownloaded,
            key = { _, item -> "downloaded_${item.translation.id}" }
          ) { index, item ->
            DownloadedTranslationCard(
              item = item,
              canMoveUp = index > 0,
              canMoveDown = index < filteredDownloaded.size - 1,
              isDownloading = item.translation.id == downloadingItemId,
              onMoveUp = { onMoveUp(item) },
              onMoveDown = { onMoveDown(item) },
              onUpdate = { onDownload(item) },
              onRemove = { itemPendingRemoval = item }
            )
          }
        }

        // Section 2: Available Translations
        if (filteredAvailable.isNotEmpty()) {
          item(key = "header_available") {
            Spacer(modifier = Modifier.height(6.dp))
            SectionHeader(
              title = stringResource(R.string.available_translations),
              count = filteredAvailable.size,
              isEmerald = false
            )
          }

          itemsIndexed(
            items = filteredAvailable,
            key = { _, item -> "available_${item.translation.id}" }
          ) { _, item ->
            AvailableTranslationCard(
              item = item,
              isDownloading = item.translation.id == downloadingItemId,
              onDownload = { onDownload(item) }
            )
          }
        }

        // Empty Search Results
        if (filteredDownloaded.isEmpty() && filteredAvailable.isEmpty()) {
          item(key = "empty_state") {
            EmptyTranslationsState(query = searchQuery)
          }
        }

        item(key = "bottom_spacer") {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

@Composable
private fun TranslationTopBar(
  isRefreshing: Boolean,
  onBack: () -> Unit,
  onRefresh: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    IconButton(onClick = onBack) {
      Icon(
        imageVector = QuranIcons.ArrowBack,
        contentDescription = "Back",
        tint = TextWhite
      )
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(start = 4.dp)
    ) {
      Text(
        text = stringResource(R.string.prefs_translations),
        color = TextWhite,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = stringResource(R.string.menu_translation),
        color = TextMuted,
        fontSize = 12.sp,
        maxLines = 1
      )
    }

    IconButton(
      onClick = onRefresh,
      enabled = !isRefreshing
    ) {
      Icon(
        imageVector = QuranIcons.Sync,
        contentDescription = "Refresh",
        tint = if (isRefreshing) Emerald else TextMuted,
        modifier = if (isRefreshing) Modifier.rotate(rotation) else Modifier
      )
    }
  }
}

@Composable
private fun TranslationSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  onClear: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(14.dp))
      .background(SurfaceDark)
      .border(BorderStroke(0.5.dp, BorderDark), RoundedCornerShape(14.dp))
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Icon(
        imageVector = QuranIcons.Search,
        contentDescription = "Search",
        tint = if (query.isNotEmpty()) Emerald else TextMuted,
        modifier = Modifier.size(18.dp)
      )

      Spacer(modifier = Modifier.width(10.dp))

      BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = TextStyle(
          color = TextWhite,
          fontSize = 14.sp
        ),
        cursorBrush = SolidColor(Emerald),
        singleLine = true,
        modifier = Modifier.weight(1f),
        decorationBox = { innerTextField ->
          if (query.isEmpty()) {
            Text(
              text = "Search translations or translators...",
              color = TextMuted.copy(alpha = 0.6f),
              fontSize = 14.sp
            )
          }
          innerTextField()
        }
      )

      if (query.isNotEmpty()) {
        Icon(
          imageVector = QuranIcons.Close,
          contentDescription = "Clear",
          tint = TextMuted,
          modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .clickable(onClick = onClear)
        )
      }
    }
  }
}

@Composable
private fun TranslationFilterChips(
  selectedFilter: String,
  downloadedCount: Int,
  availableCount: Int,
  onFilterSelected: (String) -> Unit
) {
  val filters = listOf(
    "All" to (downloadedCount + availableCount),
    "Downloaded" to downloadedCount,
    "Available" to availableCount
  )

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    filters.forEach { (filterName, count) ->
      val isSelected = selectedFilter == filterName
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Emerald.copy(alpha = 0.15f) else SurfaceDark,
        border = BorderStroke(
          width = 0.5.dp,
          color = if (isSelected) Emerald else BorderDark
        ),
        modifier = Modifier.clickable { onFilterSelected(filterName) }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = filterName,
            color = if (isSelected) Emerald else TextWhite,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) Emerald.copy(alpha = 0.25f) else Color(0xFF27272A))
              .padding(horizontal = 6.dp, vertical = 1.dp)
          ) {
            Text(
              text = count.toString(),
              color = if (isSelected) Emerald else TextMuted,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  count: Int,
  isEmerald: Boolean
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(if (isEmerald) Emerald else TextMuted)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = title.uppercase(),
      color = if (isEmerald) Emerald else TextMuted,
      fontWeight = FontWeight.Bold,
      fontSize = 12.sp,
      letterSpacing = 0.8.sp
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = "($count)",
      color = TextMuted.copy(alpha = 0.7f),
      fontSize = 12.sp
    )
  }
}

@Composable
private fun DownloadedTranslationCard(
  item: TranslationItem,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  isDownloading: Boolean,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onUpdate: () -> Unit,
  onRemove: () -> Unit
) {
  val subtitle = item.translation.translatorNameLocalized ?: item.translation.translator

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceDark,
    border = BorderStroke(0.5.dp, BorderDark),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Emerald Checkmark Icon Capsule
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Emerald.copy(alpha = 0.12f))
          .border(BorderStroke(0.5.dp, Emerald.copy(alpha = 0.35f)), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = QuranIcons.Check,
          contentDescription = "Downloaded",
          tint = Emerald,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Title & Subtitle Info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.name(),
          color = TextWhite,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        // Needs Upgrade badge
        if (item.needsUpgrade()) {
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Emerald.copy(alpha = 0.15f))
              .border(BorderStroke(0.5.dp, Emerald.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
              .clickable(onClick = onUpdate)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Icon(
              painter = painterResource(id = com.quran.mobile.feature.downloadmanager.R.drawable.ic_download),
              contentDescription = "Update",
              tint = Emerald,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.update_available),
              color = Emerald,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Reordering Arrows (Up / Down)
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onMoveUp,
          enabled = canMoveUp,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = QuranIcons.KeyboardArrowUp,
            contentDescription = "Move Up",
            tint = if (canMoveUp) TextWhite else TextMuted.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
          )
        }

        IconButton(
          onClick = onMoveDown,
          enabled = canMoveDown,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = QuranIcons.KeyboardArrowDown,
            contentDescription = "Move Down",
            tint = if (canMoveDown) TextWhite else TextMuted.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Delete Button
        IconButton(
          onClick = onRemove,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = QuranIcons.Delete,
            contentDescription = "Remove",
            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun AvailableTranslationCard(
  item: TranslationItem,
  isDownloading: Boolean,
  onDownload: () -> Unit
) {
  val subtitle = item.translation.translatorNameLocalized ?: item.translation.translator

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = SurfaceDark,
    border = BorderStroke(0.5.dp, BorderDark),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = !isDownloading, onClick = onDownload)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Book Icon Capsule
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(Color(0xFF27272A))
          .border(BorderStroke(0.5.dp, BorderDark), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = QuranIcons.MenuBook,
          contentDescription = null,
          tint = TextMuted,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Title & Subtitle Info
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.name(),
          color = TextWhite,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        if (!subtitle.isNullOrBlank()) {
          Text(
            text = subtitle,
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      // Download Action
      if (isDownloading) {
        CircularProgressIndicator(
          color = Emerald,
          strokeWidth = 2.5.dp,
          modifier = Modifier
            .size(24.dp)
            .padding(2.dp)
        )
      } else {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Emerald.copy(alpha = 0.12f))
            .border(BorderStroke(0.5.dp, Emerald.copy(alpha = 0.35f)), CircleShape)
            .clickable(onClick = onDownload),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            painter = painterResource(id = com.quran.mobile.feature.downloadmanager.R.drawable.ic_download),
            contentDescription = "Download",
            tint = Emerald,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyTranslationsState(query: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 48.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(CircleShape)
        .background(Color(0xFF27272A)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = QuranIcons.Search,
        contentDescription = null,
        tint = TextMuted,
        modifier = Modifier.size(24.dp)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = if (query.isNotBlank()) "No translations found matching \"$query\"" else "No translations available",
      color = TextWhite,
      fontSize = 15.sp,
      fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "Try adjusting your search terms or filters",
      color = TextMuted,
      fontSize = 12.sp
    )
  }
}
