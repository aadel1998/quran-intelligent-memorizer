package com.quran.labs.androidquran.common.ui.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ModernTabItem(
  val title: String,
  val badgeCount: Int? = null
)

/**
 * Modern Segmented Tab Pill Row with smooth active color transitions and badge counters.
 */
@Composable
fun ModernTabRow(
  tabs: List<ModernTabItem>,
  selectedTabIndex: Int,
  onTabSelected: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val containerShape = RoundedCornerShape(16.dp)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(containerShape)
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
      .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), containerShape)
      .padding(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      tabs.forEachIndexed { index, tab ->
        val isSelected = index == selectedTabIndex

        val backgroundColor by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
          animationSpec = tween(durationMillis = 200),
          label = "tab_bg_$index"
        )

        val contentColor by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
          animationSpec = tween(durationMillis = 200),
          label = "tab_fg_$index"
        )

        val pillShape = RoundedCornerShape(12.dp)

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(pillShape)
            .background(backgroundColor)
            .clickable {
              onTabSelected(index)
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              text = tab.title,
              color = contentColor,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
              overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            if (tab.badgeCount != null && tab.badgeCount > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    CircleShape
                  ),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = if (tab.badgeCount > 99) "99+" else tab.badgeCount.toString(),
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }
  }
}
