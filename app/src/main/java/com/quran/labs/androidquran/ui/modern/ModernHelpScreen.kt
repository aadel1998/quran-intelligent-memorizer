package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip

private data class HelpFaqItem(
  val id: Int,
  val question: String,
  val answer: String,
  val category: String,
  val icon: ImageVector
)

/**
 * Modern Material 3 Help & Frequently Asked Questions Screen.
 * Features categorized, expandable cards with emerald accents and crisp typography.
 */
@Composable
fun ModernHelpScreen(
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  val faqItems = remember {
    listOf(
      HelpFaqItem(
        id = 1,
        question = "How do I play audio recitation?",
        answer = "Open any Quran page and tap once to reveal the floating Modern Audio Bar at the bottom. Tap the Reciter pill to choose between over 20+ authentic reciters. Tap the emerald Play button to stream or listen gaplessly. You can adjust playback speed and repetition in the Audio Settings sheet.",
        category = "Audio",
        icon = QuranIcons.PlayArrow
      ),
      HelpFaqItem(
        id = 2,
        question = "How do I view translations & tafsir?",
        answer = "While reading, tap the Translation icon in the Top HUD or long-press an ayah and select 'Translate'. You can download and prioritize 70+ translations and tafsir in the Modern Translation Manager (Settings > Translations).",
        category = "Reading",
        icon = QuranIcons.Translate
      ),
      HelpFaqItem(
        id = 3,
        question = "How do I bookmark a page or ayah?",
        answer = "To bookmark a whole page, tap the Bookmark ribbon in the Top HUD (choose between Teal, Orange, or Red ribbon). To bookmark a specific ayah, long-press the ayah and tap 'Bookmark' or assign tags (Favorites, Duas, Revision) for easy organization.",
        category = "Bookmarks",
        icon = QuranIcons.BookmarkFilled
      ),
      HelpFaqItem(
        id = 4,
        question = "How do I use Spaced-Repetition Memorization?",
        answer = "Long-press any ayah or select a surah and tap 'Memorize' to enter the Intelligent Memorizer. The app helps you memorize ayah-by-ayah through progressive audio loops and recitation testing.",
        category = "Memorizer",
        icon = QuranIcons.Sync
      ),
      HelpFaqItem(
        id = 5,
        question = "How do I adjust Night Mode & text size?",
        answer = "Go to Settings > Appearance to toggle Night Mode. You can fine-tune both Text Brightness and Background Brightness sliders in real-time. For translation text size, adjust the slider under Reading Preferences.",
        category = "Display",
        icon = QuranIcons.DarkMode
      ),
      HelpFaqItem(
        id = 6,
        question = "How do I jump quickly to any Surah or Juz?",
        answer = "Tap the Jump icon in the Top HUD or the bottom page scrubber. Select the Surah tab to pick a surah and ayah, or use the Page/Juz tab to jump straight to any part of the Quran.",
        category = "Navigation",
        icon = QuranIcons.List
      ),
      HelpFaqItem(
        id = 7,
        question = "How do I search across the Quran?",
        answer = "Tap the Search icon on the Home Hub or Reader HUD. You can search instantly by Arabic text, Surah names, or translated words across your downloaded translations.",
        category = "Search",
        icon = QuranIcons.Search
      )
    )
  }

  var expandedId by remember { mutableStateOf<Int?>(1) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(
        top = statusBarTop + 12.dp,
        bottom = navBarBottom + 24.dp
      ),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Top Navigation Bar
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
          ) {
            Icon(
              imageVector = QuranIcons.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(24.dp)
            )
          }

          Text(
            text = stringResource(R.string.menu_help),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          ModernBadgeChip(
            text = "FAQ & Guides",
            backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            contentColor = MaterialTheme.colorScheme.primary
          )
        }
      }

      // Introductory Subtitle
      item {
        Text(
          text = stringResource(R.string.help_title),
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
      }

      // FAQ List items
      items(faqItems, key = { it.id }) { item ->
        val isExpanded = expandedId == item.id

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
              expandedId = if (isExpanded) null else item.id
            },
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = item.question,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  Spacer(modifier = Modifier.height(2.dp))

                  ModernBadgeChip(
                    text = item.category,
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Icon(
                imageVector = QuranIcons.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                  .size(22.dp)
                  .rotate(if (isExpanded) 180f else 0f)
              )
            }

            AnimatedVisibility(
              visible = isExpanded,
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp)
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                  text = item.answer,
                  fontSize = 13.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  lineHeight = 20.sp
                )
              }
            }
          }
        }
      }
    }
  }
}
