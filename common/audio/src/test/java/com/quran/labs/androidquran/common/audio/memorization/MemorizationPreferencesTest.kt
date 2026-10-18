package com.quran.labs.androidquran.common.audio.memorization

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.quran.data.model.SuraAyah
import org.junit.Before
import org.junit.Test

class MemorizationPreferencesTest {

  private lateinit var fakePrefs: FakeSharedPreferences
  private lateinit var memorizationPreferences: MemorizationPreferences

  @Before
  fun setUp() {
    fakePrefs = FakeSharedPreferences()
    memorizationPreferences = MemorizationPreferences(fakePrefs)
  }

  @Test
  fun `initial state has no active session and empty history`() {
    assertThat(memorizationPreferences.hasSavedSession()).isFalse()
    assertThat(memorizationPreferences.getSavedConfig()).isNull()
    assertThat(memorizationPreferences.getMasteredRanges()).isEmpty()
  }

  @Test
  fun `saveSession persists configuration and progress`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(2, 255),
      endAyah = SuraAyah(2, 257),
      windowSize = 3,
      singleRepeat = 5,
      connectRepeat = 4,
      delayMode = MemorizationDelayMode.FixedSeconds(3),
      memoryTestMode = true,
      maskingLevel = MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT,
      recordRecitation = true,
      enableBridge = false
    )

    memorizationPreferences.saveSession(config, currentStepIndex = 4, currentRepeatIteration = 2)

    assertThat(memorizationPreferences.hasSavedSession()).isTrue()
    val savedConfig = memorizationPreferences.getSavedConfig()
    assertThat(savedConfig).isNotNull()
    assertThat(savedConfig?.startAyah).isEqualTo(SuraAyah(2, 255))
    assertThat(savedConfig?.endAyah).isEqualTo(SuraAyah(2, 257))
    assertThat(savedConfig?.windowSize).isEqualTo(3)
    assertThat(savedConfig?.singleRepeat).isEqualTo(5)
    assertThat(savedConfig?.connectRepeat).isEqualTo(4)
    assertThat(savedConfig?.delayMode).isEqualTo(MemorizationDelayMode.FixedSeconds(3))
    assertThat(savedConfig?.memoryTestMode).isTrue()
    assertThat(savedConfig?.maskingLevel).isEqualTo(MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT)
    assertThat(savedConfig?.recordRecitation).isTrue()
    assertThat(savedConfig?.enableBridge).isFalse()

    assertThat(memorizationPreferences.getSavedStepIndex()).isEqualTo(4)
    assertThat(memorizationPreferences.getSavedRepeatIteration()).isEqualTo(2)
  }

  @Test
  fun `saveSession persists match duration delay mode correctly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 7),
      delayMode = MemorizationDelayMode.MatchAyahDuration
    )

    memorizationPreferences.saveSession(config, 0, 1)
    val saved = memorizationPreferences.getSavedConfig()
    assertThat(saved?.delayMode).isEqualTo(MemorizationDelayMode.MatchAyahDuration)
  }

  @Test
  fun `clearSession removes active session flag`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 7)
    )
    memorizationPreferences.saveSession(config, 0, 1)
    assertThat(memorizationPreferences.hasSavedSession()).isTrue()

    memorizationPreferences.clearSession()

    assertThat(memorizationPreferences.hasSavedSession()).isFalse()
    assertThat(memorizationPreferences.getSavedConfig()).isNull()
  }

  @Test
  fun `mastered range serialization and deserialization works`() {
    val range = MasteredRange(
      startAyah = SuraAyah(2, 255),
      endAyah = SuraAyah(2, 257),
      timestamp = 1729000000000L,
      repetitions = 7
    )

    val serialized = range.serialize()
    assertThat(serialized).isEqualTo("2:255-2:257@1729000000000#7")

    val deserialized = MasteredRange.deserialize(serialized)
    assertThat(deserialized).isEqualTo(range)
  }

  @Test
  fun `saveMasteredRange adds to history in descending order with deduplication`() {
    val r1 = MasteredRange(SuraAyah(1, 1), SuraAyah(1, 7), 1000L, 5)
    val r2 = MasteredRange(SuraAyah(2, 1), SuraAyah(2, 5), 2000L, 6)
    val r3 = MasteredRange(SuraAyah(1, 1), SuraAyah(1, 7), 3000L, 8) // Re-memorized range 1

    memorizationPreferences.saveMasteredRange(r1)
    memorizationPreferences.saveMasteredRange(r2)

    val history = memorizationPreferences.getMasteredRanges()
    assertThat(history).hasSize(2)
    assertThat(history[0]).isEqualTo(r2)
    assertThat(history[1]).isEqualTo(r1)

    // Re-memorizing r1 brings it to the top and updates repetitions/timestamp
    memorizationPreferences.saveMasteredRange(r3)
    val updatedHistory = memorizationPreferences.getMasteredRanges()
    assertThat(updatedHistory).hasSize(2)
    assertThat(updatedHistory[0]).isEqualTo(r3)
    assertThat(updatedHistory[1]).isEqualTo(r2)
  }

  @Test
  fun `clearMasteredRanges empties history`() {
    val r1 = MasteredRange(SuraAyah(1, 1), SuraAyah(1, 7), 1000L, 5)
    memorizationPreferences.saveMasteredRange(r1)
    assertThat(memorizationPreferences.getMasteredRanges()).isNotEmpty()

    memorizationPreferences.clearMasteredRanges()
    assertThat(memorizationPreferences.getMasteredRanges()).isEmpty()
  }

  @Test
  fun `per-surah session is persisted and can be cleared independently`() {
    val configBaqarah = MemorizationConfig(
      startAyah = SuraAyah(2, 1),
      endAyah = SuraAyah(2, 286),
      windowSize = 7,
      singleRepeat = 3,
      connectRepeat = 2
    )

    memorizationPreferences.saveSession(configBaqarah, currentStepIndex = 12, currentRepeatIteration = 2)

    assertThat(memorizationPreferences.hasSavedSessionForSura(2)).isTrue()
    val suraSession = memorizationPreferences.getSavedSessionForSura(2)
    assertThat(suraSession).isNotNull()
    assertThat(suraSession?.stepIndex).isEqualTo(12)
    assertThat(suraSession?.config?.startAyah).isEqualTo(SuraAyah(2, 1))
    assertThat(suraSession?.config?.endAyah).isEqualTo(SuraAyah(2, 286))
    assertThat(suraSession?.config?.windowSize).isEqualTo(7)

    // Another surah shouldn't have active session
    assertThat(memorizationPreferences.hasSavedSessionForSura(3)).isFalse()

    // Clear surah 2
    memorizationPreferences.clearSessionForSura(2)
    assertThat(memorizationPreferences.hasSavedSessionForSura(2)).isFalse()
  }

  @Test
  fun `last used settings defaults to window 7 repeat 3 connect 2 when empty and persists changes`() {
    // Initial defaults
    val initialDefaults = memorizationPreferences.getLastUsedSettings()
    assertThat(initialDefaults.windowSize).isEqualTo(7)
    assertThat(initialDefaults.singleRepeat).isEqualTo(3)
    assertThat(initialDefaults.connectRepeat).isEqualTo(2)

    // Save custom settings
    memorizationPreferences.saveLastUsedSettings(
      windowSize = 5,
      singleRepeat = 4,
      connectRepeat = 3
    )

    val updatedDefaults = memorizationPreferences.getLastUsedSettings()
    assertThat(updatedDefaults.windowSize).isEqualTo(5)
    assertThat(updatedDefaults.singleRepeat).isEqualTo(4)
    assertThat(updatedDefaults.connectRepeat).isEqualTo(3)
  }

  @Test
  fun `getAllSavedSurahSessions returns all active surah sessions sorted by last updated`() {
    val configBaqarah = MemorizationConfig(startAyah = SuraAyah(2, 1), endAyah = SuraAyah(2, 286))
    val configYasin = MemorizationConfig(startAyah = SuraAyah(36, 1), endAyah = SuraAyah(36, 83))

    memorizationPreferences.saveSessionForSura(2, configBaqarah, currentStepIndex = 5, currentRepeatIteration = 1)
    Thread.sleep(10)
    memorizationPreferences.saveSessionForSura(36, configYasin, currentStepIndex = 2, currentRepeatIteration = 1)

    val allSessions = memorizationPreferences.getAllSavedSurahSessions()
    assertThat(allSessions).hasSize(2)
    // Most recently updated (Surah 36) should be first
    assertThat(allSessions[0].sura).isEqualTo(36)
    assertThat(allSessions[1].sura).isEqualTo(2)

    // Clear Surah 2
    memorizationPreferences.clearSessionForSura(2)
    val remainingSessions = memorizationPreferences.getAllSavedSurahSessions()
    assertThat(remainingSessions).hasSize(1)
    assertThat(remainingSessions[0].sura).isEqualTo(36)
  }

  @Test
  fun `daily goal and streak tracking works as expected`() {
    assertThat(memorizationPreferences.getDailyGoalAyahs()).isEqualTo(5)
    memorizationPreferences.setDailyGoalAyahs(10)
    assertThat(memorizationPreferences.getDailyGoalAyahs()).isEqualTo(10)

    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(0)
    assertThat(memorizationPreferences.getCurrentStreak()).isEqualTo(0)

    memorizationPreferences.recordAyahsPracticed(4)
    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(4)
    assertThat(memorizationPreferences.getCurrentStreak()).isEqualTo(1)
    assertThat(memorizationPreferences.getBestStreak()).isEqualTo(1)

    // Practice more today
    memorizationPreferences.recordAyahsPracticed(6)
    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(10)
    assertThat(memorizationPreferences.getCurrentStreak()).isEqualTo(1)
  }

  @Test
  fun `flagged ayahs and mistake tracking works as expected`() {
    assertThat(memorizationPreferences.isAyahFlagged(2, 255)).isFalse()
    assertThat(memorizationPreferences.getFlaggedAyahsForSura(2)).isEmpty()

    // Flag Ayah 255
    val flagged = memorizationPreferences.toggleAyahFlagged(2, 255)
    assertThat(flagged).isTrue()
    assertThat(memorizationPreferences.isAyahFlagged(2, 255)).isTrue()
    assertThat(memorizationPreferences.getFlaggedAyahsForSura(2)).containsExactly(255)
    assertThat(memorizationPreferences.getAllFlaggedSurahs()).containsExactly(2)

    // Flag Ayah 256
    memorizationPreferences.toggleAyahFlagged(2, 256)
    assertThat(memorizationPreferences.getFlaggedAyahsForSura(2)).containsExactly(255, 256)

    // Unflag Ayah 255
    val unflagged = memorizationPreferences.toggleAyahFlagged(2, 255)
    assertThat(unflagged).isFalse()
    assertThat(memorizationPreferences.isAyahFlagged(2, 255)).isFalse()
    assertThat(memorizationPreferences.getFlaggedAyahsForSura(2)).containsExactly(256)

    // Unflag Ayah 256 -> empty
    memorizationPreferences.toggleAyahFlagged(2, 256)
    assertThat(memorizationPreferences.getFlaggedAyahsForSura(2)).isEmpty()
    assertThat(memorizationPreferences.getAllFlaggedSurahs()).isEmpty()
  }

  @Test
  fun `getTotalFlaggedAyahsCount and clearAllFlaggedAyahs work properly`() {
    memorizationPreferences.toggleAyahFlagged(2, 255)
    memorizationPreferences.toggleAyahFlagged(2, 256)
    memorizationPreferences.toggleAyahFlagged(36, 1)

    assertThat(memorizationPreferences.getTotalFlaggedAyahsCount()).isEqualTo(3)
    assertThat(memorizationPreferences.getAllFlaggedSurahs()).containsExactly(2, 36)

    memorizationPreferences.clearAllFlaggedAyahs()
    assertThat(memorizationPreferences.getTotalFlaggedAyahsCount()).isEqualTo(0)
    assertThat(memorizationPreferences.getAllFlaggedSurahs()).isEmpty()
  }

  @Test
  fun `resetStreak clears active streak and daily progress`() {
    memorizationPreferences.recordAyahsPracticed(7)
    assertThat(memorizationPreferences.getCurrentStreak()).isEqualTo(1)
    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(7)

    memorizationPreferences.resetStreak()
    assertThat(memorizationPreferences.getCurrentStreak()).isEqualTo(0)
    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(0)
  }

  @Test
  fun `playbackSpeed is persisted in session and default settings`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 7),
      playbackSpeed = 1.25f
    )
    memorizationPreferences.saveSession(config, 0, 1)

    val savedSuraSession = memorizationPreferences.getSavedSessionForSura(1)
    assertThat(savedSuraSession?.config?.playbackSpeed).isEqualTo(1.25f)

    val globalSaved = memorizationPreferences.getSavedConfig()
    assertThat(globalSaved?.playbackSpeed).isEqualTo(1.25f)

    val lastUsed = memorizationPreferences.getLastUsedSettings()
    assertThat(lastUsed.playbackSpeed).isEqualTo(1.25f)
  }

  @Test
  fun `multi-profile management creates and isolates data between profiles`() {
    // 1. Initial default profile
    val initialProfiles = memorizationPreferences.getProfiles()
    assertThat(initialProfiles).hasSize(1)
    assertThat(initialProfiles.first().id).isEqualTo(MemorizationPreferences.DEFAULT_PROFILE_ID)
    assertThat(memorizationPreferences.getActiveProfileId()).isEqualTo(MemorizationPreferences.DEFAULT_PROFILE_ID)

    // Mark Surah 1 as memorized in Default profile
    memorizationPreferences.setSurahMemorized(1, true)
    memorizationPreferences.recordAyahsPracticed(15)
    memorizationPreferences.toggleAyahFlagged(1, 4)
    assertThat(memorizationPreferences.isSurahMemorized(1)).isTrue()
    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(15)
    assertThat(memorizationPreferences.isAyahFlagged(1, 4)).isTrue()

    // 2. Create student/family profile
    val studentProfile = memorizationPreferences.createProfile("Student Ahmad")
    assertThat(studentProfile.name).isEqualTo("Student Ahmad")
    assertThat(memorizationPreferences.getProfiles()).hasSize(2)

    // Switch to Student profile
    memorizationPreferences.setActiveProfile(studentProfile.id)
    assertThat(memorizationPreferences.getActiveProfileId()).isEqualTo(studentProfile.id)

    // Student profile should have fresh isolated data
    assertThat(memorizationPreferences.isSurahMemorized(1)).isFalse()
    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(0)
    assertThat(memorizationPreferences.isAyahFlagged(1, 4)).isFalse()

    // Practice in Student profile
    memorizationPreferences.setSurahMemorized(112, true)
    memorizationPreferences.recordAyahsPracticed(4)
    assertThat(memorizationPreferences.isSurahMemorized(112)).isTrue()
    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(4)

    // 3. Switch back to Default profile
    memorizationPreferences.setActiveProfile(MemorizationPreferences.DEFAULT_PROFILE_ID)
    assertThat(memorizationPreferences.isSurahMemorized(1)).isTrue()
    assertThat(memorizationPreferences.isSurahMemorized(112)).isFalse()
    assertThat(memorizationPreferences.getAyahsMemorizedToday()).isEqualTo(15)
    assertThat(memorizationPreferences.isAyahFlagged(1, 4)).isTrue()

    // 4. Rename & Delete profile
    memorizationPreferences.renameProfile(studentProfile.id, "Ahmad (Advanced)")
    assertThat(memorizationPreferences.getProfiles().find { it.id == studentProfile.id }?.name).isEqualTo("Ahmad (Advanced)")

    memorizationPreferences.deleteProfile(studentProfile.id)
    assertThat(memorizationPreferences.getProfiles()).hasSize(1)
  }

  @Test
  fun `hifz plan persists and calculates daily pace required`() {
    assertThat(memorizationPreferences.getHifzPlan()).isNull()

    // 10 days in future
    val targetDate = System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000L
    val plan = HifzPlan(
      targetSura = 67,
      targetSuraName = "Al-Mulk",
      totalVerses = 30,
      targetDateEpoch = targetDate
    )
    memorizationPreferences.setHifzPlan(plan)

    val savedPlan = memorizationPreferences.getHifzPlan()
    assertThat(savedPlan).isNotNull()
    assertThat(savedPlan?.targetSura).isEqualTo(67)
    assertThat(savedPlan?.targetSuraName).isEqualTo("Al-Mulk")
    assertThat(savedPlan?.totalVerses).isEqualTo(30)
    assertThat(savedPlan?.daysRemaining).isIn(9..11)

    // If 10 verses completed out of 30, with ~10 days left, daily pace should be 2 to 3 verses/day
    val pace = savedPlan?.dailyPaceRequired(10)
    assertThat(pace).isIn(2..3)

    memorizationPreferences.clearHifzPlan()
    assertThat(memorizationPreferences.getHifzPlan()).isNull()
  }

  @Test
  fun `daily reminder settings persist properly`() {
    assertThat(memorizationPreferences.isDailyReminderEnabled()).isFalse()
    memorizationPreferences.setDailyReminderEnabled(true)
    assertThat(memorizationPreferences.isDailyReminderEnabled()).isTrue()

    memorizationPreferences.setDailyReminderTime(8, 30)
    val time = memorizationPreferences.getDailyReminderTime()
    assertThat(time.first).isEqualTo(8)
    assertThat(time.second).isEqualTo(30)
  }

  @Test
  fun `export and import hifz backup json round trip succeeds`() {
    // Setup state
    memorizationPreferences.setSurahMemorized(1, true)
    memorizationPreferences.setSurahMemorized(114, true)
    memorizationPreferences.setRevisionIntervalDays(1, 12)
    memorizationPreferences.setFlaggedAyahsForSura(1, setOf(3, 5))
    memorizationPreferences.setDailyGoalAyahs(10)

    val profile = memorizationPreferences.createProfile("Aisha")
    memorizationPreferences.setActiveProfile(profile.id)
    memorizationPreferences.setSurahMemorized(112, true)

    // Export
    val jsonStr = memorizationPreferences.exportHifzBackupJson()
    assertThat(jsonStr).isNotEmpty()
    assertThat(jsonStr).contains("Aisha")
    assertThat(jsonStr).contains("\"sura\": 112")

    // Clear state
    val newPrefs = FakeSharedPreferences()
    val newMemPrefs = MemorizationPreferences(newPrefs)
    assertThat(newMemPrefs.isSurahMemorized(112)).isFalse()

    // Import into new instance
    val success = newMemPrefs.importHifzBackupJson(jsonStr)
    assertThat(success).isTrue()
    assertThat(newMemPrefs.getActiveProfileId()).isEqualTo(profile.id)
    assertThat(newMemPrefs.isSurahMemorized(112)).isTrue()
    assertThat(newMemPrefs.getProfiles().any { it.name == "Aisha" }).isTrue()
  }

  /**
   * Lightweight in-memory implementation of [SharedPreferences] for unit tests.
   */
  private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String?, defValue: String?): String? =
      values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
      @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)

    override fun getInt(key: String?, defValue: Int): Int =
      values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
      values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
      values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
      values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(values)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val target: MutableMap<String, Any>) : SharedPreferences.Editor {
      private val pending = mutableMapOf<String, Any>()
      private val removed = mutableSetOf<String>()
      private var clear = false

      override fun putString(key: String?, value: String?): SharedPreferences.Editor {
        if (key != null && value != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
        if (key != null && values != null) {
          pending[key] = values
          removed.remove(key)
        }
        return this
      }

      override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
        if (key != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
        if (key != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
        if (key != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
        if (key != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun remove(key: String?): SharedPreferences.Editor {
        if (key != null) {
          pending.remove(key)
          removed.add(key)
        }
        return this
      }

      override fun clear(): SharedPreferences.Editor {
        clear = true
        return this
      }

      override fun commit(): Boolean {
        apply()
        return true
      }

      override fun apply() {
        if (clear) target.clear()
        for (k in removed) target.remove(k)
        target.putAll(pending)
      }
    }
  }
}
