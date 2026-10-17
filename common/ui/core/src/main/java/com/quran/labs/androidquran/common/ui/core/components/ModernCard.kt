package com.quran.labs.androidquran.common.ui.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Modern Surface Card with subtle outline border and soft tonal surface background.
 * Matches the design language of Intelligent Memorizer sheets and HUD capsules.
 */
@Composable
fun ModernCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(16.dp),
  backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
  borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
  borderWidth: Dp = 0.5.dp,
  onClick: (() -> Unit)? = null,
  contentPadding: Dp = 16.dp,
  content: @Composable BoxScope.() -> Unit
) {
  val clickModifier = if (onClick != null) {
    Modifier
      .clip(shape)
      .clickable(onClick = onClick)
  } else {
    Modifier.clip(shape)
  }

  Surface(
    modifier = modifier.then(clickModifier),
    shape = shape,
    color = backgroundColor,
    border = if (borderWidth > 0.dp) BorderStroke(borderWidth, borderColor) else null
  ) {
    Box(
      modifier = Modifier.padding(contentPadding),
      content = content
    )
  }
}
