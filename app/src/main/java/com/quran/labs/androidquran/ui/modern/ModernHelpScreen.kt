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
  val questionRes: Int,
  val answerRes: Int,
  val categoryRes: Int,
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
        questionRes = R.string.help_faq_audio_q,
        answerRes = R.string.help_faq_audio_a,
        categoryRes = R.string.help_cat_audio,
        icon = QuranIcons.PlayArrow
      ),
      HelpFaqItem(
        id = 2,
        questionRes = R.string.help_faq_reading_q,
        answerRes = R.string.help_faq_reading_a,
        categoryRes = R.string.help_cat_reading,
        icon = QuranIcons.Translate
      ),
      HelpFaqItem(
        id = 3,
        questionRes = R.string.help_faq_bookmark_q,
        answerRes = R.string.help_faq_bookmark_a,
        categoryRes = R.string.help_cat_bookmarks,
        icon = QuranIcons.BookmarkFilled
      ),
      HelpFaqItem(
        id = 4,
        questionRes = R.string.help_faq_memorizer_q,
        answerRes = R.string.help_faq_memorizer_a,
        categoryRes = R.string.help_cat_memorizer,
        icon = QuranIcons.Sync
      ),
      HelpFaqItem(
        id = 5,
        questionRes = R.string.help_faq_display_q,
        answerRes = R.string.help_faq_display_a,
        categoryRes = R.string.help_cat_display,
        icon = QuranIcons.DarkMode
      ),
      HelpFaqItem(
        id = 6,
        questionRes = R.string.help_faq_nav_q,
        answerRes = R.string.help_faq_nav_a,
        categoryRes = R.string.help_cat_nav,
        icon = QuranIcons.List
      ),
      HelpFaqItem(
        id = 7,
        questionRes = R.string.help_faq_search_q,
        answerRes = R.string.help_faq_search_a,
        categoryRes = R.string.help_cat_search,
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
              contentDescription = stringResource(R.string.reader_back),
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
            text = stringResource(R.string.help_badge_faq),
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
                    text = stringResource(item.questionRes),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  Spacer(modifier = Modifier.height(2.dp))

                  ModernBadgeChip(
                    text = stringResource(item.categoryRes),
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Icon(
                imageVector = QuranIcons.KeyboardArrowDown,
                contentDescription = null,
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
                  text = stringResource(item.answerRes),
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
