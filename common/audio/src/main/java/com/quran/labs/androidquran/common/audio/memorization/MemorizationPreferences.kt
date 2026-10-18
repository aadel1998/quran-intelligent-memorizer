package com.quran.labs.androidquran.common.audio.memorization

import android.content.Context
import android.content.SharedPreferences
import com.quran.data.di.AppScope
import com.quran.data.model.SuraAyah
import com.quran.mobile.di.qualifier.ApplicationContext
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Saved session progress for an individual Surah.
 */
data class SavedSuraSession(
  val config: MemorizationConfig,
  val stepIndex: Int,
  val repeatIteration: Int,
  val lastUpdated: Long = System.currentTimeMillis()
) {
  val sura: Int get() = config.startAyah.sura
}

/**
 * Information regarding a Surah's memorization state, revision history, and queue status.
 */
data class MemorizedSurahInfo(
  val sura: Int,
  val isMemorized: Boolean,
  val lastRevised: Long = 0L,
  val intervalDays: Int = 3,
  val isDue: Boolean = false,
  val inRevisionQueue: Boolean = false,
  val flaggedAyahsCount: Int = 0
)

/**
 * User profile for tracking independent memorization progress, streaks, and settings.
 * Supports family members, students, and multiple custom learning profiles (Easy Quran Memorizer Pro).
 */
data class MemorizationProfile(
  val id: String,
  val name: String,
  val createdAt: Long = System.currentTimeMillis()
)

/**
 * Memorization target plan with deadline and daily pace calculation.
 */
