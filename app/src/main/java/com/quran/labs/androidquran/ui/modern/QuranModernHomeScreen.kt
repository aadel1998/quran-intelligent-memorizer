package com.quran.labs.androidquran.ui.modern

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.model.bookmark.ReadingBookmarkType
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.audio.memorization.SavedSuraSession
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.ReadingBookmarkSlots
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip
import com.quran.labs.androidquran.common.ui.core.components.ModernCard
import com.quran.labs.androidquran.common.ui.core.components.ModernTabItem
import com.quran.labs.androidquran.common.ui.core.components.ModernTabRow
import com.quran.labs.androidquran.common.ui.core.components.SurahNumberBadge

data class SuraItemModel(
  val number: Int,
  val englishName: String,
  val arabicName: String,
  val isMakki: Boolean,
  val numberOfAyahs: Int,
  val startPage: Int,
  val juzNumber: Int,
  val savedSession: SavedSuraSession? = null,
  val readingRibbons: List<ReadingBookmarkType> = emptyList()
)

data class QuarterItemModel(
  val index: Int,
  val hizb: Int,
  val rub: Int,
  val sura: Int,
  val ayah: Int,
  val suraName: String,
  val page: Int
)

data class JuzItemModel(
  val number: Int,
  val startingPage: Int,
  val startingSuraNumber: Int,
  val startingSuraName: String,
  val startingArabicName: String,
  val quarters: List<QuarterItemModel> = emptyList()
)

data class LastReadModel(
  val page: Int,
  val suraNumber: Int,
  val suraName: String,
  val arabicName: String,
  val juzNumber: Int
)

@Composable
fun QuranModernHomeScreen(
  surahs: List<SuraItemModel>,
  juzs: List<JuzItemModel>,
  bookmarks: List<BookmarkItemModel>,
  readingBookmarks: List<ReadingBookmarkItemModel>,
  tags: List<TagItemModel>,
  savedSessions: List<SavedSuraSession>,
  lastRead: LastReadModel?,
  onOpenSurah: (sura: Int, page: Int) -> Unit,
  onOpenJuz: (juz: Int, page: Int) -> Unit,
  onOpenQuarter: (page: Int) -> Unit,
  onOpenBookmark: (BookmarkItemModel) -> Unit,
  onOpenReadingBookmark: (ReadingBookmarkItemModel) -> Unit,
  onDeleteBookmark: (BookmarkItemModel) -> Unit,
  onClearReadingBookmark: (ReadingBookmarkType) -> Unit,
  onOpenLastRead: (page: Int) -> Unit,
  onResumeMemorization: (SavedSuraSession) -> Unit,
  onStartFromBeginningMemorization: (SavedSuraSession) -> Unit,
  onOpenSearch: (String) -> Unit,
  onOpenJump: () -> Unit,
  onOpenPlaylists: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var isSearchExpanded by remember { mutableStateOf(false) }

  val totalBookmarksCount = bookmarks.size + readingBookmarks.size
  val tabs = listOf(
    ModernTabItem(title = "Surahs", badgeCount = null),
    ModernTabItem(title = "Juz", badgeCount = null),
    ModernTabItem(title = "Bookmarks", badgeCount = if (totalBookmarksCount > 0) totalBookmarksCount else null),
    ModernTabItem(title = "Memorizer", badgeCount = if (savedSessions.isNotEmpty()) savedSessions.size else null)
  )

  val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background
  ) { contentPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = statusBarPadding)
        .padding(bottom = contentPadding.calculateBottomPadding())
    ) {
      // Modern Top App Bar
      ModernHomeTopBar(
        isSearchExpanded = isSearchExpanded,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        onToggleSearch = {
          isSearchExpanded = !isSearchExpanded
          if (!isSearchExpanded) searchQuery = ""
        },
        onOpenSearch = onOpenSearch,
        onOpenJump = onOpenJump,
        onOpenPlaylists = onOpenPlaylists,
        onOpenSettings = onOpenSettings
      )

      // Continue Reading Hero Card (Shown when not searching and last read exists)
      AnimatedVisibility(
        visible = !isSearchExpanded && lastRead != null,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        if (lastRead != null) {
          ContinueReadingHeroCard(
            lastRead = lastRead,
            onClick = { onOpenLastRead(lastRead.page) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
          )
        }
      }

      // Modern Tab Bar
      ModernTabRow(
        tabs = tabs,
        selectedTabIndex = selectedTab,
        onTabSelected = { selectedTab = it },
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
      )

      // Tab Content
      Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
          0 -> SurahsTabContent(
            surahs = surahs,
            searchQuery = searchQuery,
            onSurahClick = onOpenSurah,
            onResumeMemorization = onResumeMemorization
          )
          1 -> JuzsTabContent(
            juzs = juzs,
            searchQuery = searchQuery,
            onJuzClick = onOpenJuz,
            onQuarterClick = onOpenQuarter
          )
          2 -> BookmarksTabContent(
            bookmarks = bookmarks,
            readingBookmarks = readingBookmarks,
            tags = tags,
            onOpenBookmark = onOpenBookmark,
            onOpenReadingBookmark = onOpenReadingBookmark,
            onDeleteBookmark = onDeleteBookmark,
            onClearReadingBookmark = onClearReadingBookmark
          )
          3 -> MemorizerTabContent(
            savedSessions = savedSessions,
            surahs = surahs,
            onResume = onResumeMemorization,
            onStartBeginning = onStartFromBeginningMemorization,
            onOpenSurah = onOpenSurah
          )
        }
      }
    }
  }
}

