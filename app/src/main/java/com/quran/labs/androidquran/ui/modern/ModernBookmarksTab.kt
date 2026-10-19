package com.quran.labs.androidquran.ui.modern

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.quran.labs.androidquran.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.model.bookmark.ReadingBookmarkType
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.ReadingBookmarkSlots
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip
import com.quran.labs.androidquran.common.ui.core.components.ModernCard
import com.quran.labs.androidquran.common.ui.core.components.SurahNumberBadge

data class BookmarkItemModel(
  val id: String,
  val sura: Int?,
  val ayah: Int?,
  val page: Int,
  val suraName: String,
  val arabicSuraName: String,
  val ayahText: String?,
  val timestamp: Long,
  val isPageBookmark: Boolean,
  val tags: List<TagItemModel> = emptyList()
)

data class TagItemModel(
  val id: String,
  val name: String,
  val isDefault: Boolean = false
)

data class ReadingBookmarkItemModel(
  val slot: ReadingBookmarkType,
  val page: Int,
  val sura: Int?,
  val ayah: Int?,
  val suraName: String,
  val name: String?,
  val timestamp: Long
)

private enum class BookmarkFilterType {
  ALL, AYAHS, PAGES, TAG
}

enum class BookmarkSortType {
  NEWEST, OLDEST, PAGE
}

