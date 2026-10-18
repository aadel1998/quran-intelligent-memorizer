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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.quran.labs.androidquran.common.audio.memorization.HifzPlan
import com.quran.labs.androidquran.common.audio.memorization.MemorizationProfile
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
  dailyGoal: Int = 5,
  ayahsToday: Int = 0,
  currentStreak: Int = 0,
  bestStreak: Int = 0,
  onUpdateDailyGoal: ((Int) -> Unit)? = null,
  flaggedSurahs: Set<Int> = emptySet(),
  onPracticeFlagged: ((sura: Int) -> Unit)? = null,
  onResetStreak: (() -> Unit)? = null,
  onClearAllFlagged: (() -> Unit)? = null,
  totalFlaggedAyahs: Int = 0,
  profiles: List<MemorizationProfile> = listOf(MemorizationProfile("default", "Personal", 0L)),
  activeProfile: MemorizationProfile = MemorizationProfile("default", "Personal", 0L),
  onSwitchProfile: ((String) -> Unit)? = null,
  onCreateProfile: ((String) -> Unit)? = null,
  onDeleteProfile: ((String) -> Unit)? = null,
  hifzPlan: HifzPlan? = null,
  onSetHifzPlan: ((HifzPlan) -> Unit)? = null,
  onClearHifzPlan: (() -> Unit)? = null,
  isDailyReminderEnabled: Boolean = false,
  dailyReminderTime: Pair<Int, Int> = Pair(9, 0),
  onToggleReminder: ((Boolean) -> Unit)? = null,
  onSetReminderTime: ((Int, Int) -> Unit)? = null,
  onSendTestReminder: (() -> Unit)? = null,
  onExportBackup: (() -> String)? = null,
  onImportBackup: ((String) -> Boolean)? = null,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  // 0: All, 1: Due, 2: Memorized, 3: In Queue, 4: Flagged
  var selectedFilterTab by remember { mutableIntStateOf(0) }
  var selectedJuz by remember { mutableStateOf<Int?>(null) }
  var showJuzSheet by remember { mutableStateOf(false) }
  var showBackupSheet by remember { mutableStateOf(false) }
  var quizSura by remember { mutableStateOf<SuraItemModel?>(null) }
  var showStatsDialog by remember { mutableStateOf(false) }
  var showProfileSheet by remember { mutableStateOf(false) }
  var showPlanSheet by remember { mutableStateOf(false) }

  val memorizedCount = surahs.count { memorizedInfos[it.number]?.isMemorized == true }
  val dueCount = surahs.count { memorizedInfos[it.number]?.isDue == true }
  val totalMemorizedAyahs = remember(surahs, memorizedInfos) {
    surahs.filter { memorizedInfos[it.number]?.isMemorized == true }.sumOf { it.numberOfAyahs }
  }

  // Filter surahs based on tab & query & juz
  val filteredSurahs = remember(surahs, memorizedInfos, selectedQueue, searchQuery, selectedFilterTab, flaggedSurahs, selectedJuz) {
    val juzSurahs = selectedJuz?.let { JuzConstants.getJuz(it)?.surahs }
    surahs.filter { sura ->
      val info = memorizedInfos[sura.number]
      val matchesTab = when (selectedFilterTab) {
        1 -> info?.isDue == true
        2 -> info?.isMemorized == true
        3 -> selectedQueue.contains(sura.number)
        4 -> flaggedSurahs.contains(sura.number) || (info?.flaggedAyahsCount ?: 0) > 0
        else -> true
      }
      val matchesQuery = if (searchQuery.isBlank()) {
        true
      } else {
        sura.englishName.contains(searchQuery, ignoreCase = true) ||
          sura.arabicName.contains(searchQuery) ||
          sura.number.toString() == searchQuery.trim()
      }
      val matchesJuz = juzSurahs == null || juzSurahs.contains(sura.number)
      matchesTab && matchesQuery && matchesJuz
    }
  }

  // 0: Surah Number (1..114), 1: Revision Due First, 2: Most Verses Memorized
  var sortOrder by remember { mutableIntStateOf(0) }

  val sortedSurahs = remember(filteredSurahs, sortOrder, memorizedInfos) {
    when (sortOrder) {
      1 -> filteredSurahs.sortedWith(
        compareByDescending<SuraItemModel> { memorizedInfos[it.number]?.isDue == true }
          .thenBy { it.number }
      )
      2 -> filteredSurahs.sortedWith(
        compareByDescending<SuraItemModel> { memorizedInfos[it.number]?.isMemorized == true }
          .thenByDescending { it.numberOfAyahs }
          .thenBy { it.number }
      )
      else -> filteredSurahs.sortedBy { it.number }
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
              .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = QuranIcons.Layers,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "${selectedQueue.size} Surahs Selected",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Ready for batch revision",
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.primary
                )
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              OutlinedButton(
                onClick = onClearQueue,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Icon(QuranIcons.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear", fontSize = 12.sp)
              }

              Button(
                onClick = { onStartRevision(selectedQueue.sorted()) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Icon(
                  imageVector = QuranIcons.Headphones,
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
        Row(
          modifier = Modifier.weight(1f, fill = false),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier
              .size(38.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
          ) {
            Icon(
              imageVector = QuranIcons.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Box(
            modifier = Modifier
              .size(36.dp)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = QuranIcons.MenuBook,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column {
            Text(
              text = "Quran Revision",
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground,
              maxLines = 1
            )
            Text(
              text = "Hifz Mastery",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
          }
        }

        // Top Action Pills: Profile Switcher & Memorized Stat
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Profile Selector Pill
          Box(
            modifier = Modifier
              .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
              )
              .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
              )
              .clip(RoundedCornerShape(20.dp))
              .clickable { showProfileSheet = true }
              .padding(horizontal = 8.dp, vertical = 5.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = QuranIcons.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = activeProfile.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "▾",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.width(5.dp))

          // Memorized Stat Pill
          Box(
            modifier = Modifier
              .background(
                color = Color(0xFFEAB308).copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
              )
              .border(
                width = 1.dp,
                color = Color(0xFFEAB308).copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp)
              )
              .clip(RoundedCornerShape(20.dp))
              .clickable { showStatsDialog = true }
              .padding(horizontal = 8.dp, vertical = 5.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = QuranIcons.Star,
                contentDescription = null,
                tint = Color(0xFFEAB308),
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "$memorizedCount/114",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEAB308)
              )
            }
          }

          if (onExportBackup != null && onImportBackup != null) {
            Spacer(modifier = Modifier.width(5.dp))
            Box(
              modifier = Modifier
                .background(
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                  shape = CircleShape
                )
                .border(
                  width = 1.dp,
                  color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                  shape = CircleShape
                )
                .clip(CircleShape)
                .clickable { showBackupSheet = true }
                .padding(6.dp)
            ) {
              Icon(
                imageVector = QuranIcons.CloudUpload,
                contentDescription = "Backup & Restore",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // Daily Goal & Streak Hero Card (Easy Quran Memorizer Pro)
      ModernCard(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .clickable { showStatsDialog = true },
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
        borderWidth = 1.dp,
        contentPadding = 12.dp
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "🔥 ${if (currentStreak > 0) "$currentStreak Day Streak" else "Start Streak"}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B)
              )
              if (bestStreak > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Best: $bestStreak d",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            // Quick goal change chips (3, 5, 10, 20)
            if (onUpdateDailyGoal != null) {
              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(3, 5, 10, 20).forEach { goalOpt ->
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(
                        if (dailyGoal == goalOpt) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                      )
                      .clickable { onUpdateDailyGoal(goalOpt) }
                      .padding(horizontal = 7.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = "${goalOpt}v",
                      fontSize = 10.sp,
                      fontWeight = if (dailyGoal == goalOpt) FontWeight.Bold else FontWeight.Normal,
                      color = if (dailyGoal == goalOpt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          val safeGoal = if (dailyGoal > 0) dailyGoal else 5
          val progress = (ayahsToday.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Daily Goal: $ayahsToday / $safeGoal Ayahs",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = if (ayahsToday >= safeGoal) "Achieved! ✨" else "${(progress * 100).toInt()}%",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = if (ayahsToday >= safeGoal) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(5.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF10B981),
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        }
      }

      // Active Hifz Plan Card (Easy Quran Memorizer Pro Feature)
      if (hifzPlan != null) {
        val isTargetSuraMemorized = memorizedInfos[hifzPlan.targetSura]?.isMemorized == true
        val versesCompleted = if (isTargetSuraMemorized) hifzPlan.totalVerses else 0
        val daysLeft = hifzPlan.daysRemaining
        val requiredPace = hifzPlan.dailyPaceRequired(versesCompleted)
        val planProgress = if (hifzPlan.totalVerses > 0) (versesCompleted.toFloat() / hifzPlan.totalVerses).coerceIn(0f, 1f) else 0f

        ModernCard(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { showPlanSheet = true },
          shape = RoundedCornerShape(16.dp),
          backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
          borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
          borderWidth = 1.dp,
          contentPadding = 12.dp
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎯", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "TARGET: ${hifzPlan.targetSuraName.ifBlank { "Surah ${hifzPlan.targetSura}" }}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  letterSpacing = 0.5.sp
                )
              }

              TextButton(
                onClick = { onClearHifzPlan?.invoke() },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
              ) {
                Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
              }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "$daysLeft days left • Need $requiredPace v/day",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isTargetSuraMemorized) "Completed! 🎉" else "${(planProgress * 100).toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isTargetSuraMemorized) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
              progress = { planProgress },
              modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
          }
        }
      } else {
        // Hifz Target Plan Prompt Card
        ModernCard(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { showPlanSheet = true },
          shape = RoundedCornerShape(16.dp),
          backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
          borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
          borderWidth = 1.dp,
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
                  .size(34.dp)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text("🎯", fontSize = 16.sp)
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Hifz Target Planner",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Set a deadline & daily pace for any Surah",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Box(
              modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Text(
                text = "+ Set Plan",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
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

      // Filter & Sort Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedFilterTab == 0,
            onClick = { selectedFilterTab = 0 },
            label = { Text("All (114)", fontSize = 12.sp) },
            leadingIcon = {
              Icon(
                imageVector = QuranIcons.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
            },
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
            leadingIcon = {
              Icon(
                imageVector = QuranIcons.Hourglass,
                contentDescription = null,
                tint = if (dueCount > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
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
            leadingIcon = {
              Icon(
                imageVector = QuranIcons.Star,
                contentDescription = null,
                tint = Color(0xFFEAB308),
                modifier = Modifier.size(14.dp)
              )
            },
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
              leadingIcon = {
                Icon(
                  imageVector = QuranIcons.Layers,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
              ),
              shape = RoundedCornerShape(10.dp)
            )
          }

          if (flaggedSurahs.isNotEmpty()) {
            FilterChip(
              selected = selectedFilterTab == 4,
              onClick = { selectedFilterTab = 4 },
              label = { Text("Flagged (${flaggedSurahs.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
              leadingIcon = {
                Icon(
                  imageVector = QuranIcons.Flag,
                  contentDescription = null,
                  tint = Color(0xFFEF4444),
                  modifier = Modifier.size(14.dp)
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                selectedLabelColor = Color(0xFFEF4444)
              ),
              shape = RoundedCornerShape(10.dp)
            )
          }

          FilterChip(
            selected = selectedJuz != null,
            onClick = { showJuzSheet = true },
            label = {
              Text(
                text = if (selectedJuz != null) "Juz $selectedJuz ▾" else "Juz ▾",
                fontSize = 12.sp,
                fontWeight = if (selectedJuz != null) FontWeight.Bold else FontWeight.Normal
              )
            },
            leadingIcon = {
              Icon(
                imageVector = QuranIcons.CollectionsBookmark,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
              )
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            shape = RoundedCornerShape(10.dp)
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Sort Control Pill
        var showSortMenu by remember { mutableStateOf(false) }
        Box {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
              .clickable { showSortMenu = true }
              .padding(horizontal = 8.dp, vertical = 6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = QuranIcons.SwapVert,
                contentDescription = "Sort Order",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = when (sortOrder) {
                  1 -> "Due"
                  2 -> "Verses"
                  else -> "1..114"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
          ) {
            DropdownMenuItem(
              text = { Text("Surah Number (1 → 114)") },
              leadingIcon = {
                Icon(
                  imageVector = QuranIcons.MenuBook,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
              },
              onClick = {
                sortOrder = 0
                showSortMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("Revision Due First") },
              leadingIcon = {
                Icon(
                  imageVector = QuranIcons.Hourglass,
                  contentDescription = null,
                  tint = Color(0xFFF59E0B),
                  modifier = Modifier.size(16.dp)
                )
              },
              onClick = {
                sortOrder = 1
                showSortMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("Most Verses Memorized") },
              leadingIcon = {
                Icon(
                  imageVector = QuranIcons.Star,
                  contentDescription = null,
                  tint = Color(0xFFEAB308),
                  modifier = Modifier.size(16.dp)
                )
              },
              onClick = {
                sortOrder = 2
                showSortMenu = false
              }
            )
          }
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
              4 -> "No flagged Ayahs found. Flag difficult Ayahs during memorization to review them here."
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
          if (selectedJuz != null) {
            item(key = "juz_banner") {
              val juzInfo = JuzConstants.getJuz(selectedJuz!!)
              if (juzInfo != null) {
                ModernCard(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(14.dp),
                  backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                  borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                  borderWidth = 1.dp,
                  contentPadding = 12.dp
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = "Juz ${juzInfo.number}: ${juzInfo.nameEnglish}",
                          fontSize = 13.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          text = juzInfo.nameArabic,
                          fontSize = 15.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary
                        )
                      }
                      Text(
                        text = "${juzInfo.surahs.size} Surahs • ${juzInfo.description}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                      FilledTonalButton(
                        onClick = {
                          juzInfo.surahs.forEach { s ->
                            if (!selectedQueue.contains(s)) {
                              onToggleQueueSelection(s)
                            }
                          }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                      ) {
                        Text("⚡ Select All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }

                      IconButton(
                        onClick = { selectedJuz = null },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          imageVector = QuranIcons.Close,
                          contentDescription = "Clear Juz",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          items(
            items = sortedSurahs,
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
              flaggedAyahsCount = info?.flaggedAyahsCount ?: 0,
              isSelected = isSelectedInQueue,
              onToggleQueue = { onToggleQueueSelection(sura.number) },
              onToggleMemorized = { onToggleMemorized(sura.number, !isMemorized) },
              onStartQuiz = { quizSura = sura },
              onPracticeFlagged = if (onPracticeFlagged != null) { { onPracticeFlagged(sura.number) } } else null
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

  if (showStatsDialog) {
    AlertDialog(
      onDismissRequest = { showStatsDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .background(Color(0xFFEAB308).copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = QuranIcons.Star,
              contentDescription = null,
              tint = Color(0xFFEAB308),
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Hifz Mastery Overview",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = QuranIcons.Star,
                contentDescription = null,
                tint = Color(0xFFEAB308),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Surahs Memorized:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$memorizedCount / 114", fontWeight = FontWeight.Bold, color = Color(0xFFEAB308))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = QuranIcons.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Verses Memorized:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$totalMemorizedAyahs / 6,236", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("🔥 Current Streak:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$currentStreak Days", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("⚡ Longest Streak:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$bestStreak Days", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("🎯 Practiced Today:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$ayahsToday / $dailyGoal v", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = QuranIcons.Flag,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Flagged Mistakes:", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("$totalFlaggedAyahs Verses", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

          // Memorization Target Plan Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("🎯 Memorization Plan:", color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = if (hifzPlan != null) "${hifzPlan.targetSuraName} (${hifzPlan.daysRemaining}d left)"
                       else "No active plan",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            TextButton(
              onClick = {
                showStatsDialog = false
                showPlanSheet = true
              }
            ) {
              Text(if (hifzPlan != null) "Edit Plan" else "+ Set Plan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

          // Daily Reminder Notification Settings (Easy Quran Memorizer Pro)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("⏰ Daily Hifz Reminder:", color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                text = if (isDailyReminderEnabled) {
                  val hourStr = dailyReminderTime.first.toString().padStart(2, '0')
                  val minStr = dailyReminderTime.second.toString().padStart(2, '0')
                  "Alarm set for $hourStr:$minStr"
                } else "Disabled",
                fontSize = 11.sp,
                color = if (isDailyReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
              )
            }

            Switch(
              checked = isDailyReminderEnabled,
              onCheckedChange = { onToggleReminder?.invoke(it) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
              )
            )
          }

          if (isDailyReminderEnabled && onSendTestReminder != null) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextButton(
                onClick = {
                  val nextTime = when (dailyReminderTime.first) {
                    7 -> Pair(8, 30)
                    8 -> Pair(12, 30)
                    12 -> Pair(18, 0)
                    18 -> Pair(20, 30)
                    20 -> Pair(21, 30)
                    else -> Pair(7, 0)
                  }
                  onSetReminderTime?.invoke(nextTime.first, nextTime.second)
                }
              ) {
                val hourStr = dailyReminderTime.first.toString().padStart(2, '0')
                val minStr = dailyReminderTime.second.toString().padStart(2, '0')
                Text("⏰ Time: $hourStr:$minStr ▾", fontSize = 11.sp)
              }

              TextButton(
                onClick = onSendTestReminder
              ) {
                Text("🔔 Test Alarm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

          // 30 Juz Mastery Matrix & Backup & Restore Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilledTonalButton(
              onClick = {
                showStatsDialog = false
                showJuzSheet = true
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(
                imageVector = QuranIcons.CollectionsBookmark,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("30 Juz Matrix", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            if (onExportBackup != null && onImportBackup != null) {
              FilledTonalButton(
                onClick = {
                  showStatsDialog = false
                  showBackupSheet = true
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(
                  imageVector = QuranIcons.CloudUpload,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (onResetStreak != null || onClearAllFlagged != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              if (onResetStreak != null) {
                OutlinedButton(
                  onClick = {
                    onResetStreak()
                    showStatsDialog = false
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Reset Streak", fontSize = 11.sp)
                }
              }
              if (onClearAllFlagged != null && totalFlaggedAyahs > 0) {
                OutlinedButton(
                  onClick = {
                    onClearAllFlagged()
                    showStatsDialog = false
                  },
                  modifier = Modifier.weight(1f)
                ) {
                  Text("Clear Flags", fontSize = 11.sp)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(onClick = { showStatsDialog = false }) {
          Text("Done")
        }
      }
    )
  }

  if (showProfileSheet) {
    ModernProfileManagementSheet(
      profiles = profiles,
      activeProfile = activeProfile,
      onSwitchProfile = { id -> onSwitchProfile?.invoke(id) },
      onCreateProfile = { name -> onCreateProfile?.invoke(name) },
      onDeleteProfile = { id -> onDeleteProfile?.invoke(id) },
      onDismissRequest = { showProfileSheet = false }
    )
  }

  if (showPlanSheet) {
    ModernHifzPlanSheet(
      surahs = surahs,
      existingPlan = hifzPlan,
      onSetHifzPlan = { plan -> onSetHifzPlan?.invoke(plan) },
      onClearHifzPlan = if (onClearHifzPlan != null) { { onClearHifzPlan() } } else null,
      onDismissRequest = { showPlanSheet = false }
    )
  }

  if (showJuzSheet) {
    ModernJuzPickerSheet(
      selectedJuz = selectedJuz,
      memorizedInfos = memorizedInfos,
      onSelectJuz = { selectedJuz = it },
      onDismissRequest = { showJuzSheet = false }
    )
  }

  val exportFn = onExportBackup
  val importFn = onImportBackup
  if (showBackupSheet && exportFn != null && importFn != null) {
    ModernBackupRestoreSheet(
      onExportJson = exportFn,
      onImportJson = importFn,
      onDismissRequest = { showBackupSheet = false }
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
  flaggedAyahsCount: Int = 0,
  onPracticeFlagged: (() -> Unit)? = null,
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
                .background(Color(0xFFF59E0B).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = QuranIcons.Hourglass,
                  contentDescription = null,
                  tint = Color(0xFFF59E0B),
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "DUE",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFF59E0B)
                )
              }
            }
          }

          if (flaggedAyahsCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = QuranIcons.Flag,
                  contentDescription = null,
                  tint = Color(0xFFEF4444),
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "$flaggedAyahsCount",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFEF4444)
                )
              }
            }
          }

          if (isMemorized && !isDue) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
              modifier = Modifier
                .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = QuranIcons.Check,
                  contentDescription = null,
                  tint = Color(0xFF10B981),
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "FRESH",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF10B981)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = QuranIcons.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(11.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "${sura.numberOfAyahs} ayahs",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          if (lastRevised > 0L) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "• 🕒 ${getRelativeTimeText(lastRevised)}",
              fontSize = 10.sp,
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

      // Practice Flagged Verses Button
      if (flaggedAyahsCount > 0 && onPracticeFlagged != null) {
        IconButton(
          onClick = onPracticeFlagged,
          modifier = Modifier
            .size(36.dp)
            .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
        ) {
          Icon(
            imageVector = QuranIcons.Flag,
            contentDescription = "Practice Flagged Verses",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
      }

      // Recall Quiz Button (Graduation Cap / Academic Recall)
      IconButton(
        onClick = onStartQuiz,
        modifier = Modifier
          .size(36.dp)
          .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape)
      ) {
        Icon(
          imageVector = QuranIcons.School,
          contentDescription = "Active Recall Quiz",
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
            if (isMemorized) Color(0xFFEAB308).copy(alpha = 0.18f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            CircleShape
          )
      ) {
        Icon(
          imageVector = if (isMemorized) QuranIcons.Star else QuranIcons.BookmarkBorder,
          contentDescription = if (isMemorized) "Memorized" else "Mark as memorized",
          tint = if (isMemorized) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
