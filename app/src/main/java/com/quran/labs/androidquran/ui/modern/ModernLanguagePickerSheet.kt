package com.quran.labs.androidquran.ui.modern

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.quran.mobile.common.ui.core.R as CoreR
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernCard

/**
 * Data model representing a supported language in the Quran app.
 */
data class SupportedLanguage(
  val tag: String,
  val nativeName: String,
  val englishName: String,
  val countryName: String,
  val flagEmoji: String,
  val isRtl: Boolean = false
)

/**
 * List of primary global languages supported across all parts of the application.
 */
val ALL_SUPPORTED_LANGUAGES = listOf(
  SupportedLanguage(
    tag = "",
    nativeName = "System Default",
    englishName = "Follow Android System",
    countryName = "Auto",
    flagEmoji = "🌐"
  ),
  SupportedLanguage(
    tag = "ar",
    nativeName = "العربية",
    englishName = "Arabic",
    countryName = "العالم العربي / Saudi Arabia",
    flagEmoji = "🇸🇦",
    isRtl = true
  ),
  SupportedLanguage(
    tag = "fa",
    nativeName = "فارسی",
    englishName = "Persian",
    countryName = "ایران / افغانستان",
    flagEmoji = "🇮🇷",
    isRtl = true
  ),
  SupportedLanguage(
    tag = "ur",
    nativeName = "اردو",
    englishName = "Urdu",
    countryName = "پاکستان / ہندوستان",
    flagEmoji = "🇵🇰",
    isRtl = true
  ),
  SupportedLanguage(
    tag = "in",
    nativeName = "Bahasa Indonesia",
    englishName = "Indonesian",
    countryName = "Indonesia",
    flagEmoji = "🇮🇩"
  ),
  SupportedLanguage(
    tag = "tr",
    nativeName = "Türkçe",
    englishName = "Turkish",
    countryName = "Türkiye",
    flagEmoji = "🇹🇷"
  ),
  SupportedLanguage(
    tag = "bn",
    nativeName = "বাংলা",
    englishName = "Bengali",
    countryName = "বাংলাদেশ / ভারত",
    flagEmoji = "🇧🇩"
  ),
  SupportedLanguage(
    tag = "ms",
    nativeName = "Bahasa Melayu",
    englishName = "Malay",
    countryName = "Malaysia / Brunei",
    flagEmoji = "🇲🇾"
  ),
  SupportedLanguage(
    tag = "en",
    nativeName = "English",
    englishName = "English (US)",
    countryName = "Global / International",
    flagEmoji = "🇺🇸"
  ),
  SupportedLanguage(
    tag = "fr",
    nativeName = "Français",
    englishName = "French",
    countryName = "France / Maghreb",
    flagEmoji = "🇫🇷"
  ),
  SupportedLanguage(
    tag = "ru",
    nativeName = "Русский",
    englishName = "Russian",
    countryName = "Россия / Центральная Азия",
    flagEmoji = "🇷🇺"
  ),
  SupportedLanguage(
    tag = "es",
    nativeName = "Español",
    englishName = "Spanish",
    countryName = "España / Latinoamérica",
    flagEmoji = "🇪🇸"
  ),
  SupportedLanguage(
    tag = "de",
    nativeName = "Deutsch",
    englishName = "German",
    countryName = "Deutschland / Österreich",
    flagEmoji = "🇩🇪"
  )
)

/**
 * Modern interactive language picker bottom sheet.
 * Enables immediate runtime locale switching across the entire application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernLanguagePickerSheet(
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var searchQuery by remember { mutableStateOf("") }

  // Detect current application locale
  val currentLocales = remember { AppCompatDelegate.getApplicationLocales() }
  val activeTag = remember(currentLocales) {
    if (currentLocales.isEmpty) {
      ""
    } else {
      currentLocales.get(0)?.language ?: ""
    }
  }

  val filteredLanguages = remember(searchQuery) {
    if (searchQuery.isBlank()) {
      ALL_SUPPORTED_LANGUAGES
    } else {
      val query = searchQuery.trim().lowercase()
      ALL_SUPPORTED_LANGUAGES.filter { lang ->
        lang.nativeName.lowercase().contains(query) ||
            lang.englishName.lowercase().contains(query) ||
            lang.countryName.lowercase().contains(query) ||
            lang.tag.lowercase().contains(query)
      }
    }
  }

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
    ) {
      // Header with Translate Icon
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = QuranIcons.Translate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(CoreR.string.lang_select_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = stringResource(CoreR.string.lang_select_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        IconButton(onClick = onDismissRequest) {
          Icon(
            imageVector = QuranIcons.Close,
            contentDescription = stringResource(CoreR.string.cancel),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
          Text(
            text = stringResource(CoreR.string.lang_search_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        },
        leadingIcon = {
          Icon(
            imageVector = QuranIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { searchQuery = "" }) {
              Icon(
                imageVector = QuranIcons.Close,
                contentDescription = null,
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
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Languages List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 440.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredLanguages, key = { it.tag }) { lang ->
          val isSelected = if (lang.tag.isEmpty()) {
            activeTag.isEmpty()
          } else {
            activeTag.startsWith(lang.tag)
          }

          ModernLanguageItem(
            language = lang,
            isSelected = isSelected,
            onClick = {
              val localeList = if (lang.tag.isEmpty()) {
                LocaleListCompat.getEmptyLocaleList()
              } else {
                LocaleListCompat.forLanguageTags(lang.tag)
              }
              AppCompatDelegate.setApplicationLocales(localeList)
              onDismissRequest()
            }
          )
        }

        item {
          Spacer(modifier = Modifier.height(24.dp))
        }
      }
    }
  }
}

@Composable
private fun ModernLanguageItem(
  language: SupportedLanguage,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val borderColor = if (isSelected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
  }

  val containerColor = if (isSelected) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
  }

  ModernCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    backgroundColor = containerColor,
    borderColor = borderColor,
    borderWidth = if (isSelected) 1.8.dp else 1.dp,
    onClick = onClick,
    contentPadding = 14.dp
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Flag / Icon Badge
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.size(42.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = language.flagEmoji,
            fontSize = 22.sp
          )
        }
      }

      Spacer(modifier = Modifier.width(14.dp))

      // Language Names
      Column(modifier = Modifier.weight(1f)) {
        val displayName = if (language.tag.isEmpty()) {
          stringResource(CoreR.string.lang_system_default)
        } else {
          language.nativeName
        }
        val displaySubtitle = if (language.tag.isEmpty()) {
          stringResource(CoreR.string.lang_select_subtitle)
        } else {
          "${language.englishName} • ${language.countryName}"
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurface
            }
          )

          if (language.isRtl) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
            ) {
              Text(
                text = stringResource(CoreR.string.lang_rtl_badge),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = displaySubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Selection Radio Button
      RadioButton(
        selected = isSelected,
        onClick = onClick,
        colors = RadioButtonDefaults.colors(
          selectedColor = MaterialTheme.colorScheme.primary,
          unselectedColor = MaterialTheme.colorScheme.outline
        )
      )
    }
  }
}
