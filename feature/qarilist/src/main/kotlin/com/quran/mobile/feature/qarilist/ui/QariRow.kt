package com.quran.mobile.feature.qarilist.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.audio.model.QariItem
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip
import com.quran.mobile.feature.qarilist.R

@Composable
fun QariRow(
  qariItem: QariItem,
  isSelected: Boolean,
  onItemSelected: ((QariItem) -> Unit),
  isModernUi: Boolean = false,
  modifier: Modifier = Modifier
) {
  if (isModernUi) {
    ModernQariRow(qariItem, isSelected, onItemSelected, modifier)
  } else {
    ClassicQariRow(qariItem, isSelected, onItemSelected, modifier)
  }
}

@Composable
private fun ModernQariRow(
  qariItem: QariItem,
  isSelected: Boolean,
  onItemSelected: ((QariItem) -> Unit),
  modifier: Modifier = Modifier
) {
  val surfaceColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
  }

  val borderColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
  }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .clip(RoundedCornerShape(16.dp))
      .clickable { onItemSelected(qariItem) },
    shape = RoundedCornerShape(16.dp),
    color = surfaceColor,
    border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Reciter avatar icon with mic
      Surface(
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier.size(36.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = QuranIcons.Mic,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = qariItem.name,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val styleLabel = if (qariItem.isGapless) "Gapless" else "Ayah-by-Ayah"
          ModernBadgeChip(
            text = styleLabel,
            backgroundColor = if (qariItem.isGapless) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                             else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            contentColor = if (qariItem.isGapless) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (qariItem.hasOpus()) {
            ModernBadgeChip(
              text = "HQ Opus",
              backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
              contentColor = MaterialTheme.colorScheme.tertiary
            )
          }
        }
      }

      if (isSelected) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = QuranIcons.Check,
              contentDescription = stringResource(R.string.qarilist_selected),
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ClassicQariRow(
  qariItem: QariItem,
  isSelected: Boolean,
  onItemSelected: ((QariItem) -> Unit),
  modifier: Modifier = Modifier
) {
  Row(
    modifier
      .fillMaxWidth()
      .clickable { onItemSelected(qariItem) }
      .padding(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Text(
      text = qariItem.name,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.align(Alignment.CenterVertically).weight(1f)
    )

    if (isSelected) {
      Icon(
        imageVector = QuranIcons.Check,
        contentDescription = stringResource(R.string.qarilist_selected),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.align(Alignment.CenterVertically)
      )
    }
  }
}
