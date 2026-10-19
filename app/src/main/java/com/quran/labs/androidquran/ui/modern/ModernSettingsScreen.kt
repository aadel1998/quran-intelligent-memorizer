package com.quran.labs.androidquran.ui.modern

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.quran.mobile.common.ui.core.R as CoreR
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.quran.labs.androidquran.AboutUsActivity
import com.quran.labs.androidquran.HelpActivity
import com.quran.labs.androidquran.QuranAdvancedPreferenceActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.components.ModernBadgeChip
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.pageselect.PageSelectActivity
import com.quran.labs.androidquran.ui.TranslationManagerActivity
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.util.ThemeUtil
import com.quran.mobile.feature.downloadmanager.AudioManagerActivity
import kotlin.math.roundToInt

private val EmeraldAccent = Color(0xFF10B981)
private val CardBg = Color(0xFF18181B)
private val CardBorder = Color(0xFF27272A)
private val TextMuted = Color(0xFFA1A1AA)

@Composable
fun ModernSettingsScreen(
  onNavigateBack: () -> Unit,
  onRestartSettings: () -> Unit
) {
  val context = LocalContext.current
  val quranSettings = remember { QuranSettings.getInstance(context) }
  val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

  // State
  var useModernUi by remember { mutableStateOf(quranSettings.isModernUiEnabled) }
  var appTheme by remember { mutableStateOf(quranSettings.currentTheme()) }
  var showLanguagePicker by remember { mutableStateOf(false) }
  var nightMode by remember { mutableStateOf(quranSettings.isNightMode) }
  var nightTextBrightness by remember {
    mutableFloatStateOf(quranSettings.nightModeTextBrightness.toFloat())
  }
  var nightBgBrightness by remember {
    mutableFloatStateOf(quranSettings.nightModeBackgroundBrightness.toFloat())
  }
  var lockOrientation by remember { mutableStateOf(quranSettings.isLockOrientation) }
  var landscapeOrientation by remember { mutableStateOf(quranSettings.isLandscapeOrientation) }
  var suraTranslatedName by remember { mutableStateOf(quranSettings.isShowSuraTranslatedName) }
  var overlayPageInfo by remember { mutableStateOf(quranSettings.shouldOverlayPageInfo()) }
  var markerPopup by remember { mutableStateOf(quranSettings.shouldDisplayMarkerPopup()) }
  var highlightBookmarks by remember { mutableStateOf(quranSettings.shouldHighlightBookmarks()) }
  var volumeKeyNav by remember { mutableStateOf(quranSettings.navigateWithVolumeKeys()) }
  var dualPageEnabled by remember {
    mutableStateOf(prefs.getBoolean(Constants.PREF_DUAL_PAGE_ENABLED, false))
  }
  var splitPageTranslation by remember { mutableStateOf(quranSettings.isQuranSplitWithTranslation) }
  var ayahBeforeTranslation by remember { mutableStateOf(quranSettings.wantArabicInTranslationView()) }
  var dyslexicFont by remember { mutableStateOf(quranSettings.wantDyslexicFontInTranslationView()) }
  var ayahTextSize by remember { mutableFloatStateOf(quranSettings.ayahTextSize.toFloat()) }
  var translationTextSize by remember {
    mutableFloatStateOf(quranSettings.translationTextSize.toFloat())
  }
  var preferStreaming by remember { mutableStateOf(quranSettings.shouldStream()) }
  var downloadAmount by remember {
    mutableStateOf(prefs.getString(Constants.PREF_DOWNLOAD_AMOUNT, "1") ?: "1")
  }

  // Dialog visibility
  var showThemeDialog by remember { mutableStateOf(false) }
  var showDownloadAmountDialog by remember { mutableStateOf(false) }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Navigation Bar
      ModernSettingsTopBar(
        onNavigateBack = onNavigateBack
      )

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 8.dp,
          bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Section 1: Appearance & Interface
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.prefs_category_display_settings),
            icon = QuranIcons.DarkMode
          ) {
            // Modern UI Toggle (Core Coexistence Switch)
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_use_modern_ui_title),
              subtitle = stringResource(R.string.prefs_use_modern_ui_summary),
              checked = useModernUi,
              onCheckedChange = { checked ->
                useModernUi = checked
                quranSettings.isModernUiEnabled = checked
                onRestartSettings()
              }
            )

            ModernSettingsDivider()

            // App Theme Selector
            val themeTitles = stringArrayResource(R.array.themes)
            val themeLabel = when (appTheme) {
              Constants.THEME_LIGHT -> themeTitles.getOrElse(0) { "Light" }
              Constants.THEME_DARK -> themeTitles.getOrElse(1) { "Dark" }
              else -> themeTitles.getOrElse(2) { "Automatic" }
            }
            ModernSettingsActionItem(
              title = stringResource(R.string.appearance),
              subtitle = themeLabel,
              valuePill = themeLabel,
              onClick = { showThemeDialog = true }
            )

            ModernSettingsDivider()

            // App Language Selector
            val currentAppLocales = remember { AppCompatDelegate.getApplicationLocales() }
            val currentLangTag = if (currentAppLocales.isEmpty) "" else (currentAppLocales.get(0)?.language ?: "")
            val activeLang = ALL_SUPPORTED_LANGUAGES.find { it.tag.isNotEmpty() && currentLangTag.startsWith(it.tag) }
              ?: ALL_SUPPORTED_LANGUAGES.first()
            val languageLabel = if (activeLang.tag.isEmpty()) {
              "${activeLang.flagEmoji} ${stringResource(CoreR.string.lang_system_default)}"
            } else {
              "${activeLang.flagEmoji} ${activeLang.nativeName}"
            }
            val languageSubtitle = if (activeLang.tag.isEmpty()) {
              stringResource(CoreR.string.lang_select_subtitle)
            } else {
              "${activeLang.englishName} • ${activeLang.countryName}"
            }

            ModernSettingsActionItem(
              title = stringResource(CoreR.string.lang_select_title),
              subtitle = languageSubtitle,
              valuePill = languageLabel,
              onClick = { showLanguagePicker = true }
            )

            ModernSettingsDivider()

            // Translated Surah Name Toggle
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_sura_translated_name_title),
              subtitle = stringResource(R.string.prefs_sura_translated_name_summary),
              checked = suraTranslatedName,
              onCheckedChange = { checked ->
                suraTranslatedName = checked
                prefs.edit().putBoolean(Constants.PREF_SURA_TRANSLATED_NAME, checked).apply()
              }
            )

            ModernSettingsDivider()

            // Inverted Night Mode Toggle
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_night_mode_title),
              subtitle = stringResource(R.string.prefs_night_mode_summary),
              checked = nightMode,
              onCheckedChange = { checked ->
                nightMode = checked
                prefs.edit().putBoolean(Constants.PREF_NIGHT_MODE, checked).apply()
              }
            )

            AnimatedVisibility(
              visible = nightMode,
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut()
            ) {
              Column {
                ModernSettingsDivider()
                ModernSettingsSliderItem(
                  title = stringResource(R.string.prefs_night_mode_text_brightness_title),
                  value = nightTextBrightness,
                  range = 0f..255f,
                  valueLabel = "${nightTextBrightness.roundToInt()}",
                  onValueChange = { nightTextBrightness = it },
                  onValueChangeFinished = {
                    prefs.edit().putInt(
                      Constants.PREF_NIGHT_MODE_TEXT_BRIGHTNESS,
                      nightTextBrightness.roundToInt()
                    ).apply()
                  }
                )

                ModernSettingsDivider()
                ModernSettingsSliderItem(
                  title = stringResource(R.string.prefs_night_mode_background_brightness_title),
                  value = nightBgBrightness,
                  range = 0f..64f,
                  valueLabel = "${nightBgBrightness.roundToInt()}",
                  onValueChange = { nightBgBrightness = it },
                  onValueChangeFinished = {
                    prefs.edit().putInt(
                      Constants.PREF_NIGHT_MODE_BACKGROUND_BRIGHTNESS,
                      nightBgBrightness.roundToInt()
                    ).apply()
                  }
                )
              }
            }

            ModernSettingsDivider()

            // Lock Orientation Toggle
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_lock_orientation_title),
              subtitle = if (lockOrientation) {
                stringResource(R.string.prefs_lock_orientation_summary_on)
              } else {
                stringResource(R.string.prefs_lock_orientation_summary_off)
              },
              checked = lockOrientation,
              onCheckedChange = { checked ->
                lockOrientation = checked
                prefs.edit().putBoolean(Constants.PREF_LOCK_ORIENTATION, checked).apply()
              }
            )

            AnimatedVisibility(
              visible = lockOrientation,
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut()
            ) {
              Column {
                ModernSettingsDivider()
                ModernSettingsSwitchItem(
                  title = stringResource(R.string.prefs_landscape_orientation_title),
                  subtitle = if (landscapeOrientation) {
                    stringResource(R.string.prefs_landscape_orientation_summary_on)
                  } else {
                    stringResource(R.string.prefs_landscape_orientation_summary_off)
                  },
                  checked = landscapeOrientation,
                  onCheckedChange = { checked ->
                    landscapeOrientation = checked
                    prefs.edit().putBoolean(Constants.PREF_LANDSCAPE_ORIENTATION, checked).apply()
                  }
                )
              }
            }
          }
        }

        // Section 2: Reading Experience
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.prefs_category_reading),
            icon = QuranIcons.MenuBook
          ) {
            // Page Type / Mushaf Selection
            ModernSettingsActionItem(
              title = stringResource(R.string.prefs_page_type_title),
              subtitle = stringResource(R.string.prefs_page_type_summary),
              valuePill = quranSettings.pageType ?: "Default",
              onClick = {
                context.startActivity(Intent(context, PageSelectActivity::class.java))
              }
            )

            ModernSettingsDivider()

            // Overlay Page Info
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_overlay_page_info_title),
              subtitle = stringResource(R.string.prefs_overlay_page_info_summary),
              checked = overlayPageInfo,
              onCheckedChange = { checked ->
                overlayPageInfo = checked
                quranSettings.setShouldOverlayPageInfo(checked)
              }
            )

            ModernSettingsDivider()

            // Marker Popups
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_display_marker_title),
              subtitle = stringResource(R.string.prefs_display_marker_summary),
              checked = markerPopup,
              onCheckedChange = { checked ->
                markerPopup = checked
                prefs.edit().putBoolean(Constants.PREF_DISPLAY_MARKER_POPUP, checked).apply()
              }
            )

            ModernSettingsDivider()

            // Highlight Bookmarks
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_highlight_bookmarks_title),
              subtitle = stringResource(R.string.prefs_highlight_bookmarks_summary),
              checked = highlightBookmarks,
              onCheckedChange = { checked ->
                highlightBookmarks = checked
                prefs.edit().putBoolean(Constants.PREF_HIGHLIGHT_BOOKMARKS, checked).apply()
              }
            )

            ModernSettingsDivider()

            // Volume Key Navigation
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_volume_key_navigation_title),
              subtitle = stringResource(R.string.prefs_volume_key_navigation_summary),
              checked = volumeKeyNav,
              onCheckedChange = { checked ->
                volumeKeyNav = checked
                prefs.edit().putBoolean(Constants.PREF_USE_VOLUME_KEY_NAV, checked).apply()
              }
            )
          }
        }

        // Section 3: Dual Page & Tablet
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.prefs_category_dual_screen),
            icon = QuranIcons.MenuBook
          ) {
            // Dual Page Mode
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_dual_page_mode_title),
              subtitle = if (dualPageEnabled) {
                stringResource(R.string.prefs_dual_page_mode_enabled)
              } else {
                stringResource(R.string.prefs_dual_page_mode_disabled)
              },
              checked = dualPageEnabled,
              onCheckedChange = { checked ->
                dualPageEnabled = checked
                prefs.edit().putBoolean(Constants.PREF_DUAL_PAGE_ENABLED, checked).apply()
              }
            )

            AnimatedVisibility(
              visible = dualPageEnabled,
              enter = expandVertically() + fadeIn(),
              exit = shrinkVertically() + fadeOut()
            ) {
              Column {
                ModernSettingsDivider()
                ModernSettingsSwitchItem(
                  title = stringResource(R.string.prefs_split_page_and_translation_title),
                  subtitle = stringResource(R.string.prefs_split_page_and_translation_summary),
                  checked = splitPageTranslation,
                  onCheckedChange = { checked ->
                    splitPageTranslation = checked
                    prefs.edit().putBoolean(Constants.PREF_SPLIT_PAGE_AND_TRANSLATION, checked).apply()
                  }
                )
              }
            }
          }
        }

        // Section 4: Translation & Tafsir
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.prefs_category_translation),
            icon = QuranIcons.Translate
          ) {
            // Download Translations
            ModernSettingsActionItem(
              title = stringResource(R.string.prefs_translations),
              subtitle = stringResource(R.string.prefs_translations_summary),
              valuePill = null,
              onClick = {
                context.startActivity(Intent(context, TranslationManagerActivity::class.java))
              }
            )

            ModernSettingsDivider()

            // Ayah Before Translation
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_ayah_before_translation_title),
              subtitle = stringResource(R.string.prefs_ayah_before_translation_summary),
              checked = ayahBeforeTranslation,
              onCheckedChange = { checked ->
                ayahBeforeTranslation = checked
                prefs.edit().putBoolean(Constants.PREF_AYAH_BEFORE_TRANSLATION, checked).apply()
              }
            )

            ModernSettingsDivider()

            // Dyslexic Font
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_use_dyslexic_font_title),
              subtitle = stringResource(R.string.prefs_use_dyslexic_font_summary),
              checked = dyslexicFont,
              onCheckedChange = { checked ->
                dyslexicFont = checked
                prefs.edit().putBoolean(Constants.PREF_USE_DYSLEXIC_FONT, checked).apply()
              }
            )

            ModernSettingsDivider()

            // Ayah Text Size
            ModernSettingsSliderItem(
              title = stringResource(R.string.prefs_ayah_text_title),
              value = ayahTextSize,
              range = 15f..40f,
              valueLabel = "${ayahTextSize.roundToInt()} sp",
              onValueChange = { ayahTextSize = it },
              onValueChangeFinished = {
                quranSettings.ayahTextSize = ayahTextSize.roundToInt()
              }
            )

            ModernSettingsDivider()

            // Translation Text Size
            ModernSettingsSliderItem(
              title = stringResource(R.string.prefs_translation_text_title),
              value = translationTextSize,
              range = 15f..40f,
              valueLabel = "${translationTextSize.roundToInt()} sp",
              onValueChange = { translationTextSize = it },
              onValueChangeFinished = {
                prefs.edit().putInt(
                  Constants.PREF_TRANSLATION_TEXT_SIZE,
                  translationTextSize.roundToInt()
                ).apply()
              }
            )
          }
        }

        // Section 5: Audio & Recitations
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.prefs_category_download),
            icon = QuranIcons.PlayArrow
          ) {
            // Audio Manager
            ModernSettingsActionItem(
              title = stringResource(R.string.prefs_audio_manager),
              subtitle = stringResource(R.string.prefs_audio_manager_summary),
              valuePill = null,
              onClick = {
                context.startActivity(Intent(context, AudioManagerActivity::class.java))
              }
            )

            ModernSettingsDivider()

            // Streaming Preference
            ModernSettingsSwitchItem(
              title = stringResource(R.string.prefs_streaming_title),
              subtitle = stringResource(R.string.prefs_streaming_summary),
              checked = preferStreaming,
              onCheckedChange = { checked ->
                preferStreaming = checked
                prefs.edit().putBoolean(Constants.PREF_PREFER_STREAMING, checked).apply()
              }
            )

            ModernSettingsDivider()

            // Download Batch Amount
            val downloadLabel = when (downloadAmount) {
              "2" -> stringResource(R.string.quran_sura)
              "3" -> stringResource(R.string.quran_juz2)
              else -> stringResource(R.string.quran_page)
            }
            ModernSettingsActionItem(
              title = stringResource(R.string.prefs_download_amount_title),
              subtitle = stringResource(R.string.prefs_download_amount_summary),
              valuePill = downloadLabel,
              onClick = { showDownloadAmountDialog = true }
            )
          }
        }

        // Section 6: Advanced & Storage
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.prefs_category_advanced),
            icon = QuranIcons.Settings
          ) {
            ModernSettingsActionItem(
              title = stringResource(R.string.prefs_category_advanced),
              subtitle = stringResource(R.string.prefs_category_advanced_summary),
              valuePill = null,
              onClick = {
                context.startActivity(Intent(context, QuranAdvancedPreferenceActivity::class.java))
              }
            )
          }
        }

        // Section 7: About & Support
        item {
          ModernSettingsGroup(
            title = stringResource(R.string.menu_about),
            icon = QuranIcons.MenuBook
          ) {
            ModernSettingsActionItem(
              title = stringResource(R.string.menu_help),
              subtitle = stringResource(R.string.help_title),
              valuePill = null,
              onClick = {
                context.startActivity(Intent(context, HelpActivity::class.java))
              }
            )
            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 16.dp),
              thickness = 0.5.dp,
              color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
            ModernSettingsActionItem(
              title = stringResource(R.string.menu_about),
              subtitle = stringResource(R.string.about_description),
              valuePill = null,
              onClick = {
                context.startActivity(Intent(context, AboutUsActivity::class.java))
              }
            )
          }
        }
      }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
      val themeTitles = stringArrayResource(R.array.themes)
      val themeOptions = listOf(
        Constants.THEME_DEFAULT to themeTitles.getOrElse(2) { "Automatic" },
        Constants.THEME_DARK to themeTitles.getOrElse(1) { "Dark" },
        Constants.THEME_LIGHT to themeTitles.getOrElse(0) { "Light" }
      )

      AlertDialog(
        onDismissRequest = { showThemeDialog = false },
        title = {
          Text(
            text = stringResource(R.string.appearance),
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        },
        text = {
          Column {
            themeOptions.forEach { (value, title) ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .selectable(
                    selected = (appTheme == value),
                    onClick = {
                      appTheme = value
                      prefs.edit().putString(Constants.PREF_APP_THEME, value).apply()
                      ThemeUtil.setTheme(value)
                      showThemeDialog = false
                    },
                    role = Role.RadioButton
                  )
                  .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = (appTheme == value),
                  onClick = null,
                  colors = RadioButtonDefaults.colors(
                    selectedColor = EmeraldAccent,
                    unselectedColor = TextMuted
                  )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = title,
                  color = if (appTheme == value) EmeraldAccent else Color.White,
                  fontWeight = if (appTheme == value) FontWeight.SemiBold else FontWeight.Normal
                )
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showThemeDialog = false }) {
            Text(stringResource(android.R.string.cancel), color = EmeraldAccent)
          }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(20.dp)
      )
    }

    // Download Amount Selection Dialog
    if (showDownloadAmountDialog) {
      val amountValues = arrayOf("1", "2", "3")
      val amountTitles = arrayOf(
        stringResource(R.string.quran_page),
        stringResource(R.string.quran_sura),
        stringResource(R.string.quran_juz2)
      )

      AlertDialog(
        onDismissRequest = { showDownloadAmountDialog = false },
        title = {
          Text(
            text = stringResource(R.string.prefs_download_amount_title),
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        },
        text = {
          Column {
            amountValues.forEachIndexed { index, value ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .selectable(
                    selected = (downloadAmount == value),
                    onClick = {
                      downloadAmount = value
                      prefs.edit().putString(Constants.PREF_DOWNLOAD_AMOUNT, value).apply()
                      showDownloadAmountDialog = false
                    },
                    role = Role.RadioButton
                  )
                  .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = (downloadAmount == value),
                  onClick = null,
                  colors = RadioButtonDefaults.colors(
                    selectedColor = EmeraldAccent,
                    unselectedColor = TextMuted
                  )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = amountTitles[index],
                  color = if (downloadAmount == value) EmeraldAccent else Color.White,
                  fontWeight = if (downloadAmount == value) FontWeight.SemiBold else FontWeight.Normal
                )
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showDownloadAmountDialog = false }) {
            Text(stringResource(android.R.string.cancel), color = EmeraldAccent)
          }
        },
        containerColor = CardBg,
        shape = RoundedCornerShape(20.dp)
      )
    }

    // In-App Language Picker Sheet
    if (showLanguagePicker) {
      ModernLanguagePickerSheet(
        onDismissRequest = { showLanguagePicker = false }
      )
    }
  }
}

