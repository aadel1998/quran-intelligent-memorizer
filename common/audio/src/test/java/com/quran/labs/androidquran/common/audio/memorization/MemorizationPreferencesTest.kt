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
  fun `initial state has no active session`() {
    assertThat(memorizationPreferences.hasSavedSession()).isFalse()
    assertThat(memorizationPreferences.getSavedConfig()).isNull()
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
    assertThat(savedConfig?.memoryTestMode).isTrue()
    assertThat(savedConfig?.enableBridge).isFalse()

    assertThat(memorizationPreferences.getSavedStepIndex()).isEqualTo(4)
    assertThat(memorizationPreferences.getSavedRepeatIteration()).isEqualTo(2)
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
      private var clear = false

      override fun putString(key: String?, value: String?): SharedPreferences.Editor {
        if (key != null && value != null) pending[key] = value
        return this
      }

      override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
        if (key != null && values != null) pending[key] = values
        return this
      }

      override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
        if (key != null) pending[key] = value
        return this
      }

      override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
        if (key != null) pending[key] = value
        return this
      }

      override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
        if (key != null) pending[key] = value
        return this
      }

      override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
        if (key != null) pending[key] = value
        return this
      }

      override fun remove(key: String?): SharedPreferences.Editor {
        if (key != null) pending.remove(key)
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
        target.putAll(pending)
      }
    }
  }
}
