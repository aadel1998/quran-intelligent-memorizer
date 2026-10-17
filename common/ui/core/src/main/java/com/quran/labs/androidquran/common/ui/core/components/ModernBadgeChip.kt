package com.quran.labs.androidquran.common.ui.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact pill-shaped badge chip for metadata, revelation type, or status.
 */
@Composable
fun ModernBadgeChip(
  text: String,
  modifier: Modifier = Modifier,
  backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
  contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  borderColor: Color? = null,
  leadingDotColor: Color? = null
) {
  val shape = RoundedCornerShape(8.dp)
  val borderModifier = if (borderColor != null) {
    Modifier.border(0.5.dp, borderColor, shape)
  } else {
    Modifier
  }

  Box(
    modifier = modifier
      .clip(shape)
      .background(backgroundColor)
      .then(borderModifier)
      .padding(horizontal = 8.dp, vertical = 3.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (leadingDotColor != null) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .background(leadingDotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
      }
      Text(
        text = text,
        color = contentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