@Composable
private fun ModernSettingsTopBar(
  onNavigateBack: () -> Unit
) {
  val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.background)
      .padding(top = statusBarPadding, start = 12.dp, end = 16.dp, bottom = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CardBg)
        ) {
          Icon(
            imageVector = QuranIcons.ArrowBack,
            contentDescription = stringResource(R.string.reader_back),
            tint = Color.White
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
          text = stringResource(R.string.menu_settings),
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }

      // Material 3 Badge
      ModernBadgeChip(
        text = "Material 3",
        leadingDotColor = EmeraldAccent
      )
    }
  }
}

@Composable
private fun ModernSettingsGroup(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    // Group Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 4.dp, bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(EmeraldAccent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = EmeraldAccent,
          modifier = Modifier.size(14.dp)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = EmeraldAccent
      )
    }

    // Card Container
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(20.dp),
      color = CardBg,
      border = BorderStroke(0.5.dp, CardBorder)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth()
      ) {
        content()
      }
    }
  }
}

@Composable
private fun ModernSettingsSwitchItem(
  title: String,
  subtitle: String?,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) }
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 16.dp)
    ) {
      Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
      )
      if (!subtitle.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = TextMuted,
          lineHeight = 16.sp
        )
      }
    }

    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = EmeraldAccent,
        uncheckedThumbColor = Color(0xFF71717A),
        uncheckedTrackColor = Color(0xFF27272A),
        uncheckedBorderColor = Color(0xFF3F3F46)
      )
    )
  }
}

