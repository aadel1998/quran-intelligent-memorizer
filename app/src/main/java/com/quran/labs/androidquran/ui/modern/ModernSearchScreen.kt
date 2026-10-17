package com.quran.labs.androidquran.ui.modern

import android.graphics.Typeface
import android.text.Html
import android.text.style.StyleSpan
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.QuranIcons

data class ModernSearchResultItem(
  val id: Int,
  val sura: Int,
  val ayah: Int,
  val page: Int,
  val juz: Int,
  val suraName: String,
  val suraArabicName: String,
  val text: String,
  val isArabic: Boolean
)

data class PopularSearchTopic(
  val label: String,
  val query: String,
  val subtitle: String
)

private val popularSearchTopics = listOf(
  PopularSearchTopic("الرحمن", "الرحمن", "The Most Merciful"),
  PopularSearchTopic("الصبر", "الصبر", "Patience & Perseverance"),
  PopularSearchTopic("الجنة", "الجنة", "Paradise & Eternal Bliss"),
  PopularSearchTopic("النور", "النور", "Divine Light"),
  PopularSearchTopic("موسى", "موسى", "Prophet Musa"),
  PopularSearchTopic("التوبة", "التوبة", "Repentance & Forgiveness"),
  PopularSearchTopic("الشكر", "الشكر", "Gratitude"),
  PopularSearchTopic("التقوى", "التقوى", "Mindfulness of Allah"),
  PopularSearchTopic("الهدى", "الهدى", "Guidance")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModernSearchScreen(
  query: String,
  onQueryChange: (String) -> Unit,
  onSearch: (String) -> Unit,
  searchResults: List<ModernSearchResultItem>,
  isLoading: Boolean,
  showArabicWarning: Boolean,
  showNeedTranslationsWarning: Boolean,
  onDownloadArabicDb: () -> Unit,
  onOpenTranslations: () -> Unit,
  onJumpToResult: (sura: Int, ayah: Int, translation: Boolean) -> Unit,
  onShareAyah: ((ModernSearchResultItem) -> Unit)? = null,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier
) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0 = All, 1 = Arabic, 2 = Translation

  val filteredResults = remember(searchResults, selectedFilterIndex) {
    when (selectedFilterIndex) {
      1 -> searchResults.filter { it.isArabic }
      2 -> searchResults.filter { !it.isArabic }
      else -> searchResults
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = statusBarPadding)
        .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
      // Top Search Bar
      ModernSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {
          keyboardController?.hide()
          focusManager.clearFocus()
          onSearch(it)
        },
        onClear = {
          onQueryChange("")
        },
        onNavigateUp = onNavigateUp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )

      // Warning Banners
      if (showArabicWarning) {
        ModernWarningCard(
          title = "Arabic Search Database Missing",
          description = "Download the Arabic search database to search offline across Arabic verses.",
          buttonText = "Download Database",
          onClick = onDownloadArabicDb,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }

      if (showNeedTranslationsWarning) {
        ModernWarningCard(
          title = "No Translations Found",
          description = "Download translations to search meanings in English and other languages.",
          buttonText = "Get Translations",
          onClick = onOpenTranslations,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
      }

      // Filter Chips (when results exist)
      if (searchResults.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          ModernFilterChip(
            label = "All (${searchResults.size})",
            selected = selectedFilterIndex == 0,
            onClick = { selectedFilterIndex = 0 }
          )
          val arabicCount = searchResults.count { it.isArabic }
          if (arabicCount > 0) {
            ModernFilterChip(
              label = "Arabic ($arabicCount)",
              selected = selectedFilterIndex == 1,
              onClick = { selectedFilterIndex = 1 }
            )
          }
          val transCount = searchResults.count { !it.isArabic }
          if (transCount > 0) {
            ModernFilterChip(
              label = "Translation ($transCount)",
              selected = selectedFilterIndex == 2,
              onClick = { selectedFilterIndex = 2 }
            )
          }
        }
      }

      // Content Area
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        when {
          isLoading -> {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
              )
            }
          }

          query.isBlank() -> {
            // Idle State: Popular search keywords
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(16.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              item {
                Text(
                  text = "Popular Quranic Searches",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              item {
                FlowRow(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  popularSearchTopics.forEach { topic ->
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161F22))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                        .clickable {
                          onQueryChange(topic.query)
                          onSearch(topic.query)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                      Column {
                        Text(
                          text = topic.label,
                          fontSize = 16.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                          text = topic.subtitle,
                          fontSize = 12.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          filteredResults.isEmpty() -> {
            // Empty State
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFF161F22), CircleShape)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = QuranIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                  )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                  text = "No results found for \"$query\"",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onBackground,
                  textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = "Try searching with Arabic roots, English keywords, or ensure translation databases are downloaded.",
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          else -> {
            // Results List
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(filteredResults, key = { it.id }) { item ->
                ModernSearchResultCard(
                  result = item,
                  query = query,
                  onJumpToMushaf = { onJumpToResult(item.sura, item.ayah, false) },
                  onJumpToTranslation = { onJumpToResult(item.sura, item.ayah, true) },
                  onShare = onShareAyah?.let { { it(item) } }
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
private fun ModernSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  onSearch: (String) -> Unit,
  onClear: () -> Unit,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    IconButton(
      onClick = onNavigateUp,
      modifier = Modifier
        .size(42.dp)
        .background(Color(0xFF161F22), CircleShape)
    ) {
      Icon(
        imageVector = QuranIcons.ArrowBack,
        contentDescription = "Back",
        tint = MaterialTheme.colorScheme.onSurface
      )
    }

    OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      modifier = Modifier
        .weight(1f)
        .height(52.dp),
      placeholder = {
        Text(
          text = "Search Quran...",
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      leadingIcon = {
        Icon(
          imageVector = QuranIcons.Search,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp)
        )
      },
      trailingIcon = {
        if (query.isNotEmpty()) {
          IconButton(onClick = onClear) {
            Icon(
              imageVector = QuranIcons.Close,
              contentDescription = "Clear",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = Color(0x33FFFFFF),
        focusedContainerColor = Color(0xFF161F22),
        unfocusedContainerColor = Color(0xFF161F22),
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
      ),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
    )
  }
}

@Composable
private fun ModernSearchResultCard(
  result: ModernSearchResultItem,
  query: String,
  onJumpToMushaf: () -> Unit,
  onJumpToTranslation: () -> Unit,
  onShare: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val clipboardManager = LocalClipboardManager.current
  val primaryColor = MaterialTheme.colorScheme.primary
  val formattedText = remember(result.text, query) {
    formatSearchMatchText(result.text, query, primaryColor)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFF161F22))
      .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
      .padding(14.dp)
  ) {
    // Header Row: Surah badge, name, location, Arabic surah name
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${result.sura}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Column {
          Text(
            text = result.suraName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Ayah ${result.ayah} • Page ${result.page} • Juz ${result.juz}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      if (result.suraArabicName.isNotBlank()) {
        Text(
          text = result.suraArabicName,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          fontFamily = FontFamily.Default
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Verse Content
    Text(
      text = formattedText,
      fontSize = if (result.isArabic) 20.sp else 14.sp,
      lineHeight = if (result.isArabic) 32.sp else 22.sp,
      textAlign = if (result.isArabic) TextAlign.Right else TextAlign.Left,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Actions Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
          onClick = onJumpToMushaf,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color(0xFF00382B)
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(
            imageVector = QuranIcons.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(15.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Mushaf",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        OutlinedButton(
          onClick = onJumpToTranslation,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0x33FFFFFF))
          ),
          modifier = Modifier.height(34.dp)
        ) {
          Icon(
            imageVector = QuranIcons.Translate,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Translation",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
          onClick = {
            clipboardManager.setText(
              AnnotatedString("${Html.fromHtml(result.text, Html.FROM_HTML_MODE_LEGACY)} (${result.suraName} ${result.sura}:${result.ayah})")
            )
          },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = QuranIcons.ContentCopy,
            contentDescription = "Copy",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }

        if (onShare != null) {
          IconButton(
            onClick = onShare,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = QuranIcons.Share,
              contentDescription = "Share",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ModernFilterChip(
  label: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(
        if (selected) MaterialTheme.colorScheme.primaryContainer else Color(0xFF161F22)
      )
      .border(
        1.dp,
        if (selected) MaterialTheme.colorScheme.primary else Color(0x26FFFFFF),
        RoundedCornerShape(10.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = label,
      fontSize = 12.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
      color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@Composable
private fun ModernWarningCard(
  title: String,
  description: String,
  buttonText: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0x1F00E5FF))
      .border(1.dp, Color(0x4D00E5FF), RoundedCornerShape(14.dp))
      .padding(12.dp)
  ) {
    Text(
      text = title,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = description,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
      onClick = onClick,
      shape = RoundedCornerShape(8.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color(0xFF00382B)
      ),
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
      modifier = Modifier.height(30.dp)
    ) {
      Text(
        text = buttonText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

fun formatSearchMatchText(
  rawOrHtml: String,
  query: String,
  highlightColor: Color
): AnnotatedString {
  val spanned = Html.fromHtml(rawOrHtml, Html.FROM_HTML_MODE_LEGACY)
  val rawText = spanned.toString()
  val spans = spanned.getSpans(0, spanned.length, Any::class.java)
  val boldRanges = mutableListOf<IntRange>()

  for (span in spans) {
    if (span is StyleSpan && (span.style and Typeface.BOLD != 0)) {
      val start = spanned.getSpanStart(span)
      val end = spanned.getSpanEnd(span)
      if (start in 0..rawText.length && end in 0..rawText.length && start < end) {
        boldRanges.add(start until end)
      }
    }
  }

  return buildAnnotatedString {
    append(rawText)
    if (boldRanges.isNotEmpty()) {
      for (range in boldRanges) {
        addStyle(
          SpanStyle(
            color = highlightColor,
            fontWeight = FontWeight.Bold,
            background = highlightColor.copy(alpha = 0.22f)
          ),
          range.first,
          range.last + 1
        )
      }
    } else if (query.isNotBlank() && query.length >= 2) {
      var index = rawText.indexOf(query, ignoreCase = true)
      while (index >= 0) {
        addStyle(
          SpanStyle(
            color = highlightColor,
            fontWeight = FontWeight.Bold,
            background = highlightColor.copy(alpha = 0.22f)
          ),
          index,
          index + query.length
        )
        index = rawText.indexOf(query, index + query.length, ignoreCase = true)
      }
    }
  }
}
