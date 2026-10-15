package com.quran.labs.androidquran.common.audio.memorization

import android.content.Context
import android.content.SharedPreferences
import com.quran.data.di.AppScope
import com.quran.data.model.SuraAyah
import com.quran.mobile.di.qualifier.ApplicationContext
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

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

    private const val VALUE_DELAY_NONE = "NONE"
    private const val VALUE_DELAY_FIXED = "FIXED"
    private const val VALUE_DELAY_MATCH = "MATCH"

    private const val DELIMITER_ITEM = "|"
    private const val MAX_MASTERED_RANGES = 30
  }
}
