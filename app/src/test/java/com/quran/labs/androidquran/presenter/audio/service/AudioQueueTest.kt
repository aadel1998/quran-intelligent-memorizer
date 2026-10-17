package com.quran.labs.androidquran.presenter.audio.service

import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.model.QariItem
import com.quran.labs.androidquran.common.audio.model.playback.AudioPathInfo
import com.quran.labs.androidquran.common.audio.model.playback.AudioRequest
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.Locale

class AudioQueueTest {

  private val quranInfo = QuranInfo(MadaniDataSource())

  @Test
  fun testGetUrlReturnsPrimaryLocalOpusFileWhenPresent() {
    val baseDir = Files.createTempDirectory("audio-queue-opus").toFile()
    val suraDir = File(baseDir, "1").apply { mkdirs() }
    val expectedFile = File(suraDir, "1.opus").apply { writeText("test") }
    val format = baseDir.absolutePath + File.separator + "%d" + File.separator + "%d.opus"

    val queue = AudioQueue(quranInfo, audioRequest(format, listOf("opus", "mp3")))

    assertThat(queue.getUrl()).isEqualTo(expectedFile.absolutePath)
  }

  @Test
  fun testGetUrlFallsBackToLocalMp3WhenOpusMissing() {
    val baseDir = Files.createTempDirectory("audio-queue-mp3-fallback").toFile()
    val suraDir = File(baseDir, "1").apply { mkdirs() }
    val expectedFile = File(suraDir, "1.mp3").apply { writeText("test") }
    val format = baseDir.absolutePath + File.separator + "%d" + File.separator + "%d.opus"

    val queue = AudioQueue(quranInfo, audioRequest(format, listOf("opus", "mp3")))

    assertThat(queue.getUrl()).isEqualTo(expectedFile.absolutePath)
  }

  @Test
  fun testGetUrlReturnsPrimaryPathWhenNoLocalFileExists() {
    val baseDir = Files.createTempDirectory("audio-queue-missing").toFile()
    val format = baseDir.absolutePath + File.separator + "%d" + File.separator + "%d.opus"
    val expectedPath = String.format(Locale.US, format, 1, 1)

    val queue = AudioQueue(quranInfo, audioRequest(format, listOf("opus", "mp3")))

    assertThat(queue.getUrl()).isEqualTo(expectedPath)
  }

  @Test
  fun testGetUrlForRemotePathDoesNotApplyLocalFallback() {
    val format = "https://example.com/audio/%d/%d.opus"
    val queue = AudioQueue(quranInfo, audioRequest(format, listOf("opus", "mp3")))

    assertThat(queue.getUrl()).isEqualTo("https://example.com/audio/1/1.opus")
  }

  @Test
  fun testPlayAtWithAllowBasmallahFalsePreventsBasmallah() {
    val format = "https://example.com/audio/%d/%d.opus"
    val req = audioRequest(format, listOf("opus", "mp3"), start = SuraAyah(2, 1), end = SuraAyah(2, 5))
    val queue = AudioQueue(quranInfo, req)

    // Simulate moving to ayah 2
    queue.playAt(2, 2, skipAyahRepeat = true)
    assertThat(queue.isCurrentBasmallah()).isFalse()

    // Loop back to ayah 1 with allowBasmallah = false (memorization mode)
    queue.playAt(2, 1, skipAyahRepeat = true, allowBasmallah = false)
    assertThat(queue.isCurrentBasmallah()).isFalse()
    // Should directly return 2:1 url, not 1:1 Basmallah
    assertThat(queue.getUrl()).isEqualTo("https://example.com/audio/2/1.opus")
  }

  @Test
  fun testPlayAtWithAllowBasmallahTrueTriggersBasmallahForNonGapless() {
    val format = "https://example.com/audio/%d/%d.opus"
    val req = audioRequest(format, listOf("opus", "mp3"), start = SuraAyah(2, 1), end = SuraAyah(2, 5))
    val queue = AudioQueue(quranInfo, req)

    // Move to ayah 2
    queue.playAt(2, 2, skipAyahRepeat = true)

    // Jump back to ayah 1 with allowBasmallah = true (normal mode)
    queue.playAt(2, 1, skipAyahRepeat = true, allowBasmallah = true)
    assertThat(queue.isCurrentBasmallah()).isTrue()
    // Should return 1:1 Basmallah
    assertThat(queue.getUrl()).isEqualTo("https://example.com/audio/1/1.opus")

    // Clear basmallah
    queue.clearBasmallah()
    assertThat(queue.isCurrentBasmallah()).isFalse()
    assertThat(queue.getUrl()).isEqualTo("https://example.com/audio/2/1.opus")
  }

  private fun audioRequest(
    urlFormat: String,
    allowedExtensions: List<String>,
    start: SuraAyah = SuraAyah(1, 1),
    end: SuraAyah = SuraAyah(1, 7)
  ): AudioRequest {
    val qariItem = QariItem(
      id = 1,
      name = "Test Qari",
      url = "https://example.com/mp3/",
      opusUrl = "https://example.com/opus/",
      path = "test_path",
      hasGaplessAlternative = false
    )

    return AudioRequest(
      start = start,
      end = end,
      qari = qariItem,
      enforceBounds = true,
      shouldStream = false,
      audioPathInfo = AudioPathInfo(
        urlFormat = urlFormat,
        localDirectory = "",
        gaplessDatabase = null,
        allowedExtensions = allowedExtensions
      )
    )
  }
}