@Composable
fun BookmarksTabContent(
  bookmarks: List<BookmarkItemModel>,
  readingBookmarks: List<ReadingBookmarkItemModel>,
  tags: List<TagItemModel>,
  onOpenBookmark: (BookmarkItemModel) -> Unit,
  onOpenReadingBookmark: (ReadingBookmarkItemModel) -> Unit,
  onDeleteBookmark: (BookmarkItemModel) -> Unit,
  onClearReadingBookmark: (ReadingBookmarkType) -> Unit,
  onUpdateBookmarkTags: (bookmarkId: String, selectedTagIds: Set<String>) -> Unit = { _, _ -> },
  onCreateTag: (tagName: String) -> Unit = {},
  onDeleteTag: (tagId: String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedFilter by remember { mutableStateOf(BookmarkFilterType.ALL) }
  var selectedTagId by remember { mutableStateOf<String?>(null) }
  var sortType by remember { mutableStateOf(BookmarkSortType.NEWEST) }

  // Tagging Bottom Sheet state
  var taggingBookmark by remember { mutableStateOf<BookmarkItemModel?>(null) }
  var isManageTagsOpen by remember { mutableStateOf(false) }
  var activeTagSheetIds by remember { mutableStateOf<Set<String>>(emptySet()) }

  val filteredBookmarks = remember(bookmarks, selectedFilter, selectedTagId, sortType) {
    val list = when (selectedFilter) {
      BookmarkFilterType.ALL -> bookmarks
      BookmarkFilterType.AYAHS -> bookmarks.filter { !it.isPageBookmark }
      BookmarkFilterType.PAGES -> bookmarks.filter { it.isPageBookmark }
      BookmarkFilterType.TAG -> {
        val tagId = selectedTagId
        if (tagId == null) bookmarks else bookmarks.filter { b -> b.tags.any { it.id == tagId } }
      }
    }
    when (sortType) {
      BookmarkSortType.NEWEST -> list.sortedByDescending { it.timestamp }
      BookmarkSortType.OLDEST -> list.sortedBy { it.timestamp }
      BookmarkSortType.PAGE -> list.sortedWith(compareBy({ it.page }, { it.ayah ?: 0 }))
    }
  }

  val ayahCount = remember(bookmarks) { bookmarks.count { !it.isPageBookmark } }
  val pageCount = remember(bookmarks) { bookmarks.count { it.isPageBookmark } }

  Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // 1. Pinned Reading Bookmarks Ribbons (Teal, Orange, Red)
      item {
        ReadingBookmarksSection(
          readingBookmarks = readingBookmarks,
          onOpen = onOpenReadingBookmark,
          onClear = onClearReadingBookmark
        )
      }

      // 2. Filter Pills Row
      item {
        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            FilterPill(
              title = stringResource(R.string.bookmarks_filter_all, bookmarks.size),
              isSelected = selectedFilter == BookmarkFilterType.ALL,
              onClick = {
                selectedFilter = BookmarkFilterType.ALL
                selectedTagId = null
              }
            )
          }
          item {
            FilterPill(
              title = stringResource(R.string.bookmarks_filter_ayahs, ayahCount),
              isSelected = selectedFilter == BookmarkFilterType.AYAHS,
              onClick = {
                selectedFilter = BookmarkFilterType.AYAHS
                selectedTagId = null
              }
            )
          }
          item {
            FilterPill(
              title = stringResource(R.string.bookmarks_filter_pages, pageCount),
              isSelected = selectedFilter == BookmarkFilterType.PAGES,
              onClick = {
                selectedFilter = BookmarkFilterType.PAGES
                selectedTagId = null
              }
            )
          }
          items(tags, key = { it.id }) { tag ->
            val count = bookmarks.count { b -> b.tags.any { it.id == tag.id } }
            val tagColor = getModernTagColor(tag.id)
            FilterPillWithDot(
              title = stringResource(R.string.bookmarks_tag_format, tag.name, count),
              dotColor = tagColor,
              isSelected = selectedFilter == BookmarkFilterType.TAG && selectedTagId == tag.id,
              onClick = {
                selectedFilter = BookmarkFilterType.TAG
                selectedTagId = tag.id
              }
            )
          }
          item {
            // "+ Tag" quick action
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .border(
                  0.5.dp,
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                  RoundedCornerShape(20.dp)
                )
                .clickable { isManageTagsOpen = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = QuranIcons.Add,
                  contentDescription = stringResource(R.string.bookmarks_new_tag),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.bookmarks_new_tag),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }
      }

      // 3. Sort & Summary Row (shown when bookmarks are present)
      if (bookmarks.isNotEmpty()) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (filteredBookmarks.size == 1) {
                stringResource(R.string.bookmarks_count_single, 1)
              } else {
                stringResource(R.string.bookmarks_count_plural, filteredBookmarks.size)
              },
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .clickable {
                  sortType = if (sortType == BookmarkSortType.NEWEST) {
                    BookmarkSortType.PAGE
                  } else {
                    BookmarkSortType.NEWEST
                  }
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (sortType == BookmarkSortType.NEWEST) {
                  stringResource(R.string.bookmarks_sort_recent)
                } else {
                  stringResource(R.string.bookmarks_sort_page)
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      // 4. Bookmarks List or Empty State
      if (filteredBookmarks.isEmpty()) {
        item {
          EmptyBookmarksState(
            isFilterActive = selectedFilter != BookmarkFilterType.ALL,
            onResetFilter = {
              selectedFilter = BookmarkFilterType.ALL
              selectedTagId = null
            }
          )
        }
      } else {
        items(
          items = filteredBookmarks,
          key = { it.id }
        ) { bookmark ->
          BookmarkCard(
            bookmark = bookmark,
            onClick = { onOpenBookmark(bookmark) },
            onDelete = { onDeleteBookmark(bookmark) },
            onEditTags = {
              taggingBookmark = bookmark
              activeTagSheetIds = bookmark.tags.map { it.id }.toSet()
            },
            onTagClick = { tagId ->
              selectedFilter = BookmarkFilterType.TAG
              selectedTagId = tagId
            }
          )
        }
      }
    }

    // Modern Tag Bottom Sheet for an individual bookmark
    if (taggingBookmark != null) {
      val currentBookmark = taggingBookmark
      if (currentBookmark != null) {
        val targetLabel = if (!currentBookmark.isPageBookmark && currentBookmark.sura != null && currentBookmark.ayah != null) {
          stringResource(R.string.bookmarks_ayah_format, currentBookmark.suraName, currentBookmark.ayah)
        } else {
          stringResource(R.string.bookmarks_page_format, currentBookmark.page)
        }

        ModernTagSheet(
          visible = true,
          targetTitle = targetLabel,
          tags = tags,
          selectedTagIds = activeTagSheetIds,
          onToggleTag = { tagId ->
            activeTagSheetIds = if (activeTagSheetIds.contains(tagId)) {
              activeTagSheetIds - tagId
            } else {
              activeTagSheetIds + tagId
            }
          },
          onCreateTag = onCreateTag,
          onDeleteTag = onDeleteTag,
          onSave = {
            onUpdateBookmarkTags(currentBookmark.id, activeTagSheetIds)
            taggingBookmark = null
          },
          onDismiss = { taggingBookmark = null }
        )
      }
    }

    // Modern Tag Bottom Sheet for General Tag Management
    if (isManageTagsOpen) {
      ModernTagSheet(
        visible = true,
        targetTitle = stringResource(R.string.bookmarks_manage_collections),
        tags = tags,
        selectedTagIds = emptySet(),
        onToggleTag = {},
        onCreateTag = onCreateTag,
        onDeleteTag = onDeleteTag,
        onSave = { isManageTagsOpen = false },
        onDismiss = { isManageTagsOpen = false }
      )
    }
  }
}

