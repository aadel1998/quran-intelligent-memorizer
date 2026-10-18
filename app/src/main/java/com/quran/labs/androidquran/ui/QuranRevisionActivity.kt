package com.quran.labs.androidquran.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.common.audio.memorization.MemorizationPreferences
import com.quran.labs.androidquran.common.audio.memorization.MemorizedSurahInfo
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.ui.helpers.QuranNavigator
import com.quran.labs.androidquran.ui.modern.ModernRevisionDashboardScreen
import com.quran.labs.androidquran.ui.modern.SuraItemModel
import com.quran.labs.androidquran.util.QuranSettings
import dev.zacsweers.metro.Inject
import com.quran.mobile.common.ui.core.R as UiCoreR

/**
 * Dedicated Activity hosting the Easy Quran Memorizer Pro - Revision Dashboard.
 */
class QuranRevisionActivity : AppCompatActivity() {

  @Inject lateinit var quranDisplayData: QuranDisplayData
  @Inject lateinit var quranInfo: QuranInfo
  @Inject lateinit var memorizationPreferences: MemorizationPreferences
  @Inject lateinit var quranSettings: QuranSettings
  @Inject lateinit var quranNavigatorFactory: QuranNavigator.Factory

  private val quranNavigator by lazy { quranNavigatorFactory.create(this) }

  private var memorizedInfosState by mutableStateOf<Map<Int, MemorizedSurahInfo>>(emptyMap())
  private var selectedQueueState by mutableStateOf<Set<Int>>(emptySet())
  private var dailyGoalAyahsState by mutableStateOf(5)
  private var ayahsTodayState by mutableStateOf(0)
  private var currentStreakState by mutableStateOf(0)
  private var bestStreakState by mutableStateOf(0)
  private var flaggedSurahsState by mutableStateOf<Set<Int>>(emptySet())
  private var totalFlaggedAyahsState by mutableIntStateOf(0)

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    (application as QuranApplication).applicationComponent.inject(this)

    refreshState()

    val wantTranslation = quranSettings.isShowSuraTranslatedName
    val wantPrefix = true
    val arabicSuraNames = resources.getStringArray(UiCoreR.array.sura_names_arabic)

    val surahs: List<SuraItemModel> = (1..Constants.SURAS_COUNT).map { sura ->
      SuraItemModel(
        number = sura,
        englishName = quranDisplayData.getSuraName(this, sura, wantPrefix, wantTranslation),
        arabicName = arabicSuraNames.getOrElse(sura - 1) { "" },
        isMakki = quranInfo.isMakki(sura),
        numberOfAyahs = quranInfo.getNumberOfAyahs(sura),
        startPage = quranInfo.getPageNumberForSura(sura),
        juzNumber = quranInfo.getJuzFromPage(quranInfo.getPageNumberForSura(sura))
      )
    }

    setContent {
      QuranTheme {
        ModernRevisionDashboardScreen(
          surahs = surahs,
          memorizedInfos = memorizedInfosState,
          selectedQueue = selectedQueueState,
          onToggleMemorized = { sura, isMemorized ->
            memorizationPreferences.setSurahMemorized(sura, isMemorized)
            refreshState()
          },
          onToggleQueueSelection = { sura ->
            val newQueue = if (selectedQueueState.contains(sura)) {
              selectedQueueState - sura
            } else {
              selectedQueueState + sura
            }
            selectedQueueState = newQueue
            memorizationPreferences.setRevisionQueue(newQueue.toList())
          },
          onSelectAllDue = {
            val dueSurahs = memorizedInfosState.filterValues { it.isDue }.keys
            val combined = selectedQueueState + dueSurahs
            selectedQueueState = combined
            memorizationPreferences.setRevisionQueue(combined.toList())
          },
          onClearQueue = {
            selectedQueueState = emptySet()
            memorizationPreferences.clearRevisionQueue()
          },
          onStartRevision = { suraList ->
            if (suraList.isNotEmpty()) {
              memorizationPreferences.setRevisionQueue(suraList)
              val totalAyahs = suraList.sumOf { surahs.getOrNull(it - 1)?.numberOfAyahs ?: 7 }
              memorizationPreferences.recordAyahsPracticed(totalAyahs)
              val firstSura = suraList.first()
              val intent = quranNavigator.intentFor(SuraAyah(firstSura, 1))
              intent.putExtra(PagerActivity.EXTRA_START_MEMORIZATION_SURA, firstSura)
              startActivity(intent)
            }
          },
          onQuizResult = { sura, passed ->
            memorizationPreferences.recordQuizResult(sura, passed)
            if (passed) {
              memorizationPreferences.recordAyahsPracticed(1)
            }
            refreshState()
          },
          dailyGoal = dailyGoalAyahsState,
          ayahsToday = ayahsTodayState,
          currentStreak = currentStreakState,
          bestStreak = bestStreakState,
          onUpdateDailyGoal = { goal ->
            memorizationPreferences.setDailyGoalAyahs(goal)
            refreshState()
          },
          flaggedSurahs = flaggedSurahsState,
          onPracticeFlagged = { sura ->
            val flaggedAyahs = memorizationPreferences.getFlaggedAyahsForSura(sura).sorted()
            if (flaggedAyahs.isNotEmpty()) {
              val firstAyah = flaggedAyahs.first()
              val lastAyah = flaggedAyahs.last()
              val intent = quranNavigator.intentFor(SuraAyah(sura, firstAyah))
              intent.putExtra(PagerActivity.EXTRA_START_MEMORIZATION_SURA, sura)
              intent.putExtra(PagerActivity.EXTRA_START_MEMORIZATION_START_AYAH, firstAyah)
              intent.putExtra(PagerActivity.EXTRA_START_MEMORIZATION_END_AYAH, lastAyah)
              startActivity(intent)
            }
          },
          onResetStreak = {
            memorizationPreferences.resetStreak()
            refreshState()
          },
          onClearAllFlagged = {
            memorizationPreferences.clearAllFlaggedAyahs()
            refreshState()
          },
          totalFlaggedAyahs = totalFlaggedAyahsState,
          onBackClick = { finish() }
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    refreshState()
  }

  private fun refreshState() {
    memorizedInfosState = memorizationPreferences.getAllMemorizedSurahInfo()
    selectedQueueState = memorizationPreferences.getRevisionQueue().toSet()
    dailyGoalAyahsState = memorizationPreferences.getDailyGoalAyahs()
    ayahsTodayState = memorizationPreferences.getAyahsMemorizedToday()
    currentStreakState = memorizationPreferences.getCurrentStreak()
    bestStreakState = memorizationPreferences.getBestStreak()
    flaggedSurahsState = memorizationPreferences.getAllFlaggedSurahs()
    totalFlaggedAyahsState = memorizationPreferences.getTotalFlaggedAyahsCount()
  }

  companion object {
    fun getIntent(context: Context): Intent = Intent(context, QuranRevisionActivity::class.java)
  }
}
