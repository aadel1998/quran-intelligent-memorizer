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
)

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
  val enableBridge: Boolean = true
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
      enableBridge = config.enableBridge
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
      playlistName = playlistName
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

  /**
   * Saves active memorization configuration and progress step specifically for a Surah.
   */
  fun saveSessionForSura(sura: Int, config: MemorizationConfig, currentStepIndex: Int, currentRepeatIteration: Int) {
    val prefix = "sura_${sura}_"
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
    return prefs.getBoolean("sura_${sura}_$KEY_HAS_ACTIVE_SESSION", false)
  }

  fun getSavedSessionForSura(sura: Int): SavedSuraSession? {
    if (!hasSavedSessionForSura(sura)) return null
    val prefix = "sura_${sura}_"
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
      enableBridge = prefs.getBoolean("${prefix}$KEY_ENABLE_BRIDGE", true)
    )

    return SavedSuraSession(
      config = config,
      stepIndex = stepIndex,
      repeatIteration = repeatIter,
      lastUpdated = timestamp
    )
  }

  fun clearSessionForSura(sura: Int) {
    prefs.edit().putBoolean("sura_${sura}_$KEY_HAS_ACTIVE_SESSION", false).apply()
  }

  fun saveLastUsedSettings(
    windowSize: Int,
    singleRepeat: Int,
    connectRepeat: Int,
    delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
    memoryTestMode: Boolean = false,
    maskingLevel: MaskingLevel = MaskingLevel.OFF,
    recordRecitation: Boolean = false,
    enableBridge: Boolean = true
  ) {
    val editor = prefs.edit()
      .putInt(KEY_DEFAULT_WINDOW_SIZE, windowSize)
      .putInt(KEY_DEFAULT_SINGLE_REPEAT, singleRepeat)
      .putInt(KEY_DEFAULT_CONNECT_REPEAT, connectRepeat)
      .putBoolean(KEY_DEFAULT_MEMORY_TEST, memoryTestMode)
      .putString(KEY_DEFAULT_MASKING_LEVEL, maskingLevel.name)
      .putBoolean(KEY_DEFAULT_RECORD_RECITATION, recordRecitation)
      .putBoolean(KEY_DEFAULT_ENABLE_BRIDGE, enableBridge)
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
        connectRepeat = 2
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
      enableBridge = prefs.getBoolean(KEY_DEFAULT_ENABLE_BRIDGE, true)
    )
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

    private const val KEY_DEFAULT_WINDOW_SIZE = "mem_def_window_size"
    private const val KEY_DEFAULT_SINGLE_REPEAT = "mem_def_single_repeat"
    private const val KEY_DEFAULT_CONNECT_REPEAT = "mem_def_connect_repeat"
    private const val KEY_DEFAULT_MEMORY_TEST = "mem_def_test_mode"
    private const val KEY_DEFAULT_MASKING_LEVEL = "mem_def_masking_level"
    private const val KEY_DEFAULT_RECORD_RECITATION = "mem_def_record_recitation"
    private const val KEY_DEFAULT_DELAY_MODE = "mem_def_delay_mode"
    private const val KEY_DEFAULT_DELAY_SECONDS = "mem_def_delay_seconds"
    private const val KEY_DEFAULT_ENABLE_BRIDGE = "mem_def_enable_bridge"
    private const val KEY_HAS_CUSTOM_DEFAULTS = "mem_has_custom_defaults"

    private const val VALUE_DELAY_NONE = "NONE"
    private const val VALUE_DELAY_FIXED = "FIXED"
    private const val VALUE_DELAY_MATCH = "MATCH"

    private const val DELIMITER_ITEM = "|"
    private const val MAX_MASTERED_RANGES = 30
  }
}
