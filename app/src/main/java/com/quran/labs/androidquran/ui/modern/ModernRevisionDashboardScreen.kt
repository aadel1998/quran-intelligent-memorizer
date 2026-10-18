package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.audio.memorization.MemorizedSurahInfo
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernCard

/**
 * Easy Quran Memorizer Pro - Special Revision Dashboard.
 *
 * Dedicated hub for tracking memorized Surahs, scheduling revisions,
 * filtering due Surahs, and launching automated audio revision sessions.
 */
@Composable
fun ModernRevisionDashboardScreen(
  surahs: List<SuraItemModel>,
  memorizedInfos: Map<Int, MemorizedSurahInfo>,
  selectedQueue: Set<Int>,
  onToggleMemorized: (sura: Int, isMemorized: Boolean) -> Unit,
  onToggleQueueSelection: (sura: Int) -> Unit,
  onSelectAllDue: () -> Unit,
  onClearQueue: () -> Unit,
  onStartRevision: (List<Int>) -> Unit,
  onBackClick: () -> Unit,
  onQuizResult: ((sura: Int, passed: Boolean) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  // 0: All, 1: Due, 2: Memorized, 3: In Queue
  var selectedFilterTab by remember { mutableIntStateOf(0) }
  var quizSura by remember { mutableStateOf<SuraItemModel?>(null) }

  val memorizedCount = surahs.count { memorizedInfos[it.number]?.isMemorized == true }
  val dueCount = surahs.count { memorizedInfos[it.number]?.isDue == true }

  // Filter surahs based on tab & query
  val filteredSurahs = remember(surahs, memorizedInfos, selectedQueue, searchQuery, selectedFilterTab) {
    surahs.filter { sura ->
      val info = memorizedInfos[sura.number]
      val matchesTab = when (selectedFilterTab) {
        1 -> info?.isDue == true
        2 -> info?.isMemorized == true
        3 -> selectedQueue.contains(sura.number)
        else -> true
      }
      val matchesQuery = if (searchQuery.isBlank()) {
        true
      } else {
        sura.englishName.contains(searchQuery, ignoreCase = true) ||
          sura.arabicName.contains(searchQuery) ||
          sura.number.toString() == searchQuery.trim()
      }
      matchesTab && matchesQuery
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding(),
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      if (selectedQueue.isNotEmpty()) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp,
          shadowElevation = 16.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "${selectedQueue.size} Surahs Selected",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Ready for revision session",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              TextButton(onClick = onClearQueue) {
                Text("Clear", color = MaterialTheme.colorScheme.onSurfaceVariant)
              }

              Button(
                onClick = { onStartRevision(selectedQueue.sorted()) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(
                  imageVector = QuranIcons.PlayArrow,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Revise", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Top App Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier
              .size(40.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
          ) {
            Icon(
              imageVector = QuranIcons.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "Revision Dashboard",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "Special tracking for memorized Surahs",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Memorized Stat Pill
        Box(
          modifier = Modifier
            .background(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(20.dp)
            )
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
              shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = QuranIcons.BookmarkFilled,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$memorizedCount/114",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      // Search Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search by Surah name or number...", fontSize = 14.sp) },
          leadingIcon = {
            Icon(
              imageVector = QuranIcons.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
          },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(
                  imageVector = QuranIcons.Close,
                  contentDescription = "Clear",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }

      // Filter Chips Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = selectedFilterTab == 0,
          onClick = { selectedFilterTab = 0 },
          label = { Text("All (114)", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          shape = RoundedCornerShape(10.dp)
        )

        FilterChip(
          selected = selectedFilterTab == 1,
          onClick = { selectedFilterTab = 1 },
          label = {
            Text(
              text = if (dueCount > 0) "Due ($dueCount)" else "Due",
              fontSize = 12.sp,
              fontWeight = if (dueCount > 0) FontWeight.Bold else FontWeight.Normal
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          shape = RoundedCornerShape(10.dp)
        )

        FilterChip(
          selected = selectedFilterTab == 2,
          onClick = { selectedFilterTab = 2 },
          label = { Text("Memorized ($memorizedCount)", fontSize = 12.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
          ),
          shape = RoundedCornerShape(10.dp)
        )

        if (selectedQueue.isNotEmpty()) {
          FilterChip(
            selected = selectedFilterTab == 3,
            onClick = { selectedFilterTab = 3 },
            label = { Text("Queue (${selectedQueue.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(10.dp)
          )
        }
      }

      // Quick action bar (e.g. Select All Due)
      if (dueCount > 0) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "$dueCount ${if (dueCount == 1) "Surah is" else "Surahs are"} due for revision",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
          )

          TextButton(onClick = onSelectAllDue) {
            Text("Select All Due", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Surahs List
      if (filteredSurahs.isEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(64.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = QuranIcons.BookmarkFilled,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "No Surahs Found",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = when (selectedFilterTab) {
              1 -> "Great job! None of your memorized Surahs are currently due for revision."
              2 -> "You haven't marked any Surahs as memorized yet. Tap the checkmark icon on any Surah to track it here."
              3 -> "Your revision queue is empty. Select Surahs to start an audio revision session."
              else -> "No Surahs match '$searchQuery'."
            },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(
            items = filteredSurahs,
            key = { it.number }
          ) { sura ->
            val info = memorizedInfos[sura.number]
            val isMemorized = info?.isMemorized == true
            val isDue = info?.isDue == true
            val isSelectedInQueue = selectedQueue.contains(sura.number)

            RevisionSurahCard(
              sura = sura,
              isMemorized = isMemorized,
              isDue = isDue,
              lastRevised = info?.lastRevised ?: 0L,
              isSelected = isSelectedInQueue,
              onToggleQueue = { onToggleQueueSelection(sura.number) },
              onToggleMemorized = { onToggleMemorized(sura.number, !isMemorized) },
              onStartQuiz = { quizSura = sura }
            )
          }
        }
      }
    }
  }

  val activeQuiz = quizSura
  if (activeQuiz != null) {
    ModernRecallQuizSheet(
      sura = activeQuiz,
      onQuizResult = { passed ->
        onQuizResult?.invoke(activeQuiz.number, passed)
      },
      onDismissRequest = { quizSura = null }
    )
  }
}

@Composable
private fun RevisionSurahCard(
  sura: SuraItemModel,
  isMemorized: Boolean,
  isDue: Boolean,
  lastRevised: Long,
  isSelected: Boolean,
  onToggleQueue: () -> Unit,
  onToggleMemorized: () -> Unit,
  onStartQuiz: () -> Unit,
  modifier: Modifier = Modifier
) {
  val backgroundColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
  } else {
    MaterialTheme.colorScheme.surface
  }

  val borderColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
  }

  ModernCard(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onToggleQueue),
    shape = RoundedCornerShape(16.dp),
    backgroundColor = backgroundColor,
    borderColor = borderColor,
    borderWidth = 1.dp,
    contentPadding = 12.dp
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Selection Checkbox
      Checkbox(
        checked = isSelected,
        onCheckedChange = { onToggleQueue() },
        colors = CheckboxDefaults.colors(
          checkedColor = MaterialTheme.colorScheme.primary,
          uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier.size(24.dp)
      )

      Spacer(modifier = Modifier.width(12.dp))

      // Surah Number Badge
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(
            if (isMemorized) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          ),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = sura.number.toString(),
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = if (isMemorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Name & Ayah details
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = sura.englishName,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )

          if (isDue) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "DUE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${sura.numberOfAyahs} ayahs",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (lastRevised > 0L) {
            Text(
              text = " • Revised ${getRelativeTimeText(lastRevised)}",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
          }
        }
      }

      // Arabic Name
      Text(
        text = sura.arabicName,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp)
      )

      // Recall Quiz Button
      IconButton(
        onClick = onStartQuiz,
        modifier = Modifier
          .size(36.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
      ) {
        Icon(
          imageVector = QuranIcons.Speed,
          contentDescription = "Recall Quiz",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Memorized Status Toggle Button
      IconButton(
        onClick = onToggleMemorized,
        modifier = Modifier
          .size(36.dp)
          .background(
            if (isMemorized) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            CircleShape
          )
      ) {
        Icon(
          imageVector = if (isMemorized) QuranIcons.Check else QuranIcons.BookmarkBorder,
          contentDescription = if (isMemorized) "Memorized" else "Mark as memorized",
          tint = if (isMemorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

private fun getRelativeTimeText(timestamp: Long): String {
  val diff = System.currentTimeMillis() - timestamp
  val seconds = diff / 1000
  val minutes = seconds / 60
  val hours = minutes / 60
  val days = hours / 24

  return when {
    days > 30 -> "${days / 30}mo ago"
    days > 0 -> "${days}d ago"
    hours > 0 -> "${hours}h ago"
    minutes > 0 -> "${minutes}m ago"
    else -> "just now"
  }
}
