package com.quran.labs.androidquran.ui.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.audio.memorization.MemorizedSurahInfo
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernJuzPickerSheet(
  selectedJuz: Int?, // null = All Juz
  memorizedInfos: Map<Int, MemorizedSurahInfo>,
  onSelectJuz: (Int?) -> Unit,
  onDismissRequest: () -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp)
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = QuranIcons.CollectionsBookmark,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Select Juz (Para)",
              fontSize = 19.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Filter dashboard and batch-revise entire Juz",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Box(
          modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onDismissRequest),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = QuranIcons.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // "All Juz" Option
        item {
          val isSelected = selectedJuz == null
          ModernCard(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onSelectJuz(null)
                onDismissRequest()
              },
            shape = RoundedCornerShape(14.dp),
            backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            borderColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            borderWidth = if (isSelected) 1.5.dp else 1.dp,
            contentPadding = 12.dp
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .background(
                      if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                      CircleShape
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = QuranIcons.MenuBook,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "All Juz (1 — 30)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Show all 114 Surahs across the entire Quran",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              if (isSelected) {
                Text(
                  text = "Active",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }
          }
        }

        // 30 Individual Juz
        items(
          items = JuzConstants.ALL_JUZ,
          key = { it.number }
        ) { juz ->
          val isSelected = selectedJuz == juz.number
          val memorizedInJuz = juz.surahs.count { memorizedInfos[it]?.isMemorized == true }
          val totalSurahsInJuz = juz.surahs.size
          val progress = if (totalSurahsInJuz > 0) memorizedInJuz.toFloat() / totalSurahsInJuz else 0f
          val isFullyMemorized = memorizedInJuz == totalSurahsInJuz && totalSurahsInJuz > 0

          ModernCard(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onSelectJuz(juz.number)
                onDismissRequest()
              },
            shape = RoundedCornerShape(14.dp),
            backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            borderColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            borderWidth = if (isSelected) 1.5.dp else 1.dp,
            contentPadding = 12.dp
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .background(
                        when {
                          isFullyMemorized -> Color(0xFF10B981)
                          isSelected -> MaterialTheme.colorScheme.primary
                          else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    if (isFullyMemorized) {
                      Icon(
                        imageVector = QuranIcons.Check,
                        contentDescription = "Memorized",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                      )
                    } else {
                      Text(
                        text = "${juz.number}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                      )
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = "Juz ${juz.number}: ${juz.nameEnglish}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      if (isFullyMemorized) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                          imageVector = QuranIcons.Star,
                          contentDescription = "Hāfiz",
                          tint = Color(0xFFEAB308),
                          modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                          text = "Hāfiz",
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold,
                          color = Color(0xFF10B981)
                        )
                      }
                    }
                    Text(
                      text = "${juz.description} (${juz.surahs.size} Surahs)",
                      fontSize = 11.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Text(
                  text = juz.nameArabic,
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              // Mini Progress indicator
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                LinearProgressIndicator(
                  progress = { progress },
                  modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                  color = if (isFullyMemorized) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                  trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "$memorizedInJuz / $totalSurahsInJuz surahs",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (isFullyMemorized) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}
