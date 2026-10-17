package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip

/**
 * Modern Reader Top Bar HUD with glassmorphic surface, Surah titles (Arabic & English),
 * Juz/Hizb badges, and quick actions.
 */
@Composable
fun ModernReaderTopBar(
  suraName: String,
  arabicSuraName: String,
  pageNumber: Int,
  juzNumber: Int,
  hizbNumber: Int,
  rubNumber: Int,
  isReadingBookmarked: Boolean,
  isNightMode: Boolean,
  isTranslationMode: Boolean,
  activeTranslationName: String = "",
  onBackClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onTranslationClick: () -> Unit,
  onTranslationOptionsClick: () -> Unit = {},
  onNightModeToggle: () -> Unit,
  onJumpClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val layoutDirection = LocalLayoutDirection.current
  val systemBars = WindowInsets.systemBars.asPaddingValues()
  val statusBarTop = systemBars.calculateTopPadding()
  val horizontalInsetLeft = systemBars.calculateStartPadding(layoutDirection)
  val horizontalInsetRight = systemBars.calculateEndPadding(layoutDirection)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color.Black.copy(alpha = 0.85f),
            Color.Black.copy(alpha = 0.50f),
            Color.Transparent
          )
        )
      )
  ) {
    Spacer(modifier = Modifier.height(statusBarTop))

    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          start = 12.dp + horizontalInsetLeft,
          end = 12.dp + horizontalInsetRight,
          top = 6.dp,
          bottom = 6.dp
        )
        .shadow(elevation = 12.dp, shape = RoundedCornerShape(22.dp)),
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
      )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Back navigation button
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = QuranIcons.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
          )
        }

        // Center Surah metadata column
        Column(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = suraName,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            if (arabicSuraName.isNotEmpty()) {
              Text(
                text = arabicSuraName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
              )
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            ModernBadgeChip(
              text = "p. $pageNumber",
              backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
              contentColor = MaterialTheme.colorScheme.primary
            )

            ModernBadgeChip(
              text = "Juz $juzNumber",
              backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (hizbNumber > 0) {
              val rubSymbol = when (rubNumber) {
                2 -> "½"
                3 -> "¾"
                4 -> "4/4"
                else -> "¼"
              }
              ModernBadgeChip(
                text = "Hizb $hizbNumber • $rubSymbol",
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Active translation selector pill when in translation mode
          if (isTranslationMode && activeTranslationName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onTranslationOptionsClick() },
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
              border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
              )
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = QuranIcons.Translate,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(11.dp)
                )
                Text(
                  text = activeTranslationName,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Icon(
                  imageVector = QuranIcons.KeyboardArrowDown,
                  contentDescription = "Switch translation",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(13.dp)
                )
              }
            }
          }
        }

        // Right action buttons
        Row(
          horizontalArrangement = Arrangement.spacedBy(0.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Bookmark Ribbon button
          IconButton(
            onClick = onBookmarkClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = if (isReadingBookmarked) QuranIcons.BookmarkFilled else QuranIcons.BookmarkBorder,
              contentDescription = if (isReadingBookmarked) "Bookmarked" else "Bookmark",
              tint = if (isReadingBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          // Translation toggle
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(if (isTranslationMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent),
            contentAlignment = Alignment.Center
          ) {
            IconButton(
              onClick = onTranslationClick,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = QuranIcons.Translate,
                contentDescription = "Translation",
                tint = if (isTranslationMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          // Night Mode toggle
          IconButton(
            onClick = onNightModeToggle,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = QuranIcons.DarkMode,
              contentDescription = "Night Mode",
              tint = if (isNightMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          // Quick Jump
          IconButton(
            onClick = onJumpClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = QuranIcons.List,
              contentDescription = "Jump",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          // Settings
          IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = QuranIcons.Settings,
              contentDescription = "Settings",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Modern Page Scrubber HUD floating at the bottom with smooth slider,
 * page jumping, previous/next page steppers, and live page preview.
 */
@Composable
fun ModernPageScrubber(
  currentPage: Int,
  totalPages: Int,
  currentSuraName: String,
  onPageChanged: (Int) -> Unit,
  onJumpClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  var sliderPosition by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }
  var isDragging by remember { mutableStateOf(false) }
  val layoutDirection = LocalLayoutDirection.current
  val systemBars = WindowInsets.systemBars.asPaddingValues()
  val navBarBottom = systemBars.calculateBottomPadding()
  val horizontalInsetLeft = systemBars.calculateStartPadding(layoutDirection)
  val horizontalInsetRight = systemBars.calculateEndPadding(layoutDirection)

  val displayPage = if (isDragging) sliderPosition.toInt().coerceIn(1, totalPages) else currentPage

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(
        start = 16.dp + horizontalInsetLeft,
        end = 16.dp + horizontalInsetRight,
        top = 8.dp,
        bottom = 8.dp + navBarBottom
      )
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(elevation = 14.dp, shape = RoundedCornerShape(24.dp)),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
      )
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Prev Page button
        IconButton(
          onClick = {
            if (currentPage > 1) {
              onPageChanged(currentPage - 1)
            }
          },
          enabled = currentPage > 1,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = QuranIcons.ArrowBack,
            contentDescription = "Previous Page",
            tint = if (currentPage > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
          )
        }

        // Center slider with page indicator badge
        Column(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "p. $displayPage",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )

            Text(
              text = "of $totalPages",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (currentSuraName.isNotEmpty()) {
              Text(
                text = "• $currentSuraName",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
              isDragging = true
              sliderPosition = newValue
            },
            onValueChangeFinished = {
              isDragging = false
              val targetPage = sliderPosition.toInt().coerceIn(1, totalPages)
              onPageChanged(targetPage)
            },
            valueRange = 1f..totalPages.toFloat(),
            colors = SliderDefaults.colors(
              thumbColor = MaterialTheme.colorScheme.primary,
              activeTrackColor = MaterialTheme.colorScheme.primary,
              inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(26.dp)
          )
        }

        // Next Page button
        IconButton(
          onClick = {
            if (currentPage < totalPages) {
              onPageChanged(currentPage + 1)
            }
          },
          enabled = currentPage < totalPages,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = QuranIcons.ChevronRight,
            contentDescription = "Next Page",
            tint = if (currentPage < totalPages) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
          )
        }

        // Quick Jump Pill
        IconButton(
          onClick = onJumpClick,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = QuranIcons.Search,
            contentDescription = "Jump to page",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