@Composable
private fun ModernSettingsActionItem(
  title: String,
  subtitle: String?,
  valuePill: String?,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .padding(end = 12.dp)
    ) {
      Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
      )
      if (!subtitle.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          fontSize = 12.sp,
          color = TextMuted,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      if (valuePill != null) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF27272A)
        ) {
          Text(
            text = valuePill,
            fontSize = 12.sp,
            color = EmeraldAccent,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
      }

      Icon(
        imageVector = QuranIcons.ChevronRight,
        contentDescription = null,
        tint = TextMuted,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun ModernSettingsSliderItem(
  title: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  valueLabel: String,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White
      )
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = EmeraldAccent.copy(alpha = 0.15f)
      ) {
        Text(
          text = valueLabel,
          fontSize = 12.sp,
          color = EmeraldAccent,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }

    Slider(
      value = value,
      onValueChange = onValueChange,
      onValueChangeFinished = onValueChangeFinished,
      valueRange = range,
      colors = SliderDefaults.colors(
        thumbColor = EmeraldAccent,
        activeTrackColor = EmeraldAccent,
        inactiveTrackColor = Color(0xFF27272A)
      ),
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
private fun ModernSettingsDivider() {
  HorizontalDivider(
    thickness = 0.5.dp,
    color = CardBorder
  )
}
