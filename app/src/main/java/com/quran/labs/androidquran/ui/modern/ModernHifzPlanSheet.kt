package com.quran.labs.androidquran.ui.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.audio.memorization.HifzPlan
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernCard

/**
 * Bottom sheet to configure a structured Memorization Plan (Easy Quran Memorizer Pro).
 * Calculates target dates, daily pace, and sets up visual tracking.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModernHifzPlanSheet(
  surahs: List<SuraItemModel>,
  existingPlan: HifzPlan? = null,
  onSetHifzPlan: (HifzPlan) -> Unit,
  onClearHifzPlan: (() -> Unit)? = null,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedSuraNumber by remember { mutableIntStateOf(existingPlan?.targetSura ?: 67) }
  var selectedDays by remember { mutableIntStateOf(existingPlan?.daysRemaining ?: 30) }
  var customSuraInput by remember { mutableStateOf(selectedSuraNumber.toString()) }

  val targetSura = surahs.find { it.number == selectedSuraNumber } ?: surahs.firstOrNull { it.number == 67 } ?: surahs.first()
  val totalVerses = targetSura.numberOfAyahs
  val dailyPace = if (selectedDays > 0) ((totalVerses + selectedDays - 1) / selectedDays).coerceAtLeast(1) else totalVerses

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 8.dp,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .padding(bottom = 28.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text("🎯", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Hifz Target Planner",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Set a goal deadline to calculate your daily required memorization pace",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = onDismissRequest) {
          Icon(
            imageVector = QuranIcons.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 1. Select Target Surah
      Text(
        text = "TARGET SURAH",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      val popularSurahs = listOf(
        Pair(67, "Al-Mulk (30v)"),
        Pair(36, "Yā-Sīn (83v)"),
        Pair(18, "Al-Kahf (110v)"),
        Pair(78, "An-Naba (40v)"),
        Pair(2, "Al-Baqarah (286v)"),
        Pair(1, "Al-Fātihah (7v)")
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        popularSurahs.forEach { (num, label) ->
          FilterChip(
            selected = selectedSuraNumber == num,
            onClick = {
              selectedSuraNumber = num
              customSuraInput = num.toString()
            },
            label = { Text(label, fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = customSuraInput,
        onValueChange = { input ->
          customSuraInput = input
          val num = input.toIntOrNull()
          if (num != null && num in 1..114) {
            selectedSuraNumber = num
          }
        },
        label = { Text("Or enter Surah number (1–114)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      // 2. Select Deadline / Days
      Text(
        text = "TARGET TIMEFRAME",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      val dayOptions = listOf(7, 14, 21, 30, 60, 90, 180)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        dayOptions.forEach { days ->
          FilterChip(
            selected = selectedDays == days,
            onClick = { selectedDays = days },
            label = { Text("$days Days", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF10B981),
              selectedLabelColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // 3. Dynamic Calculation Summary Card
      ModernCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        borderWidth = 1.dp,
        contentPadding = 14.dp
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${targetSura.englishName} (${targetSura.arabicName})",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "$totalVerses Ayahs",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Required Daily Pace:",
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "⚡ $dailyPace Ayahs / Day",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF10B981)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Save / Clear Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        if (existingPlan != null && onClearHifzPlan != null) {
          Button(
            onClick = {
              onClearHifzPlan()
              onDismissRequest()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
              contentColor = MaterialTheme.colorScheme.error
            )
          ) {
            Icon(
              imageVector = QuranIcons.Delete,
              contentDescription = null,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Clear", fontSize = 13.sp)
          }
        }

        Button(
          onClick = {
            val targetDateEpoch = System.currentTimeMillis() + (selectedDays.toLong() * 24L * 60L * 60L * 1000L)
            val plan = HifzPlan(
              targetSura = targetSura.number,
              targetSuraName = targetSura.englishName,
              totalVerses = totalVerses,
              targetDateEpoch = targetDateEpoch
            )
            onSetHifzPlan(plan)
            onDismissRequest()
          },
          modifier = Modifier.weight(2f),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
          )
        ) {
          Icon(
            imageVector = QuranIcons.Check,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}
