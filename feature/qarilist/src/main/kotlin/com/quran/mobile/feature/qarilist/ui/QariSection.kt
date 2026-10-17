package com.quran.mobile.feature.qarilist.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun QariSection(@StringRes sectionHeader: Int, isModernUi: Boolean = false) {
  if (isModernUi) {
    Text(
      text = stringResource(sectionHeader),
      style = MaterialTheme.typography.labelLarge,
      fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
        .padding(horizontal = 20.dp, vertical = 8.dp)
    )
  } else {
    Text(
      text = stringResource(sectionHeader),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSecondaryContainer,
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .padding(8.dp)
    )
  }
}
