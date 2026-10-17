package com.quran.labs.androidquran.ui.modern

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.dao.BookmarkSortOrder
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip

private val ScreenBg = Color(0xFF09090B)
private val CardBg = Color(0xFF18181B)
private val CardBorder = Color(0xFF27272A)
private val EmeraldAccent = Color(0xFF10B981)
private val TextMuted = Color(0xFFA1A1AA)

sealed class ModernBookmarkRow {
  data class Header(val title: String) : ModernBookmarkRow()
  data class Ayah(
    val id: String,
    val sura: Int,
    val ayah: Int,
    val page: Int,
    val suraName: String,
    val ayahText: String?,
    val metadata: String,
    val timestamp: Long,
    val tagColor: Color? = null
  ) : ModernBookmarkRow()
  data class Page(
    val id: String,
    val page: Int,
    val title: String,
    val metadata: String,
    val timestamp: Long
  ) : ModernBookmarkRow()
}

/**
 * Modern Material 3 Screen for Tag Collections and Color Highlights.
 */
@Composable
fun ModernBookmarkListScreen(
  title: String,
  subtitle: String? = null,
  items: List<ModernBookmarkRow>,
  sortOrder: Int,
  canRename: Boolean = false,
  highlightColor: Color? = null,
  onBackClick: () -> Unit,
  onSortChanged: (Int) -> Unit,
  onRenameClick: () -> Unit = {},
  onItemClick: (ModernBookmarkRow) -> Unit,
  onDeleteItem: (ModernBookmarkRow) -> Unit
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
      ModernBookmarkListTopBar(
        title = title,
        itemCount = items.count { it !is ModernBookmarkRow.Header },
        sortOrder = sortOrder,
        canRename = canRename,
        highlightColor = highlightColor,
        onBackClick = onBackClick,
        onSortChanged = onSortChanged,
        onRenameClick = onRenameClick
      )

      if (items.isEmpty()) {
        // Empty State
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(EmeraldAccent.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = QuranIcons.BookmarkBorder,
                contentDescription = null,
                tint = EmeraldAccent,
                modifier = Modifier.size(36.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = stringResource(R.string.collection_empty),
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "Bookmarks and highlights added to this collection will appear here.",
              fontSize = 13.sp,
              color = TextMuted,
              lineHeight = 18.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      } else {
        // Items List
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
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          items(items, key = { item ->
            when (item) {
              is ModernBookmarkRow.Header -> "header_${item.title}"
              is ModernBookmarkRow.Ayah -> "ayah_${item.id}_${item.sura}_${item.ayah}"
              is ModernBookmarkRow.Page -> "page_${item.id}_${item.page}"
            }
          }) { row ->
            when (row) {
              is ModernBookmarkRow.Header -> {
                Text(
                  text = row.title,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = EmeraldAccent,
                  modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
                )
              }

              is ModernBookmarkRow.Ayah -> {
                ModernAyahBookmarkCard(
                  ayah = row,
                  onClick = { onItemClick(row) },
                  onDelete = { onDeleteItem(row) }
                )
              }

              is ModernBookmarkRow.Page -> {
                ModernPageBookmarkCard(
                  page = row,
                  onClick = { onItemClick(row) },
                  onDelete = { onDeleteItem(row) }
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ModernBookmarkListTopBar(
  title: String,
  itemCount: Int,
  sortOrder: Int,
  canRename: Boolean,
  highlightColor: Color?,
  onBackClick: () -> Unit,
  onSortChanged: (Int) -> Unit,
  onRenameClick: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
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

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (highlightColor != null) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(highlightColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        if (itemCount > 0) {
          Text(
            text = "$itemCount saved",
            fontSize = 12.sp,
            color = TextMuted
          )
        }
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      // Sort Menu Button
      Box {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CardBg)
        ) {
          Icon(
            imageVector = QuranIcons.List,
            contentDescription = "Sort",
            tint = Color.White
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier.background(CardBg)
        ) {
          DropdownMenuItem(
            text = {
              Text(
                text = stringResource(R.string.menu_sort_date),
                color = if (sortOrder == BookmarkSortOrder.SORT_DATE_ADDED) EmeraldAccent else Color.White,
                fontWeight = if (sortOrder == BookmarkSortOrder.SORT_DATE_ADDED) FontWeight.Bold else FontWeight.Normal
              )
            },
            onClick = {
              onSortChanged(BookmarkSortOrder.SORT_DATE_ADDED)
              showMenu = false
            }
          )
          DropdownMenuItem(
            text = {
              Text(
                text = stringResource(R.string.menu_sort_location),
                color = if (sortOrder == BookmarkSortOrder.SORT_LOCATION) EmeraldAccent else Color.White,
                fontWeight = if (sortOrder == BookmarkSortOrder.SORT_LOCATION) FontWeight.Bold else FontWeight.Normal
              )
            },
            onClick = {
              onSortChanged(BookmarkSortOrder.SORT_LOCATION)
              showMenu = false
            }
          )

          if (canRename) {
            HorizontalDivider(color = CardBorder)
            DropdownMenuItem(
              text = {
                Text(
                  text = "Rename Collection",
                  color = Color.White
                )
              },
              onClick = {
                showMenu = false
                onRenameClick()
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ModernAyahBookmarkCard(
  ayah: ModernBookmarkRow.Ayah,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = CardBg,
    border = BorderStroke(0.5.dp, CardBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp)
    ) {
      // Header: Surah Name, Ayah Badge, Page Pill, and Delete Icon
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = ayah.suraName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )

          ModernBadgeChip(
            text = "Ayah ${ayah.ayah}",
            backgroundColor = EmeraldAccent.copy(alpha = 0.15f),
            contentColor = EmeraldAccent
          )

          ModernBadgeChip(
            text = "p. ${ayah.page}",
            backgroundColor = Color(0xFF27272A),
            contentColor = TextMuted
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = QuranIcons.Delete,
            contentDescription = "Delete",
            tint = Color(0xFF71717A),
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Ayah Text Preview (if present)
      if (!ayah.ayahText.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = ayah.ayahText,
          fontSize = 15.sp,
          color = Color(0xFFE4E4E7),
          style = MaterialTheme.typography.bodyMedium.copy(
            textDirection = TextDirection.Rtl
          ),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          lineHeight = 22.sp
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Metadata & Relative Timestamp
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = ayah.metadata,
          fontSize = 12.sp,
          color = TextMuted
        )

        val relativeTime = DateUtils.getRelativeTimeSpanString(
          ayah.timestamp,
          System.currentTimeMillis(),
          DateUtils.MINUTE_IN_MILLIS
        ).toString()

        Text(
          text = relativeTime,
          fontSize = 11.sp,
          color = Color(0xFF71717A)
        )
      }
    }
  }
}

@Composable
private fun ModernPageBookmarkCard(
  page: ModernBookmarkRow.Page,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = CardBg,
    border = BorderStroke(0.5.dp, CardBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(EmeraldAccent.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = QuranIcons.MenuBook,
            contentDescription = null,
            tint = EmeraldAccent,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = page.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = page.metadata,
            fontSize = 12.sp,
            color = TextMuted
          )
        }
      }

      IconButton(
        onClick = onDelete,
        modifier = Modifier.size(28.dp)
      ) {
        Icon(
          imageVector = QuranIcons.Delete,
          contentDescription = "Delete",
          tint = Color(0xFF71717A),
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
