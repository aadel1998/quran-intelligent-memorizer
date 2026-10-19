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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip
import com.quran.mobile.translation.model.LocalTranslation

/**
 * Sleek Material 3 Bottom Sheet for selecting active Translation / Tafsir.
 */
@Composable
fun ModernTranslationPickerSheet(
  visible: Boolean,
  translations: List<LocalTranslation>,
  activeTranslations: Set<String?>?,
  onSelectTranslation: (LocalTranslation) -> Unit,
  onManageTranslations: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(),
    exit = fadeOut(),
    modifier = modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.55f))
        .clickable(onClick = onDismiss),
      contentAlignment = Alignment.BottomCenter
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = false) {}
          .shadow(elevation = 24.dp, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
          0.5.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
          // Drag handle
          Box(
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .width(38.dp)
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Header Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = QuranIcons.Translate,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
              }

              Column {
                Text(
                  text = stringResource(R.string.picker_trans_title),
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = stringResource(R.string.picker_trans_subtitle),
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = QuranIcons.Close,
                contentDescription = stringResource(R.string.hifz_stats_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Translations List
          if (translations.isEmpty()) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = stringResource(R.string.picker_trans_none_downloaded),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(12.dp))
              Button(
                onClick = onManageTranslations,
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(14.dp)
              ) {
                Text(stringResource(R.string.picker_trans_btn_download))
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(translations) { item ->
                val isActive = activeTranslations?.contains(item.filename) == true ||
                    (activeTranslations.isNullOrEmpty() && translations.firstOrNull()?.filename == item.filename)

                val itemBg = if (isActive) {
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
                val borderStroke = if (isActive) {
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                  MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                }

                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectTranslation(item) }
                    .background(itemBg)
                    .border(0.5.dp, borderStroke, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    // Check Indicator
                    Box(
                      modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                          if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .border(
                          1.5.dp,
                          if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                          CircleShape
                        ),
                      contentAlignment = Alignment.Center
                    ) {
                      if (isActive) {
                        Icon(
                          imageVector = QuranIcons.Check,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.onPrimary,
                          modifier = Modifier.size(14.dp)
                        )
                      }
                    }

                    Column {
                      Text(
                        text = item.resolveTranslatorName(),
                        fontSize = 15.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      if (!item.languageCode.isNullOrEmpty()) {
                        Text(
                          text = item.languageCode?.uppercase() ?: "",
                          fontSize = 11.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }

                  if (isActive) {
                    ModernBadgeChip(
                      text = stringResource(R.string.picker_trans_badge_active),
                      backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                      contentColor = MaterialTheme.colorScheme.primary
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Bottom Action: Manage Translations
          OutlinedButton(
            onClick = onManageTranslations,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = QuranIcons.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = stringResource(R.string.picker_trans_btn_manage),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
        }
      }
    }
  }
}