data class HifzPlan(
  val targetSura: Int,
  val targetSuraName: String,
  val totalVerses: Int,
  val targetDateEpoch: Long,
  val createdAt: Long = System.currentTimeMillis()
) {
  val daysRemaining: Int
    get() {
      val diff = targetDateEpoch - System.currentTimeMillis()
      return (diff / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(1)
    }

  fun dailyPaceRequired(versesCompleted: Int): Int {
    val versesLeft = (totalVerses - versesCompleted).coerceAtLeast(0)
    val days = daysRemaining
    return if (days > 0) ((versesLeft + days - 1) / days).coerceAtLeast(1) else versesLeft
  }
}

/**
 * Persisted default settings for memorization configuration.
 */
data class MemorizationDefaultSettings(
  val windowSize: Int = 7,
  val singleRepeat: Int = 3,
  val connectRepeat: Int = 2,
  val delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
  val memoryTestMode: Boolean = false,
  val maskingLevel: MaskingLevel = MaskingLevel.OFF,
  val recordRecitation: Boolean = false,
  val enableBridge: Boolean = true,
  val isIntelliJMode: Boolean = true,
  val rangeRepeat: Int = 1,
  val playbackSpeed: Float = 1.0f
)

/**
 * Handles persistent storage of memorization session progress, configuration,
 * custom playlist sessions, and mastered ranges history.
 */
@SingleIn(AppScope::class)
class MemorizationPreferences(
  private val prefs: SharedPreferences
) {

  @Inject
  constructor(@ApplicationContext context: Context) : this(
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  )

  val currentPrefix: String
    get() {
      val activeId = getActiveProfileId()
      return if (activeId == DEFAULT_PROFILE_ID) "" else "prof_${activeId}_"
    }

  // --- Multi-Profile Hifz Management (Easy Quran Memorizer Pro Feature) ---

  fun getProfiles(): List<MemorizationProfile> {
    val raw = prefs.getString(KEY_PROFILES_LIST, null)
    if (raw.isNullOrBlank()) {
      return listOf(MemorizationProfile(DEFAULT_PROFILE_ID, "Personal", 0L))
    }
    return raw.split("|").mapNotNull { entry ->
      val parts = entry.split(":")
      if (parts.size >= 2) {
        val id = parts[0]
        val name = parts[1]
        val created = parts.getOrNull(2)?.toLongOrNull() ?: 0L
        MemorizationProfile(id, name, created)
      } else null
    }.ifEmpty {
      listOf(MemorizationProfile(DEFAULT_PROFILE_ID, "Personal", 0L))
    }
  }

  fun getActiveProfileId(): String {
    return prefs.getString(KEY_ACTIVE_PROFILE_ID, DEFAULT_PROFILE_ID) ?: DEFAULT_PROFILE_ID
  }

  fun getActiveProfile(): MemorizationProfile {
    val activeId = getActiveProfileId()
    return getProfiles().find { it.id == activeId } ?: MemorizationProfile(DEFAULT_PROFILE_ID, "Personal", 0L)
  }

  fun setActiveProfile(profileId: String) {
    prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
  }

  fun createProfile(name: String): MemorizationProfile {
    val cleanName = name.trim().ifBlank { "Profile" }
    val id = "p_${System.currentTimeMillis()}"
    val newProfile = MemorizationProfile(id, cleanName, System.currentTimeMillis())
    val list = getProfiles().toMutableList()
    list.add(newProfile)
    saveProfilesList(list)
    return newProfile
  }

  fun deleteProfile(profileId: String) {
    if (profileId == DEFAULT_PROFILE_ID) return
    val list = getProfiles().filter { it.id != profileId }
    saveProfilesList(list)
    if (getActiveProfileId() == profileId) {
      setActiveProfile(DEFAULT_PROFILE_ID)
    }
  }

  fun renameProfile(profileId: String, newName: String) {
    val cleanName = newName.trim().ifBlank { return }
    val list = getProfiles().map {
      if (it.id == profileId) it.copy(name = cleanName) else it
    }
    saveProfilesList(list)
  }

  private fun saveProfilesList(list: List<MemorizationProfile>) {
    val str = list.joinToString("|") { "${it.id}:${it.name}:${it.createdAt}" }
    prefs.edit().putString(KEY_PROFILES_LIST, str).apply()
  }

  // --- Hifz Plan Management (Easy Quran Memorizer Pro Feature) ---

  fun getHifzPlan(): HifzPlan? {
    val active = prefs.getBoolean("${currentPrefix}$KEY_HIFZ_PLAN_ACTIVE", false)
    if (!active) return null
    val targetSura = prefs.getInt("${currentPrefix}$KEY_HIFZ_PLAN_TARGET_SURA", 0)
    val suraName = prefs.getString("${currentPrefix}$KEY_HIFZ_PLAN_SURA_NAME", "") ?: ""
    val totalVerses = prefs.getInt("${currentPrefix}$KEY_HIFZ_PLAN_TOTAL_VERSES", 0)
    val targetDate = prefs.getLong("${currentPrefix}$KEY_HIFZ_PLAN_TARGET_DATE", 0L)
    val createdAt = prefs.getLong("${currentPrefix}$KEY_HIFZ_PLAN_CREATED_AT", 0L)
    if (targetSura <= 0 || targetDate <= 0L) return null
    return HifzPlan(
      targetSura = targetSura,
      targetSuraName = suraName,
      totalVerses = totalVerses,
      targetDateEpoch = targetDate,
      createdAt = createdAt
    )
  }

  fun setHifzPlan(plan: HifzPlan) {
    prefs.edit()
      .putBoolean("${currentPrefix}$KEY_HIFZ_PLAN_ACTIVE", true)
      .putInt("${currentPrefix}$KEY_HIFZ_PLAN_TARGET_SURA", plan.targetSura)
      .putString("${currentPrefix}$KEY_HIFZ_PLAN_SURA_NAME", plan.targetSuraName)
      .putInt("${currentPrefix}$KEY_HIFZ_PLAN_TOTAL_VERSES", plan.totalVerses)
      .putLong("${currentPrefix}$KEY_HIFZ_PLAN_TARGET_DATE", plan.targetDateEpoch)
      .putLong("${currentPrefix}$KEY_HIFZ_PLAN_CREATED_AT", plan.createdAt)
      .apply()
  }

  fun clearHifzPlan() {
    prefs.edit()
      .remove("${currentPrefix}$KEY_HIFZ_PLAN_ACTIVE")
      .remove("${currentPrefix}$KEY_HIFZ_PLAN_TARGET_SURA")
      .remove("${currentPrefix}$KEY_HIFZ_PLAN_SURA_NAME")
      .remove("${currentPrefix}$KEY_HIFZ_PLAN_TOTAL_VERSES")
      .remove("${currentPrefix}$KEY_HIFZ_PLAN_TARGET_DATE")
      .remove("${currentPrefix}$KEY_HIFZ_PLAN_CREATED_AT")
      .apply()
  }

  // --- Daily Reminder Notification Settings (Easy Quran Memorizer Pro Feature) ---

  fun isDailyReminderEnabled(): Boolean = prefs.getBoolean(KEY_REMINDER_ENABLED, false)

  fun setDailyReminderEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply()
  }

  fun getDailyReminderTime(): Pair<Int, Int> =
    Pair(prefs.getInt(KEY_REMINDER_HOUR, 9), prefs.getInt(KEY_REMINDER_MINUTE, 0))

  fun setDailyReminderTime(hour: Int, minute: Int) {
    prefs.edit()
      .putInt(KEY_REMINDER_HOUR, hour.coerceIn(0, 23))
      .putInt(KEY_REMINDER_MINUTE, minute.coerceIn(0, 59))
      .apply()
  }

  /**
   * Saves active memorization configuration and progress step.
   */
  fun saveSession(config: MemorizationConfig, currentStepIndex: Int, currentRepeatIteration: Int) {
    saveSessionForSura(config.startAyah.sura, config, currentStepIndex, currentRepeatIteration)
    saveLastUsedSettings(
      windowSize = config.windowSize,
      singleRepeat = config.singleRepeat,
      connectRepeat = config.connectRepeat,
      delayMode = config.delayMode,
      memoryTestMode = config.memoryTestMode,
      maskingLevel = config.maskingLevel,
      recordRecitation = config.recordRecitation,
      enableBridge = config.enableBridge,
      isIntelliJMode = config.isIntelliJMode,
      rangeRepeat = config.rangeRepeat,
      playbackSpeed = config.playbackSpeed
    )
    val editor = prefs.edit()
      .putInt(KEY_START_SURA, config.startAyah.sura)
      .putInt(KEY_START_AYAH, config.startAyah.ayah)
      .putInt(KEY_END_SURA, config.endAyah.sura)
      .putInt(KEY_END_AYAH, config.endAyah.ayah)
      .putInt(KEY_WINDOW_SIZE, config.windowSize)
      .putInt(KEY_SINGLE_REPEAT, config.singleRepeat)
      .putInt(KEY_CONNECT_REPEAT, config.connectRepeat)
      .putBoolean(KEY_MEMORY_TEST, config.memoryTestMode)
      .putString(KEY_MASKING_LEVEL, config.maskingLevel.name)
      .putBoolean(KEY_RECORD_RECITATION, config.recordRecitation)
      .putBoolean(KEY_ENABLE_BRIDGE, config.enableBridge)
      .putBoolean(KEY_INTELLIJ_MODE, config.isIntelliJMode)
      .putInt(KEY_RANGE_REPEAT, config.rangeRepeat)
      .putFloat(KEY_PLAYBACK_SPEED, config.playbackSpeed)
      .putInt(KEY_SAVED_STEP_INDEX, currentStepIndex)
      .putInt(KEY_SAVED_REPEAT, currentRepeatIteration)
      .putBoolean(KEY_HAS_ACTIVE_SESSION, true)

    if (config.segments.isNotEmpty()) {
      editor.putString(KEY_SEGMENTS, config.segments.joinToString(",") { it.serialize() })
      editor.putString(KEY_PLAYLIST_ID, config.playlistId)
      editor.putString(KEY_PLAYLIST_NAME, config.playlistName)
    } else {
      editor.remove(KEY_SEGMENTS)
      editor.remove(KEY_PLAYLIST_ID)
      editor.remove(KEY_PLAYLIST_NAME)
    }

    when (val mode = config.delayMode) {
      is MemorizationDelayMode.None -> {
        editor.putString(KEY_DELAY_MODE, VALUE_DELAY_NONE)
      }
      is MemorizationDelayMode.FixedSeconds -> {
        editor.putString(KEY_DELAY_MODE, VALUE_DELAY_FIXED)
        editor.putInt(KEY_DELAY_SECONDS, mode.seconds)
      }
      is MemorizationDelayMode.MatchAyahDuration -> {
        editor.putString(KEY_DELAY_MODE, VALUE_DELAY_MATCH)
      }
    }

    editor.apply()
  }

  /**
   * Clears saved session on completion or manual termination.
   */
  fun clearSession() {
    prefs.edit().putBoolean(KEY_HAS_ACTIVE_SESSION, false).apply()
  }

  /**
   * Returns true if there is an unfinished memorization session saved.
   */
  fun hasSavedSession(): Boolean = prefs.getBoolean(KEY_HAS_ACTIVE_SESSION, false)

  /**
   * Retrieves the saved session's [MemorizationConfig], or null if none exists.
   */
  fun getSavedConfig(): MemorizationConfig? {
    if (!hasSavedSession()) return null
    val startSura = prefs.getInt(KEY_START_SURA, 1)
    val startAyah = prefs.getInt(KEY_START_AYAH, 1)
    val endSura = prefs.getInt(KEY_END_SURA, 1)
    val endAyah = prefs.getInt(KEY_END_AYAH, 1)

    val delayModeStr = prefs.getString(KEY_DELAY_MODE, VALUE_DELAY_NONE)
    val delayMode = when (delayModeStr) {
      VALUE_DELAY_FIXED -> MemorizationDelayMode.FixedSeconds(prefs.getInt(KEY_DELAY_SECONDS, 3))
      VALUE_DELAY_MATCH -> MemorizationDelayMode.MatchAyahDuration
      else -> MemorizationDelayMode.None
    }

    val memoryTest = prefs.getBoolean(KEY_MEMORY_TEST, false)
    val maskingLevelStr = prefs.getString(KEY_MASKING_LEVEL, null)
    val maskingLevel = if (maskingLevelStr != null) {
      try {
        MaskingLevel.valueOf(maskingLevelStr)
      } catch (e: Exception) {
        if (memoryTest) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF
      }
    } else {
      if (memoryTest) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF
    }

    val recordRecitation = prefs.getBoolean(KEY_RECORD_RECITATION, false)

    val segmentsStr = prefs.getString(KEY_SEGMENTS, null)
    val segments = if (!segmentsStr.isNullOrBlank()) {
      segmentsStr.split(",").mapNotNull { PlaylistSegment.deserialize(it) }
    } else {
      emptyList()
    }
    val playlistId = prefs.getString(KEY_PLAYLIST_ID, null)
    val playlistName = prefs.getString(KEY_PLAYLIST_NAME, null)

    return MemorizationConfig(
      startAyah = SuraAyah(startSura, startAyah),
      endAyah = SuraAyah(endSura, endAyah),
      windowSize = prefs.getInt(KEY_WINDOW_SIZE, 4),
      singleRepeat = prefs.getInt(KEY_SINGLE_REPEAT, 3),
      connectRepeat = prefs.getInt(KEY_CONNECT_REPEAT, 2),
      delayMode = delayMode,
      memoryTestMode = memoryTest,
      maskingLevel = maskingLevel,
      recordRecitation = recordRecitation,
      enableBridge = prefs.getBoolean(KEY_ENABLE_BRIDGE, true),
      segments = segments,
      playlistId = playlistId,
      playlistName = playlistName,
      isIntelliJMode = prefs.getBoolean(KEY_INTELLIJ_MODE, true),
      rangeRepeat = prefs.getInt(KEY_RANGE_REPEAT, 1),
      playbackSpeed = prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f)
    )
  }

  fun getSavedStepIndex(): Int = prefs.getInt(KEY_SAVED_STEP_INDEX, 0)

  fun getSavedRepeatIteration(): Int = prefs.getInt(KEY_SAVED_REPEAT, 1)

  /**
   * Logs a completed memorization range into mastery history.
   * Keeps most recent ranges first, limited to [MAX_MASTERED_RANGES].
   */
  fun saveMasteredRange(range: MasteredRange) {
    val existing = getMasteredRanges().toMutableList()
    existing.removeAll { it.startAyah == range.startAyah && it.endAyah == range.endAyah }
    existing.add(0, range)
    val limited = existing.take(MAX_MASTERED_RANGES)
    val serialized = limited.joinToString(DELIMITER_ITEM) { it.serialize() }
    prefs.edit().putString(KEY_MASTERED_RANGES, serialized).apply()
  }

  /**
   * Returns list of mastered ranges in descending order of completion.
   */
  fun getMasteredRanges(): List<MasteredRange> {
    val raw = prefs.getString(KEY_MASTERED_RANGES, null) ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw.split(DELIMITER_ITEM).mapNotNull { MasteredRange.deserialize(it) }
  }

  /**
   * Clears the mastered ranges history.
   */
  fun clearMasteredRanges() {
    prefs.edit().remove(KEY_MASTERED_RANGES).apply()
  }

  // --- Revision & Memorization Tracking (Easy Quran Memorizer Pro Feature) ---

  /**
   * Sets whether an entire Surah has been marked as fully memorized.
   */
  fun setSurahMemorized(sura: Int, memorized: Boolean) {
    val editor = prefs.edit().putBoolean("${currentPrefix}sura_memorized_$sura", memorized)
    if (memorized) {
      if (getLastRevised(sura) == 0L) {
        editor.putLong("${currentPrefix}sura_revised_$sura", System.currentTimeMillis())
      }
    }
    editor.apply()
  }

  fun isSurahMemorized(sura: Int): Boolean {
    return prefs.getBoolean("${currentPrefix}sura_memorized_$sura", false)
  }

  fun getMemorizedSurahs(): Set<Int> {
    return (1..114).filter { isSurahMemorized(it) }.toSet()
  }

  fun setLastRevised(sura: Int, timestamp: Long = System.currentTimeMillis()) {
    prefs.edit().putLong("${currentPrefix}sura_revised_$sura", timestamp).apply()
  }

  fun getLastRevised(sura: Int): Long {
    return prefs.getLong("${currentPrefix}sura_revised_$sura", 0L)
  }

  fun setRevisionIntervalDays(sura: Int, days: Int) {
    prefs.edit().putInt("${currentPrefix}sura_rev_interval_$sura", days).apply()
  }

  fun getRevisionIntervalDays(sura: Int): Int {
    return prefs.getInt("${currentPrefix}sura_rev_interval_$sura", 3)
  }

  fun isSurahDueForRevision(sura: Int): Boolean {
    if (!isSurahMemorized(sura)) return false
    val last = getLastRevised(sura)
    if (last == 0L) return true
    val intervalMillis = getRevisionIntervalDays(sura).toLong() * 24 * 60 * 60 * 1000L
    return (System.currentTimeMillis() - last) >= intervalMillis
  }

  fun getSurahsDueForRevision(): List<Int> {
    return getMemorizedSurahs().filter { isSurahDueForRevision(it) }.sorted()
  }

  fun getRevisionQueue(): Set<Int> {
    val raw = prefs.getString("${currentPrefix}$KEY_REVISION_QUEUE", null) ?: return emptySet()
    if (raw.isBlank()) return emptySet()
    return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
  }

  fun setRevisionQueue(surahs: Collection<Int>) {
    prefs.edit().putString("${currentPrefix}$KEY_REVISION_QUEUE", surahs.toSet().sorted().joinToString(",")).apply()
  }

  /**
   * Records self-testing recall quiz results with Spaced Repetition (SRS) interval adaptation.
   */
  fun recordQuizResult(sura: Int, passed: Boolean) {
    if (passed) {
      setLastRevised(sura, System.currentTimeMillis())
      val currentInterval = getRevisionIntervalDays(sura)
      // Exponential SRS expansion: 3d -> 6d -> 12d -> 24d (capped at 60d)
      val nextInterval = (currentInterval * 2).coerceIn(3, 60)
      setRevisionIntervalDays(sura, nextInterval)
      removeFromRevisionQueue(sura)
    } else {
      // Failed recall reset interval to 1 day and add to revision queue
      setRevisionIntervalDays(sura, 1)
      addToRevisionQueue(sura)
    }
  }

  fun addToRevisionQueue(sura: Int) {
    val current = getRevisionQueue().toMutableSet()
    current.add(sura)
    setRevisionQueue(current)
  }

  fun removeFromRevisionQueue(sura: Int) {
    val current = getRevisionQueue().toMutableSet()
    current.remove(sura)
    setRevisionQueue(current)
  }

  fun clearRevisionQueue() {
    prefs.edit().remove("${currentPrefix}$KEY_REVISION_QUEUE").apply()
  }

  fun getMemorizedSurahInfo(sura: Int): MemorizedSurahInfo {
    val isMem = isSurahMemorized(sura)
    return MemorizedSurahInfo(
      sura = sura,
      isMemorized = isMem,
      lastRevised = getLastRevised(sura),
      intervalDays = getRevisionIntervalDays(sura),
      isDue = isSurahDueForRevision(sura),
      inRevisionQueue = getRevisionQueue().contains(sura),
      flaggedAyahsCount = getFlaggedAyahsForSura(sura).size
    )
  }

  fun getAllMemorizedSurahInfos(): List<MemorizedSurahInfo> {
    val queue = getRevisionQueue()
    return (1..114).map { sura ->
      val isMem = isSurahMemorized(sura)
      MemorizedSurahInfo(
        sura = sura,
        isMemorized = isMem,
        lastRevised = getLastRevised(sura),
        intervalDays = getRevisionIntervalDays(sura),
        isDue = isSurahDueForRevision(sura),
        inRevisionQueue = queue.contains(sura),
        flaggedAyahsCount = getFlaggedAyahsForSura(sura).size
      )
    }
  }

  fun getAllMemorizedSurahInfo(): Map<Int, MemorizedSurahInfo> {
    return getAllMemorizedSurahInfos().associateBy { it.sura }
  }

  // --- Daily Goal & Streak Tracking (Easy Quran Memorizer Pro) ---

  fun getDailyGoalAyahs(): Int = prefs.getInt("${currentPrefix}$KEY_DAILY_GOAL_AYAHS", 5)

  fun setDailyGoalAyahs(goal: Int) {
    prefs.edit().putInt("${currentPrefix}$KEY_DAILY_GOAL_AYAHS", goal.coerceIn(1, 100)).apply()
  }

  fun getAyahsMemorizedToday(): Int {
    val currentEpochDay = (System.currentTimeMillis() / 86_400_000L).toInt()
    val lastEpochDay = prefs.getInt("${currentPrefix}$KEY_LAST_ACTIVE_EPOCH_DAY", 0)
    return if (lastEpochDay == currentEpochDay) {
      prefs.getInt("${currentPrefix}$KEY_AYAHS_MEMORIZED_TODAY", 0)
    } else {
      0
    }
  }

  fun getCurrentStreak(): Int {
    val currentEpochDay = (System.currentTimeMillis() / 86_400_000L).toInt()
    val lastEpochDay = prefs.getInt("${currentPrefix}$KEY_LAST_ACTIVE_EPOCH_DAY", 0)
    return when {
      lastEpochDay == currentEpochDay || lastEpochDay == currentEpochDay - 1 -> prefs.getInt("${currentPrefix}$KEY_CURRENT_STREAK", 0)
      else -> 0
    }
  }

  fun getBestStreak(): Int = prefs.getInt("${currentPrefix}$KEY_BEST_STREAK", 0)

  fun recordAyahsPracticed(count: Int) {
    if (count <= 0) return
    val currentEpochDay = (System.currentTimeMillis() / 86_400_000L).toInt()
    val lastEpochDay = prefs.getInt("${currentPrefix}$KEY_LAST_ACTIVE_EPOCH_DAY", 0)
    val editor = prefs.edit()
    val newStreak = when {
      lastEpochDay == currentEpochDay -> prefs.getInt("${currentPrefix}$KEY_CURRENT_STREAK", 1)
      lastEpochDay == currentEpochDay - 1 -> prefs.getInt("${currentPrefix}$KEY_CURRENT_STREAK", 0) + 1
      else -> 1
    }
    val bestStreak = maxOf(newStreak, prefs.getInt("${currentPrefix}$KEY_BEST_STREAK", 0))
    val todayAyahs = if (lastEpochDay == currentEpochDay) {
      prefs.getInt("${currentPrefix}$KEY_AYAHS_MEMORIZED_TODAY", 0) + count
    } else {
      count
    }

    editor.putInt("${currentPrefix}$KEY_LAST_ACTIVE_EPOCH_DAY", currentEpochDay)
      .putInt("${currentPrefix}$KEY_CURRENT_STREAK", newStreak)
      .putInt("${currentPrefix}$KEY_BEST_STREAK", bestStreak)
      .putInt("${currentPrefix}$KEY_AYAHS_MEMORIZED_TODAY", todayAyahs)
      .apply()
  }

  // --- Difficult / Flagged Ayahs (Mistake Tracker) ---

  fun toggleAyahFlagged(sura: Int, ayah: Int): Boolean {
    val current = getFlaggedAyahsForSura(sura).toMutableSet()
    val isNowFlagged = if (current.contains(ayah)) {
      current.remove(ayah)
      false
    } else {
      current.add(ayah)
      true
    }
    setFlaggedAyahsForSura(sura, current)
    return isNowFlagged
  }

  fun isAyahFlagged(sura: Int, ayah: Int): Boolean {
    return getFlaggedAyahsForSura(sura).contains(ayah)
  }

  fun getFlaggedAyahsForSura(sura: Int): Set<Int> {
    val raw = prefs.getString("${currentPrefix}${KEY_FLAGGED_AYAHS_PREFIX}$sura", null) ?: return emptySet()
    return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
  }

  fun setFlaggedAyahsForSura(sura: Int, ayahs: Set<Int>) {
    if (ayahs.isEmpty()) {
      prefs.edit().remove("${currentPrefix}${KEY_FLAGGED_AYAHS_PREFIX}$sura").apply()
    } else {
      prefs.edit().putString("${currentPrefix}${KEY_FLAGGED_AYAHS_PREFIX}$sura", ayahs.joinToString(",")).apply()
    }
  }

  fun getAllFlaggedSurahs(): Set<Int> {
    return (1..114).filter { getFlaggedAyahsForSura(it).isNotEmpty() }.toSet()
  }

  fun getTotalFlaggedAyahsCount(): Int {
    return (1..114).sumOf { getFlaggedAyahsForSura(it).size }
  }

  fun clearAllFlaggedAyahs() {
    val editor = prefs.edit()
    (1..114).forEach { sura ->
      editor.remove("${currentPrefix}${KEY_FLAGGED_AYAHS_PREFIX}$sura")
    }
    editor.apply()
  }

  fun resetStreak() {
    prefs.edit()
      .putInt("${currentPrefix}$KEY_CURRENT_STREAK", 0)
      .putInt("${currentPrefix}$KEY_AYAHS_MEMORIZED_TODAY", 0)
      .remove("${currentPrefix}$KEY_LAST_ACTIVE_EPOCH_DAY")
      .apply()
  }


  /**
   * Saves active memorization configuration and progress step specifically for a Surah.
   */
  fun saveSessionForSura(sura: Int, config: MemorizationConfig, currentStepIndex: Int, currentRepeatIteration: Int) {
    val prefix = "${currentPrefix}sura_${sura}_"
    val editor = prefs.edit()
      .putInt("${prefix}$KEY_START_SURA", config.startAyah.sura)
      .putInt("${prefix}$KEY_START_AYAH", config.startAyah.ayah)
      .putInt("${prefix}$KEY_END_SURA", config.endAyah.sura)
      .putInt("${prefix}$KEY_END_AYAH", config.endAyah.ayah)
      .putInt("${prefix}$KEY_WINDOW_SIZE", config.windowSize)
      .putInt("${prefix}$KEY_SINGLE_REPEAT", config.singleRepeat)
      .putInt("${prefix}$KEY_CONNECT_REPEAT", config.connectRepeat)
      .putBoolean("${prefix}$KEY_MEMORY_TEST", config.memoryTestMode)
      .putString("${prefix}$KEY_MASKING_LEVEL", config.maskingLevel.name)
      .putBoolean("${prefix}$KEY_RECORD_RECITATION", config.recordRecitation)
      .putBoolean("${prefix}$KEY_ENABLE_BRIDGE", config.enableBridge)
      .putBoolean("${prefix}$KEY_INTELLIJ_MODE", config.isIntelliJMode)
      .putInt("${prefix}$KEY_RANGE_REPEAT", config.rangeRepeat)
      .putFloat("${prefix}$KEY_PLAYBACK_SPEED", config.playbackSpeed)
      .putInt("${prefix}$KEY_SAVED_STEP_INDEX", currentStepIndex)
      .putInt("${prefix}$KEY_SAVED_REPEAT", currentRepeatIteration)
      .putBoolean("${prefix}$KEY_HAS_ACTIVE_SESSION", true)
      .putLong("${prefix}timestamp", System.currentTimeMillis())

    when (val mode = config.delayMode) {
      is MemorizationDelayMode.None -> {
        editor.putString("${prefix}$KEY_DELAY_MODE", VALUE_DELAY_NONE)
      }
      is MemorizationDelayMode.FixedSeconds -> {
        editor.putString("${prefix}$KEY_DELAY_MODE", VALUE_DELAY_FIXED)
        editor.putInt("${prefix}$KEY_DELAY_SECONDS", mode.seconds)
      }
      is MemorizationDelayMode.MatchAyahDuration -> {
        editor.putString("${prefix}$KEY_DELAY_MODE", VALUE_DELAY_MATCH)
      }
    }
    editor.apply()
  }

  fun hasSavedSessionForSura(sura: Int): Boolean {
    return prefs.getBoolean("${currentPrefix}sura_${sura}_$KEY_HAS_ACTIVE_SESSION", false)
  }

  fun getSavedSessionForSura(sura: Int): SavedSuraSession? {
    if (!hasSavedSessionForSura(sura)) return null
    val prefix = "${currentPrefix}sura_${sura}_"
    val startSura = prefs.getInt("${prefix}$KEY_START_SURA", sura)
    val startAyah = prefs.getInt("${prefix}$KEY_START_AYAH", 1)
    val endSura = prefs.getInt("${prefix}$KEY_END_SURA", sura)
    val endAyah = prefs.getInt("${prefix}$KEY_END_AYAH", 1)

    val delayModeStr = prefs.getString("${prefix}$KEY_DELAY_MODE", VALUE_DELAY_NONE)
    val delayMode = when (delayModeStr) {
      VALUE_DELAY_FIXED -> MemorizationDelayMode.FixedSeconds(prefs.getInt("${prefix}$KEY_DELAY_SECONDS", 3))
      VALUE_DELAY_MATCH -> MemorizationDelayMode.MatchAyahDuration
      else -> MemorizationDelayMode.None
    }

    val memoryTest = prefs.getBoolean("${prefix}$KEY_MEMORY_TEST", false)
    val maskingLevelStr = prefs.getString("${prefix}$KEY_MASKING_LEVEL", null)
    val maskingLevel = if (maskingLevelStr != null) {
      try {
        MaskingLevel.valueOf(maskingLevelStr)
      } catch (e: Exception) {
        if (memoryTest) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF
      }
    } else {
      if (memoryTest) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF
    }

    val recordRecitation = prefs.getBoolean("${prefix}$KEY_RECORD_RECITATION", false)
    val isIntelliJ = prefs.getBoolean("${prefix}$KEY_INTELLIJ_MODE", true)
    val rangeRepeat = prefs.getInt("${prefix}$KEY_RANGE_REPEAT", 1)
    val playbackSpeed = prefs.getFloat("${prefix}$KEY_PLAYBACK_SPEED", 1.0f)
    val stepIndex = prefs.getInt("${prefix}$KEY_SAVED_STEP_INDEX", 0)
    val repeatIter = prefs.getInt("${prefix}$KEY_SAVED_REPEAT", 1)
    val timestamp = prefs.getLong("${prefix}timestamp", System.currentTimeMillis())

    val config = MemorizationConfig(
      startAyah = SuraAyah(startSura, startAyah),
      endAyah = SuraAyah(endSura, endAyah),
      windowSize = prefs.getInt("${prefix}$KEY_WINDOW_SIZE", 7),
      singleRepeat = prefs.getInt("${prefix}$KEY_SINGLE_REPEAT", 3),
      connectRepeat = prefs.getInt("${prefix}$KEY_CONNECT_REPEAT", 2),
      delayMode = delayMode,
      memoryTestMode = memoryTest,
      maskingLevel = maskingLevel,
      recordRecitation = recordRecitation,
      enableBridge = prefs.getBoolean("${prefix}$KEY_ENABLE_BRIDGE", true),
      isIntelliJMode = isIntelliJ,
      rangeRepeat = rangeRepeat,
      playbackSpeed = playbackSpeed
    )

    return SavedSuraSession(
      config = config,
      stepIndex = stepIndex,
      repeatIteration = repeatIter,
      lastUpdated = timestamp
    )
  }

  fun clearSessionForSura(sura: Int) {
    prefs.edit().putBoolean("${currentPrefix}sura_${sura}_$KEY_HAS_ACTIVE_SESSION", false).apply()
  }

  /**
   * Returns all active saved Surah memorization sessions sorted by most recently updated.
   */
  fun getAllSavedSurahSessions(): List<SavedSuraSession> {
    return (1..114).mapNotNull { sura ->
      getSavedSessionForSura(sura)
    }.sortedByDescending { it.lastUpdated }
  }

  fun saveLastUsedSettings(
    windowSize: Int,
    singleRepeat: Int,
    connectRepeat: Int,
    delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
    memoryTestMode: Boolean = false,
    maskingLevel: MaskingLevel = MaskingLevel.OFF,
    recordRecitation: Boolean = false,
    enableBridge: Boolean = true,
    isIntelliJMode: Boolean = true,
    rangeRepeat: Int = 1,
    playbackSpeed: Float = 1.0f
  ) {
    val editor = prefs.edit()
      .putInt(KEY_DEFAULT_WINDOW_SIZE, windowSize)
      .putInt(KEY_DEFAULT_SINGLE_REPEAT, singleRepeat)
      .putInt(KEY_DEFAULT_CONNECT_REPEAT, connectRepeat)
      .putBoolean(KEY_DEFAULT_MEMORY_TEST, memoryTestMode)
      .putString(KEY_DEFAULT_MASKING_LEVEL, maskingLevel.name)
      .putBoolean(KEY_DEFAULT_RECORD_RECITATION, recordRecitation)
      .putBoolean(KEY_DEFAULT_ENABLE_BRIDGE, enableBridge)
      .putBoolean(KEY_DEFAULT_INTELLIJ_MODE, isIntelliJMode)
      .putInt(KEY_DEFAULT_RANGE_REPEAT, rangeRepeat)
      .putFloat(KEY_DEFAULT_PLAYBACK_SPEED, playbackSpeed)
      .putBoolean(KEY_HAS_CUSTOM_DEFAULTS, true)

    when (delayMode) {
      is MemorizationDelayMode.None -> {
        editor.putString(KEY_DEFAULT_DELAY_MODE, VALUE_DELAY_NONE)
      }
      is MemorizationDelayMode.FixedSeconds -> {
        editor.putString(KEY_DEFAULT_DELAY_MODE, VALUE_DELAY_FIXED)
        editor.putInt(KEY_DEFAULT_DELAY_SECONDS, delayMode.seconds)
      }
      is MemorizationDelayMode.MatchAyahDuration -> {
        editor.putString(KEY_DEFAULT_DELAY_MODE, VALUE_DELAY_MATCH)
      }
    }
    editor.apply()
  }

  fun getLastUsedSettings(): MemorizationDefaultSettings {
    val hasCustom = prefs.getBoolean(KEY_HAS_CUSTOM_DEFAULTS, false)
    if (!hasCustom) {
      return MemorizationDefaultSettings(
        windowSize = 7,
        singleRepeat = 3,
        connectRepeat = 2,
        isIntelliJMode = true,
        rangeRepeat = 1,
        playbackSpeed = 1.0f
      )
    }

    val delayModeStr = prefs.getString(KEY_DEFAULT_DELAY_MODE, VALUE_DELAY_NONE)
    val delayMode = when (delayModeStr) {
      VALUE_DELAY_FIXED -> MemorizationDelayMode.FixedSeconds(prefs.getInt(KEY_DEFAULT_DELAY_SECONDS, 3))
      VALUE_DELAY_MATCH -> MemorizationDelayMode.MatchAyahDuration
      else -> MemorizationDelayMode.None
    }

    val memoryTest = prefs.getBoolean(KEY_DEFAULT_MEMORY_TEST, false)
    val maskingLevelStr = prefs.getString(KEY_DEFAULT_MASKING_LEVEL, null)
    val maskingLevel = if (maskingLevelStr != null) {
      try {
        MaskingLevel.valueOf(maskingLevelStr)
      } catch (e: Exception) {
        if (memoryTest) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF
      }
    } else {
      if (memoryTest) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF
    }

    return MemorizationDefaultSettings(
      windowSize = prefs.getInt(KEY_DEFAULT_WINDOW_SIZE, 7),
      singleRepeat = prefs.getInt(KEY_DEFAULT_SINGLE_REPEAT, 3),
      connectRepeat = prefs.getInt(KEY_DEFAULT_CONNECT_REPEAT, 2),
      delayMode = delayMode,
      memoryTestMode = memoryTest,
      maskingLevel = maskingLevel,
      recordRecitation = prefs.getBoolean(KEY_DEFAULT_RECORD_RECITATION, false),
      enableBridge = prefs.getBoolean(KEY_DEFAULT_ENABLE_BRIDGE, true),
      isIntelliJMode = prefs.getBoolean(KEY_DEFAULT_INTELLIJ_MODE, true),
      rangeRepeat = prefs.getInt(KEY_DEFAULT_RANGE_REPEAT, 1),
      playbackSpeed = prefs.getFloat(KEY_DEFAULT_PLAYBACK_SPEED, 1.0f)
    )
  }

  // --- Backup & Restore (Easy Quran Memorizer Pro Feature) ---

  fun exportHifzBackupJson(): String {
    val root = org.json.JSONObject()
    root.put("version", 1)
    root.put("exportTimestamp", System.currentTimeMillis())
    root.put("activeProfileId", getActiveProfileId())

    // Profiles
    val profilesArray = org.json.JSONArray()
    getProfiles().forEach { profile ->
      val pObj = org.json.JSONObject()
      pObj.put("id", profile.id)
      pObj.put("name", profile.name)
      pObj.put("createdAt", profile.createdAt)
      profilesArray.put(pObj)
    }
    root.put("profiles", profilesArray)

    // Current profile data
    val currentData = org.json.JSONObject()
    val memorizedArray = org.json.JSONArray()
    for (sura in 1..114) {
      if (isSurahMemorized(sura)) {
        val sObj = org.json.JSONObject()
        sObj.put("sura", sura)
        sObj.put("lastRevised", getLastRevised(sura))
        sObj.put("intervalDays", getRevisionIntervalDays(sura))
        memorizedArray.put(sObj)
      }
    }
    currentData.put("memorizedSurahs", memorizedArray)

    // Flagged ayahs
    val flaggedObj = org.json.JSONObject()
    for (sura in 1..114) {
      val flagged = getFlaggedAyahsForSura(sura)
      if (flagged.isNotEmpty()) {
        val aArray = org.json.JSONArray()
        flagged.forEach { aArray.put(it) }
        flaggedObj.put(sura.toString(), aArray)
      }
    }
    currentData.put("flaggedAyahs", flaggedObj)

    // Streak & goals
    val streakObj = org.json.JSONObject()
    streakObj.put("currentStreak", getCurrentStreak())
    streakObj.put("bestStreak", getBestStreak())
    streakObj.put("dailyGoal", getDailyGoalAyahs())
    streakObj.put("ayahsToday", getAyahsMemorizedToday())
    currentData.put("streaks", streakObj)

    // Plan
    val plan = getHifzPlan()
    if (plan != null) {
      val planObj = org.json.JSONObject()
      planObj.put("targetSura", plan.targetSura)
      planObj.put("targetSuraName", plan.targetSuraName)
      planObj.put("totalVerses", plan.totalVerses)
      planObj.put("targetDateEpoch", plan.targetDateEpoch)
      planObj.put("createdAt", plan.createdAt)
      currentData.put("hifzPlan", planObj)
    }

    root.put("currentProfileData", currentData)
    return root.toString(2)
  }

  fun importHifzBackupJson(jsonString: String): Boolean {
    return try {
      val root = org.json.JSONObject(jsonString)
      // Check profiles
      if (root.has("profiles")) {
        val pArray = root.getJSONArray("profiles")
        val importedProfiles = mutableListOf<MemorizationProfile>()
        for (i in 0 until pArray.length()) {
          val pObj = pArray.getJSONObject(i)
          importedProfiles.add(
            MemorizationProfile(
              id = pObj.getString("id"),
              name = pObj.getString("name"),
              createdAt = pObj.optLong("createdAt", System.currentTimeMillis())
            )
          )
        }
        if (importedProfiles.isNotEmpty()) {
          saveProfilesList(importedProfiles)
        }
      }

      val activeId = root.optString("activeProfileId", null)
      if (!activeId.isNullOrBlank()) {
        setActiveProfile(activeId)
      }

      val data = root.optJSONObject("currentProfileData") ?: root
      if (data.has("memorizedSurahs")) {
        val memArray = data.getJSONArray("memorizedSurahs")
        for (i in 0 until memArray.length()) {
          val sObj = memArray.getJSONObject(i)
          val sura = sObj.getInt("sura")
          setSurahMemorized(sura, true)
          val lastRev = sObj.optLong("lastRevised", 0L)
          if (lastRev > 0L) {
            setLastRevised(sura, lastRev)
          }
          val interval = sObj.optInt("intervalDays", 3)
          setRevisionIntervalDays(sura, interval)
        }
      }

      if (data.has("flaggedAyahs")) {
        val flaggedObj = data.getJSONObject("flaggedAyahs")
        val keys = flaggedObj.keys()
        while (keys.hasNext()) {
          val suraKey = keys.next()
          val sura = suraKey.toIntOrNull() ?: continue
          val aArray = flaggedObj.getJSONArray(suraKey)
          val ayahsSet = mutableSetOf<Int>()
          for (j in 0 until aArray.length()) {
            ayahsSet.add(aArray.getInt(j))
          }
          setFlaggedAyahsForSura(sura, ayahsSet)
        }
      }

      if (data.has("streaks")) {
        val streakObj = data.getJSONObject("streaks")
        val curr = streakObj.optInt("currentStreak", -1)
        val best = streakObj.optInt("bestStreak", -1)
        if (curr >= 0) prefs.edit().putInt("${currentPrefix}$KEY_CURRENT_STREAK", curr).apply()
        if (best >= 0) prefs.edit().putInt("${currentPrefix}$KEY_BEST_STREAK", best).apply()
        val goal = streakObj.optInt("dailyGoal", -1)
        if (goal > 0) setDailyGoalAyahs(goal)
      }

      if (data.has("hifzPlan")) {
        val planObj = data.getJSONObject("hifzPlan")
        setHifzPlan(
          HifzPlan(
            targetSura = planObj.getInt("targetSura"),
            targetSuraName = planObj.getString("targetSuraName"),
            totalVerses = planObj.getInt("totalVerses"),
            targetDateEpoch = planObj.getLong("targetDateEpoch"),
            createdAt = planObj.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }
      true
    } catch (e: Exception) {
      false
    }
  }

  companion object {
    private const val PREFS_NAME = "quran_intelligent_memorizer_prefs"
    private const val KEY_START_SURA = "mem_start_sura"
    private const val KEY_START_AYAH = "mem_start_ayah"
    private const val KEY_END_SURA = "mem_end_sura"
    private const val KEY_END_AYAH = "mem_end_ayah"
    private const val KEY_WINDOW_SIZE = "mem_window_size"
    private const val KEY_SINGLE_REPEAT = "mem_single_repeat"
    private const val KEY_CONNECT_REPEAT = "mem_connect_repeat"
    private const val KEY_MEMORY_TEST = "mem_test_mode"
    private const val KEY_MASKING_LEVEL = "mem_masking_level"
    private const val KEY_RECORD_RECITATION = "mem_record_recitation"
    private const val KEY_DELAY_MODE = "mem_delay_mode"
    private const val KEY_DELAY_SECONDS = "mem_delay_seconds"
    private const val KEY_ENABLE_BRIDGE = "mem_enable_bridge"
    private const val KEY_SAVED_STEP_INDEX = "mem_step_idx"
    private const val KEY_SAVED_REPEAT = "mem_repeat_iter"
    private const val KEY_HAS_ACTIVE_SESSION = "mem_has_session"
    private const val KEY_MASTERED_RANGES = "mem_mastered_ranges"
    private const val KEY_SEGMENTS = "mem_segments"
    private const val KEY_PLAYLIST_ID = "mem_playlist_id"
    private const val KEY_PLAYLIST_NAME = "mem_playlist_name"
    private const val KEY_REVISION_QUEUE = "mem_revision_queue"
    private const val KEY_DAILY_GOAL_AYAHS = "mem_daily_goal_ayahs"
    private const val KEY_AYAHS_MEMORIZED_TODAY = "mem_ayahs_today"
    private const val KEY_CURRENT_STREAK = "mem_current_streak"
    private const val KEY_BEST_STREAK = "mem_best_streak"
    private const val KEY_LAST_ACTIVE_EPOCH_DAY = "mem_last_active_epoch_day"
    private const val KEY_FLAGGED_AYAHS_PREFIX = "mem_flagged_sura_"
    const val DEFAULT_PROFILE_ID = "default"
    private const val KEY_PROFILES_LIST = "mem_profiles_list"
    private const val KEY_ACTIVE_PROFILE_ID = "mem_active_profile_id"
    private const val KEY_HIFZ_PLAN_TARGET_SURA = "mem_hifz_plan_sura"
    private const val KEY_HIFZ_PLAN_SURA_NAME = "mem_hifz_plan_name"
    private const val KEY_HIFZ_PLAN_TOTAL_VERSES = "mem_hifz_plan_total"
    private const val KEY_HIFZ_PLAN_TARGET_DATE = "mem_hifz_plan_date"
    private const val KEY_HIFZ_PLAN_CREATED_AT = "mem_hifz_plan_created"
    private const val KEY_HIFZ_PLAN_ACTIVE = "mem_hifz_plan_active"
    private const val KEY_REMINDER_ENABLED = "mem_reminder_enabled"
    private const val KEY_REMINDER_HOUR = "mem_reminder_hour"
    private const val KEY_REMINDER_MINUTE = "mem_reminder_minute"

    private const val KEY_DEFAULT_WINDOW_SIZE = "mem_def_window_size"
    private const val KEY_DEFAULT_SINGLE_REPEAT = "mem_def_single_repeat"
    private const val KEY_DEFAULT_CONNECT_REPEAT = "mem_def_connect_repeat"
    private const val KEY_DEFAULT_MEMORY_TEST = "mem_def_test_mode"
    private const val KEY_DEFAULT_MASKING_LEVEL = "mem_def_masking_level"
    private const val KEY_DEFAULT_RECORD_RECITATION = "mem_def_record_recitation"
    private const val KEY_DEFAULT_DELAY_MODE = "mem_def_delay_mode"
    private const val KEY_DEFAULT_DELAY_SECONDS = "mem_def_delay_seconds"
    private const val KEY_DEFAULT_ENABLE_BRIDGE = "mem_def_enable_bridge"
    private const val KEY_DEFAULT_INTELLIJ_MODE = "mem_def_intellij_mode"
    private const val KEY_DEFAULT_RANGE_REPEAT = "mem_def_range_repeat"
    private const val KEY_DEFAULT_PLAYBACK_SPEED = "mem_def_playback_speed"
    private const val KEY_INTELLIJ_MODE = "mem_intellij_mode"
    private const val KEY_RANGE_REPEAT = "mem_range_repeat"
    private const val KEY_PLAYBACK_SPEED = "mem_playback_speed"
    private const val KEY_HAS_CUSTOM_DEFAULTS = "mem_has_custom_defaults"

    private const val VALUE_DELAY_NONE = "NONE"
    private const val VALUE_DELAY_FIXED = "FIXED"
    private const val VALUE_DELAY_MATCH = "MATCH"

    private const val DELIMITER_ITEM = "|"
    private const val MAX_MASTERED_RANGES = 30
  }
}