@Composable
private fun ReadingBookmarksSection(
  readingBookmarks: List<ReadingBookmarkItemModel>,
  onOpen: (ReadingBookmarkItemModel) -> Unit,
  onClear: (ReadingBookmarkType) -> Unit,
  modifier: Modifier = Modifier
) {
  val slots = listOf(ReadingBookmarkType.TEAL, ReadingBookmarkType.ORANGE, ReadingBookmarkType.RED)
  val bookmarksBySlot = remember(readingBookmarks) { readingBookmarks.associateBy { it.slot } }

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .background(MaterialTheme.colorScheme.primary, CircleShape)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = stringResource(R.string.bookmarks_reading_ribbons),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      slots.forEach { slot ->
        val item = bookmarksBySlot[slot]
        val spec = ReadingBookmarkSlots[slot]
        val slotColor = colorResource(spec.colorResourceId)

        Box(modifier = Modifier.weight(1f)) {
          if (item != null) {
            // Placed Reading Ribbon Card
            ModernCard(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(14.dp),
              backgroundColor = slotColor.copy(alpha = 0.12f),
              borderColor = slotColor.copy(alpha = 0.35f),
              borderWidth = 1.dp,
              onClick = { onOpen(item) },
              contentPadding = 10.dp
            ) {
              Column {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .background(slotColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = ReadingBookmarkSlots.displayName(slot, item.name),
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = slotColor,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  IconButton(
                    onClick = { onClear(slot) },
                    modifier = Modifier.size(18.dp)
                  ) {
                    Icon(
                      imageVector = QuranIcons.Close,
                      contentDescription = stringResource(R.string.bookmarks_clear_ribbon),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                  text = if (item.ayah != null) {
                    stringResource(R.string.bookmarks_ayah_format, item.suraName, item.ayah)
                  } else {
                    stringResource(R.string.bookmarks_page_format, item.page)
                  },
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                  text = stringResource(R.string.bookmarks_page_format, item.page),
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          } else {
            // Empty Ribbon Slot
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(
                  width = 0.5.dp,
                  color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                  shape = RoundedCornerShape(14.dp)
                )
                .padding(10.dp)
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(slotColor.copy(alpha = 0.35f), CircleShape)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = ReadingBookmarkSlots.displayName(slot, null),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                  )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                  text = stringResource(R.string.bookmarks_not_placed),
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = stringResource(R.string.bookmarks_pin_in_reader),
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
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
private fun FilterPill(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(20.dp)
  Box(
    modifier = Modifier
      .clip(shape)
      .background(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
      )
      .border(
        width = 0.5.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        shape = shape
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      fontSize = 12.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun FilterPillWithDot(
  title: String,
  dotColor: Color,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val shape = RoundedCornerShape(20.dp)
  Box(
    modifier = Modifier
      .clip(shape)
      .background(
        if (isSelected) dotColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
      )
      .border(
        width = 0.5.dp,
        color = if (isSelected) dotColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        shape = shape
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(7.dp)
          .clip(CircleShape)
          .background(dotColor)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) dotColor else MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun BookmarkCard(
  bookmark: BookmarkItemModel,
  onClick: () -> Unit,
  onDelete: () -> Unit,
  onEditTags: () -> Unit,
  onTagClick: (tagId: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val relativeTime = remember(bookmark.timestamp) {
    DateUtils.getRelativeTimeSpanString(
      bookmark.timestamp,
      System.currentTimeMillis(),
      DateUtils.MINUTE_IN_MILLIS
    ).toString()
  }

  ModernCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    onClick = onClick,
    contentPadding = 12.dp
  ) {
    Column {
      // Top Row: Location Pill + Arabic Surah + Actions (Tag & Delete)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (!bookmark.isPageBookmark && bookmark.sura != null && bookmark.ayah != null) {
            SurahNumberBadge(
              number = bookmark.sura,
              backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
              borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
              textColor = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = stringResource(R.string.bookmarks_ayah_format, bookmark.suraName, bookmark.ayah),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = stringResource(R.string.bookmarks_page_format, bookmark.page),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                .border(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = QuranIcons.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = stringResource(R.string.bookmarks_page_format, bookmark.page),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = bookmark.suraName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (bookmark.arabicSuraName.isNotBlank()) {
            Text(
              text = bookmark.arabicSuraName,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 6.dp)
            )
          }

          // Tag Action Button
          IconButton(
            onClick = onEditTags,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = QuranIcons.BookmarkFilled,
              contentDescription = stringResource(R.string.bookmarks_edit_tags),
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
              modifier = Modifier.size(16.dp)
            )
          }

          Spacer(modifier = Modifier.width(2.dp))

          // Delete Action Button
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = QuranIcons.Delete,
              contentDescription = stringResource(R.string.bookmarks_delete),
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      // Arabic text snippet if available (for Ayah bookmarks)
      if (!bookmark.ayahText.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = bookmark.ayahText,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge.copy(
              textDirection = TextDirection.Rtl
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // Footer: Date + Tag Chips
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = relativeTime,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        if (bookmark.tags.isNotEmpty()) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            bookmark.tags.forEach { tag ->
              val tagColor = getModernTagColor(tag.id)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(tagColor.copy(alpha = 0.14f))
                  .border(0.5.dp, tagColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                  .clickable { onTagClick(tag.id) }
                  .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(tagColor)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = tag.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tagColor
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyBookmarksState(
  isFilterActive: Boolean,
  onResetFilter: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 48.dp, horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = QuranIcons.BookmarkBorder,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(32.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = if (isFilterActive) {
        stringResource(R.string.bookmarks_empty_filtered)
      } else {
        stringResource(R.string.bookmarks_empty_saved)
      },
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = if (isFilterActive) {
        stringResource(R.string.bookmarks_empty_filtered_desc)
      } else {
        stringResource(R.string.bookmarks_empty_saved_desc)
      },
      fontSize = 13.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )

    if (isFilterActive) {
      Spacer(modifier = Modifier.height(14.dp))
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(MaterialTheme.colorScheme.primary)
          .clickable(onClick = onResetFilter)
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Text(
          text = stringResource(R.string.bookmarks_show_all),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimary
        )
      }
    }
  }
}