@Composable
private fun ModernHomeTopBar(
  isSearchExpanded: Boolean,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onToggleSearch: () -> Unit,
  onOpenSearch: (String) -> Unit,
  onOpenJump: () -> Unit,
  onOpenPlaylists: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = QuranIcons.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = stringResource(R.string.app_name),
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleSearch) {
          Icon(
            imageVector = if (isSearchExpanded) QuranIcons.Close else QuranIcons.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onOpenJump) {
          Icon(
            imageVector = QuranIcons.FastForward,
            contentDescription = "Jump",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onOpenPlaylists) {
          Icon(
            imageVector = QuranIcons.List,
            contentDescription = "Playlists",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        IconButton(onClick = onOpenSettings) {
          Icon(
            imageVector = QuranIcons.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    AnimatedVisibility(visible = isSearchExpanded) {
      Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Filter Surahs by name or number...") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
          ),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(onSearch = {
            keyboardController?.hide()
            focusManager.clearFocus()
            if (searchQuery.isNotBlank()) {
              onOpenSearch(searchQuery)
            }
          }),
          trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(
                    imageVector = QuranIcons.Close,
                    contentDescription = "Clear",
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
              IconButton(onClick = { onOpenSearch(searchQuery) }) {
                Icon(
                  imageVector = QuranIcons.Search,
                  contentDescription = "Search whole Quran",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
          },
          modifier = Modifier.fillMaxWidth()
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onOpenSearch(searchQuery) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = QuranIcons.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "Search whole Quran for \"$searchQuery\"" else "Deep search across entire Quran",
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Icon(
            imageVector = QuranIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun ContinueReadingHeroCard(
  lastRead: LastReadModel,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  ModernCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
    borderWidth = 1.dp,
    onClick = onClick,
    contentPadding = 16.dp
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .background(MaterialTheme.colorScheme.primary, CircleShape)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "CONTINUE READING",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = lastRead.suraName,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ModernBadgeChip(
            text = "Page ${lastRead.page}",
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onSurface
          )
          ModernBadgeChip(
            text = "Juz ${lastRead.juzNumber}",
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = lastRead.arabicName,
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = QuranIcons.PlayArrow,
            contentDescription = "Read",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SurahsTabContent(
  surahs: List<SuraItemModel>,
  searchQuery: String,
  onSurahClick: (sura: Int, page: Int) -> Unit,
  onResumeMemorization: (SavedSuraSession) -> Unit,
  modifier: Modifier = Modifier
) {
  val filteredSurahs = remember(surahs, searchQuery) {
    if (searchQuery.isBlank()) {
      surahs
    } else {
      val query = searchQuery.trim().lowercase()
      surahs.filter {
        it.number.toString() == query ||
        it.englishName.lowercase().contains(query) ||
        it.arabicName.contains(query)
      }
    }
  }

  val listState = rememberLazyListState()

  LazyColumn(
    state = listState,
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(
      items = filteredSurahs,
      key = { it.number }
    ) { sura ->
      SurahCard(
        sura = sura,
        onClick = { onSurahClick(sura.number, sura.startPage) },
        onResumeMemorization = onResumeMemorization
      )
    }
  }
}

@Composable
private fun SurahCard(
  sura: SuraItemModel,
  onClick: () -> Unit,
  onResumeMemorization: (SavedSuraSession) -> Unit,
  modifier: Modifier = Modifier
) {
  ModernCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    onClick = onClick,
    contentPadding = 12.dp
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          SurahNumberBadge(number = sura.number)

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = sura.englishName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ModernBadgeChip(
                text = if (sura.isMakki) "Makki" else "Madani",
                backgroundColor = if (sura.isMakki) {
                  MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                } else {
                  MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                },
                contentColor = if (sura.isMakki) {
                  MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                  MaterialTheme.colorScheme.onTertiaryContainer
                }
              )

              ModernBadgeChip(
                text = "${sura.numberOfAyahs} ayahs",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )

              ModernBadgeChip(
                text = "p. ${sura.startPage}",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )

              if (sura.readingRibbons.isNotEmpty()) {
                sura.readingRibbons.forEach { slot ->
                  val spec = ReadingBookmarkSlots[slot]
                  val color = colorResource(spec.colorResourceId)
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(color, CircleShape)
                  )
                }
              }
            }
          }
        }

        Text(
          text = sura.arabicName,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 8.dp)
        )
      }

      // If active memorization in progress, show modern progress strip
      if (sura.savedSession != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF10B981), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Memorizing • Step ${sura.savedSession.stepIndex + 1}",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Text(
            text = "Tap to resume ▶",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onResumeMemorization(sura.savedSession) }
          )
        }
      }
    }
  }
}

