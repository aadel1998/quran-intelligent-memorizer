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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip

/**
 * Sleek Modern Material 3 Bottom Action Sheet for Ayah selection.
 * Appears when an Ayah is long-pressed in the Quran reader.
 */
@Composable
fun ModernAyahActionSheet(
  visible: Boolean,
  suraName: String,
  arabicSuraName: String,
  startAyah: Int,
  endAyah: Int = startAyah,
  isBookmarked: Boolean = false,
  onPlayClick: () -> Unit,
  onMemorizeClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onTranslateClick: () -> Unit,
  onCopyClick: () -> Unit,
  onShareClick: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
  val bottomPadding = if (navBarBottom > 0.dp) navBarBottom + 8.dp else 16.dp

  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    modifier = modifier
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .shadow(elevation = 20.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
      )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp)
          .padding(bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Drag Handle
        Box(
          modifier = Modifier
            .width(36.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Header Row: Surah & Ayah info + close button
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
              text = suraName,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            if (arabicSuraName.isNotEmpty()) {
              Text(
                text = arabicSuraName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }

            val ayahText = if (startAyah == endAyah) {
              "Ayah $startAyah"
            } else {
              "Ayahs $startAyah - $endAyah"
            }

            ModernBadgeChip(
              text = ayahText,
              backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
              contentColor = MaterialTheme.colorScheme.primary
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(36.dp)
          ) {
            Icon(
              imageVector = QuranIcons.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ModernAyahActionItem(
            icon = QuranIcons.PlayArrow,
            label = "Play",
            onClick = onPlayClick,
            modifier = Modifier.weight(1f),
            isHighlighted = true,
            highlightColor = MaterialTheme.colorScheme.primary
          )

          ModernAyahActionItem(
            icon = QuranIcons.Repeat,
            label = "Memorize",
            onClick = onMemorizeClick,
            modifier = Modifier.weight(1f),
            isHighlighted = true,
            highlightColor = Color(0xFF10B981) // Emerald accent
          )

          ModernAyahActionItem(
            icon = if (isBookmarked) QuranIcons.BookmarkFilled else QuranIcons.BookmarkBorder,
            label = "Bookmark",
            onClick = onBookmarkClick,
            modifier = Modifier.weight(1f),
            isHighlighted = isBookmarked,
            highlightColor = Color(0xFFF59E0B) // Amber accent
          )

          ModernAyahActionItem(
            icon = QuranIcons.Translate,
            label = "Translate",
            onClick = onTranslateClick,
            modifier = Modifier.weight(1f)
          )

          ModernAyahActionItem(
            icon = QuranIcons.ContentCopy,
            label = "Copy",
            onClick = onCopyClick,
            modifier = Modifier.weight(1f)
          )

          ModernAyahActionItem(
            icon = QuranIcons.Share,
            label = "Share",
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun ModernAyahActionItem(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isHighlighted: Boolean = false,
  highlightColor: Color = MaterialTheme.colorScheme.primary
) {
  val bg = if (isHighlighted) {
    highlightColor.copy(alpha = 0.16f)
  } else {
    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
  }
  val iconTint = if (isHighlighted) highlightColor else MaterialTheme.colorScheme.onSurface
  val textColor = if (isHighlighted) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .background(bg)
      .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
      .padding(vertical = 10.dp, horizontal = 2.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      modifier = Modifier.size(22.dp),
      tint = iconTint
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
      color = textColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
