package com.quran.labs.androidquran.ui.modern

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import com.quran.labs.androidquran.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip
import com.quran.labs.androidquran.common.ui.core.components.ModernTabItem
import com.quran.labs.androidquran.common.ui.core.components.ModernTabRow
import com.quran.labs.androidquran.common.ui.core.components.SurahNumberBadge

/**
 * Modern Jump Sheet (Phase 4.1).
 * Sleek Material 3 bottom sheet providing instant multi-modal navigation across
 * Page (numeric keypad + presets), Surah & Ayah (searchable filter + stepper), and Juz.
 */
@Composable
fun ModernJumpSheet(
  visible: Boolean,
  currentPage: Int,
  totalPages: Int,
  suraNames: List<String>,
  onJumpToPage: (page: Int) -> Unit,
  onJumpToSuraAyah: (sura: Int, ayah: Int) -> Unit,
  onDismiss: () -> Unit,
  getSuraForPage: (page: Int) -> Int,
  getFirstAyahForPage: (page: Int) -> Int,
  getPageForSuraAyah: (sura: Int, ayah: Int) -> Int,
  getNumberOfAyahs: (sura: Int) -> Int,
  getStartingPageForJuz: (juz: Int) -> Int,
  getJuzForPage: (page: Int) -> Int,
  modifier: Modifier = Modifier
) {
  if (!visible) return

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Page, 1: Surah, 2: Juz

  // Page Tab state
  var inputPageString by remember(currentPage) { mutableStateOf(currentPage.toString()) }
  val resolvedPage = inputPageString.toIntOrNull()?.coerceIn(1, totalPages) ?: currentPage

  // Surah Tab state
  val currentSura = remember(currentPage) { getSuraForPage(currentPage) }
  var selectedSura by remember(currentSura) { mutableIntStateOf(currentSura) }
  var selectedAyah by remember { mutableIntStateOf(1) }
  var suraSearchQuery by remember { mutableStateOf("") }

  // Juz Tab state
  val currentJuz = remember(currentPage) { getJuzForPage(currentPage) }
  var selectedJuz by remember(currentJuz) { mutableIntStateOf(currentJuz) }

  val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.55f))
      .clickable { onDismiss() },
    contentAlignment = Alignment.BottomCenter
  ) {
    AnimatedVisibility(
      visible = visible,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.82f)
          .clickable(enabled = false) {}, // Prevent dismiss click inside sheet
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
          0.5.dp,
          MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        shadowElevation = 16.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = navBarPadding)
        ) {
          // Top Drag Handle Pill
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .width(42.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
          }

          // Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(
                  0.5.dp,
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = QuranIcons.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }

              Column {
                Text(
                  text = stringResource(R.string.jump_sheet_title),
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = stringResource(R.string.jump_sheet_subtitle),
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
                contentDescription = stringResource(android.R.string.cancel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          // Mode Selector Tabs
          ModernTabRow(
            tabs = listOf(
              ModernTabItem(title = stringResource(R.string.jump_tab_page), icon = QuranIcons.MenuBook),
              ModernTabItem(title = stringResource(R.string.jump_tab_surah), icon = QuranIcons.List),
              ModernTabItem(title = stringResource(R.string.jump_tab_juz), icon = QuranIcons.Layers)
            ),
            selectedTabIndex = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )

          // Content body based on active tab
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
          ) {
            when (selectedTab) {
              0 -> PageJumpContent(
                resolvedPage = resolvedPage,
                inputPageString = inputPageString,
                totalPages = totalPages,
                suraNames = suraNames,
                getSuraForPage = getSuraForPage,
                getFirstAyahForPage = getFirstAyahForPage,
                getJuzForPage = getJuzForPage,
                onDigitPressed = { digit ->
                  if (inputPageString.length < 3) {
                    val candidate = (inputPageString + digit).toIntOrNull()
                    if (candidate != null && candidate <= totalPages) {
                      inputPageString += digit
                    } else if (candidate != null && candidate > totalPages) {
                      inputPageString = totalPages.toString()
                    }
                  }
                },
                onClearPressed = {
                  inputPageString = ""
                },
                onBackspacePressed = {
                  if (inputPageString.isNotEmpty()) {
                    inputPageString = inputPageString.dropLast(1)
                  }
                },
                onSelectPreset = { p ->
                  inputPageString = p.toString()
                }
              )

              1 -> SurahJumpContent(
                selectedSura = selectedSura,
                selectedAyah = selectedAyah,
                suraNames = suraNames,
                searchQuery = suraSearchQuery,
                onSearchQueryChanged = { suraSearchQuery = it },
                onSelectSura = { s ->
                  selectedSura = s
                  val max = getNumberOfAyahs(s)
                  if (selectedAyah > max) selectedAyah = 1
                },
                onSelectAyah = { a ->
                  selectedAyah = a
                },
                getNumberOfAyahs = getNumberOfAyahs,
                getPageForSuraAyah = getPageForSuraAyah
              )

              2 -> JuzJumpContent(
                selectedJuz = selectedJuz,
                totalPages = totalPages,
                suraNames = suraNames,
                getStartingPageForJuz = getStartingPageForJuz,
                getSuraForPage = getSuraForPage,
                onSelectJuz = { j ->
                  selectedJuz = j
                }
              )
            }
          }

          // Bottom Action Button
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.Transparent
          ) {
            Button(
              onClick = {
                when (selectedTab) {
                  0 -> onJumpToPage(resolvedPage)
                  1 -> onJumpToSuraAyah(selectedSura, selectedAyah)
                  2 -> onJumpToPage(getStartingPageForJuz(selectedJuz))
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              )
            ) {
              Icon(
                imageVector = QuranIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              val buttonText = when (selectedTab) {
                0 -> stringResource(R.string.jump_btn_page, resolvedPage)
                1 -> {
                  val name = suraNames.getOrNull(selectedSura - 1) ?: "Surah $selectedSura"
                  stringResource(R.string.jump_btn_ayah, name, selectedAyah)
                }
                2 -> stringResource(R.string.jump_btn_juz, selectedJuz, getStartingPageForJuz(selectedJuz))
                else -> stringResource(R.string.jump_btn_general)
              }
              Text(
                text = buttonText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Tab 0: Direct Page Jump with In-App Number Pad & Presets.
 */
@Composable
private fun PageJumpContent(
  resolvedPage: Int,
  inputPageString: String,
  totalPages: Int,
  suraNames: List<String>,
  getSuraForPage: (page: Int) -> Int,
  getFirstAyahForPage: (page: Int) -> Int,
  getJuzForPage: (page: Int) -> Int,
  onDigitPressed: (String) -> Unit,
  onClearPressed: () -> Unit,
  onBackspacePressed: () -> Unit,
  onSelectPreset: (Int) -> Unit
) {
  val suraNumber = getSuraForPage(resolvedPage)
  val suraName = suraNames.getOrNull(suraNumber - 1) ?: "Surah $suraNumber"
  val firstAyah = getFirstAyahForPage(resolvedPage)
  val juzNumber = getJuzForPage(resolvedPage)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Current / Target Page Indicator Card
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = if (inputPageString.isEmpty()) "—" else stringResource(R.string.bookmarks_page_format, resolvedPage),
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = stringResource(R.string.jump_page_of_total, totalPages),
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = "$suraName • Ayah $firstAyah • Juz $juzNumber",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        ModernBadgeChip(
          text = stringResource(R.string.juz_title_format, juzNumber),
          backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
          contentColor = MaterialTheme.colorScheme.primary
        )
      }
    }

    // Quick Presets Row
    Column(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = stringResource(R.string.jump_popular_jumps),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 6.dp)
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val presets = listOf(
          "Start (p. 1)" to 1,
          "Al-Baqarah (p. 2)" to 2,
          "Al-Kahf (p. 293)" to 293,
          "Yasin (p. 440)" to 440,
          "Al-Mulk (p. 562)" to 562,
          "Juz 'Amma (p. 582)" to 582
        )

        presets.forEach { (label, p) ->
          val isSelected = resolvedPage == p
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
              0.5.dp,
              if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
              else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { onSelectPreset(p) }
          ) {
            Text(
              text = label,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
            )
          }
        }
      }
    }

    // Modern Numeric Keypad
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      val keypadRows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
      )

      keypadRows.forEach { rowKeys ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          rowKeys.forEach { key ->
            val isAction = key == "C" || key == "⌫"
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isAction) MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                      else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
              border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
              ),
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                  when (key) {
                    "C" -> onClearPressed()
                    "⌫" -> onBackspacePressed()
                    else -> onDigitPressed(key)
                  }
                }
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Text(
                  text = key,
                  fontSize = if (isAction) 16.sp else 19.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (isAction) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Tab 1: Surah & Ayah Jump with Search Filter & Stepper.
 */
@Composable
private fun SurahJumpContent(
  selectedSura: Int,
  selectedAyah: Int,
  suraNames: List<String>,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onSelectSura: (Int) -> Unit,
  onSelectAyah: (Int) -> Unit,
  getNumberOfAyahs: (Int) -> Int,
  getPageForSuraAyah: (Int, Int) -> Int
) {
  val maxAyahs = getNumberOfAyahs(selectedSura)
  val targetPage = getPageForSuraAyah(selectedSura, selectedAyah)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Search Bar
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = QuranIcons.Search,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChanged,
          placeholder = {
            Text(
              text = stringResource(R.string.jump_search_surah_hint),
              fontSize = 13.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          singleLine = true,
          modifier = Modifier.weight(1f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
          )
        )
        if (searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { onSearchQueryChanged("") },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = QuranIcons.Close,
              contentDescription = stringResource(R.string.action_clear),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // Ayah Stepper Card for Selected Surah
    val selectedSuraName = suraNames.getOrNull(selectedSura - 1) ?: "Surah $selectedSura"
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
      border = androidx.compose.foundation.BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "$selectedSura. $selectedSuraName",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = stringResource(R.string.jump_total_verses_format, targetPage, maxAyahs),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Ayah stepper
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .clickable {
                if (selectedAyah > 1) onSelectAyah(selectedAyah - 1)
              }
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            modifier = Modifier.padding(horizontal = 4.dp)
          ) {
            Text(
              text = stringResource(R.string.jump_ayah_indicator, selectedAyah),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .clickable {
                if (selectedAyah < maxAyahs) onSelectAyah(selectedAyah + 1)
              }
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
          }
        }
      }
    }

    // Filtered Surah List
    val filteredSuras = remember(searchQuery, suraNames) {
      (1..114).filter { suraNum ->
        if (searchQuery.isBlank()) true
        else {
          val name = suraNames.getOrNull(suraNum - 1) ?: ""
          name.contains(searchQuery, ignoreCase = true) || suraNum.toString().contains(searchQuery)
        }
      }
    }

    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      items(filteredSuras) { suraNum ->
        val name = suraNames.getOrNull(suraNum - 1) ?: "Surah $suraNum"
        val isSelected = suraNum == selectedSura
        val ayahs = getNumberOfAyahs(suraNum)
        val startPage = getPageForSuraAyah(suraNum, 1)

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelectSura(suraNum) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              SurahNumberBadge(
                number = suraNum,
                size = 28.dp
              )

              Column {
                Text(
                  text = name,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = stringResource(R.string.jump_total_verses_format, startPage, ayahs),
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (isSelected) {
              Icon(
                imageVector = QuranIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Tab 2: Juz Jump with Grid/List of all 30 Juz.
 */
@Composable
private fun JuzJumpContent(
  selectedJuz: Int,
  totalPages: Int,
  suraNames: List<String>,
  getStartingPageForJuz: (Int) -> Int,
  getSuraForPage: (Int) -> Int,
  onSelectJuz: (Int) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    items(30) { index ->
      val juzNum = index + 1
      val startPage = getStartingPageForJuz(juzNum)
      val suraNum = getSuraForPage(startPage)
      val suraName = suraNames.getOrNull(suraNum - 1) ?: "Surah $suraNum"
      val isSelected = juzNum == selectedJuz

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
          0.5.dp,
          if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
          else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .clickable { onSelectJuz(juzNum) }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              modifier = Modifier.size(30.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "$juzNum",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
              }
            }

            Column {
              Text(
                text = stringResource(R.string.juz_title_format, juzNum),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "$suraName • " + stringResource(R.string.juz_begins_at, stringResource(R.string.bookmarks_page_format, startPage)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          ModernBadgeChip(
            text = stringResource(R.string.bookmarks_page_format, startPage),
            backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = if (isSelected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