@Composable
private fun JuzsTabContent(
  juzs: List<JuzItemModel>,
  searchQuery: String,
  onJuzClick: (juz: Int, page: Int) -> Unit,
  onQuarterClick: (page: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val filteredJuzs = remember(juzs, searchQuery) {
    if (searchQuery.isBlank()) {
      juzs
    } else {
      val query = searchQuery.trim().lowercase()
      juzs.filter {
        it.number.toString() == query ||
        it.startingSuraName.lowercase().contains(query) ||
        it.startingArabicName.contains(query)
      }
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(
      items = filteredJuzs,
      key = { it.number }
    ) { juz ->
      JuzCard(
        juz = juz,
        onJuzClick = { onJuzClick(juz.number, juz.startingPage) },
        onQuarterClick = onQuarterClick
      )
    }
  }
}

@Composable
private fun JuzCard(
  juz: JuzItemModel,
  onJuzClick: () -> Unit,
  onQuarterClick: (page: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  ModernCard(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    contentPadding = 14.dp
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onJuzClick)
            .padding(vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          SurahNumberBadge(
            number = juz.number,
            backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
            borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
            textColor = MaterialTheme.colorScheme.secondary
          )

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = "Juz ${juz.number}",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Begins at ${juz.startingSuraName}",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            ModernBadgeChip(
              text = "Page ${juz.startingPage}",
              backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
          Text(
            text = juz.startingArabicName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )

          if (juz.quarters.isNotEmpty()) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = if (isExpanded) QuranIcons.KeyboardArrowUp else QuranIcons.KeyboardArrowDown,
              contentDescription = if (isExpanded) "Collapse" else "Expand quarters",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(0.5.dp)
              .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
          )
          Spacer(modifier = Modifier.height(8.dp))

          juz.quarters.forEach { quarter ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onQuarterClick(quarter.page) }
                .padding(horizontal = 6.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                ModernBadgeChip(
                  text = "Hizb ${quarter.hizb} • ¼ ${quarter.rub}",
                  backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "${quarter.suraName} ${quarter.sura}:${quarter.ayah}",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              ModernBadgeChip(
                text = "p. ${quarter.page}",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MemorizerTabContent(
  savedSessions: List<SavedSuraSession>,
  surahs: List<SuraItemModel>,
  onResume: (SavedSuraSession) -> Unit,
  onStartBeginning: (SavedSuraSession) -> Unit,
  onOpenSurah: (sura: Int, page: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  if (savedSessions.isEmpty()) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(64.dp)
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = QuranIcons.Repeat,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(32.dp)
        )
      }
      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "No In-Progress Memorizations",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Select any Surah from the Surahs tab and tap Memorize in the audio bar to begin spaced-repetition memorization.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  } else {
    LazyColumn(
      modifier = modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Summary Banner
      item {
        ModernCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
          borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
          borderWidth = 1.dp,
          contentPadding = 14.dp
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "SURAH MEMORIZATION TRACKER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${savedSessions.size} Active ${if (savedSessions.size == 1) "Surah" else "Surahs"} in Progress",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Box(
              modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = QuranIcons.Repeat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // Active Surah Sessions
      items(
        items = savedSessions,
        key = { it.sura }
      ) { session ->
        val suraInfo = surahs.find { it.number == session.sura }

        ModernCard(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          contentPadding = 14.dp
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                SurahNumberBadge(number = session.sura)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = suraInfo?.englishName ?: "Surah ${session.sura}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Ayahs ${session.config.startAyah.ayah}..${session.config.endAyah.ayah}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Text(
                text = suraInfo?.arabicName ?: "",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step Indicator Strip
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              ModernBadgeChip(
                text = "Step ${session.stepIndex + 1}",
                backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                leadingDotColor = Color(0xFF10B981)
              )

              ModernBadgeChip(
                text = "Window: ${session.config.windowSize} • Repeat: ${session.config.singleRepeat}x",
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Resume & From Beginning
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .weight(1.3f)
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.primary)
                  .clickable { onResume(session) }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = QuranIcons.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Resume (Step ${session.stepIndex + 1})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                  )
                }
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                  .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                  .clickable { onStartBeginning(session) }
                  .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "From Beginning",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }
  }
}
