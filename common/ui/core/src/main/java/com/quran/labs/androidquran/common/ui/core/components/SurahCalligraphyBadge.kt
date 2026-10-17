package com.quran.labs.androidquran.common.ui.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modern Surah Number Badge with rounded container, subtle border, and vibrant typography.
 */
@Composable
fun SurahNumberBadge(
  number: Int,
  modifier: Modifier = Modifier,
  size: Dp = 42.dp,
  backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
  borderColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
  textColor: Color = MaterialTheme.colorScheme.primary
) {
  val shape = RoundedCornerShape(12.dp)
  Box(
    modifier = modifier
      .size(size)
      .clip(shape)
      .background(backgroundColor)
      .border(BorderStroke(1.dp, borderColor), shape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = number.toString(),
      color = textColor,
      fontSize = if (number >= 100) 13.sp else 15.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
