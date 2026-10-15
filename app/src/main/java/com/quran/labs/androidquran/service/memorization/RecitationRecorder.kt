package com.quran.labs.androidquran.service.memorization

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import timber.log.Timber
import java.io.File

/**
 * Handles recording the user's voice recitation during silent recitation pauses
 * and optional self-check playback against the Qari.
 */
class RecitationRecorder(private val context: Context) {

  private var mediaRecorder: MediaRecorder? = null
  private var currentOutputFile: File? = null
  private var isRecording = false
  private var activePlayer: MediaPlayer? = null

  /**
   * Starts recording user recitation into the specified output file.
   */
  fun startRecording(outputFile: File): Boolean {
    stopRecording()
    stopPlayback()

    return try {
      if (outputFile.exists()) {
        outputFile.delete()
      }

      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }

      recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(128000)
        setAudioSamplingRate(44100)
        setOutputFile(outputFile.absolutePath)
        prepare()
        start()
      }

      mediaRecorder = recorder
      currentOutputFile = outputFile
      isRecording = true
      Timber.d("RecitationRecorder: recording started to ${outputFile.absolutePath}")
      true
    } catch (e: Exception) {
      Timber.e(e, "RecitationRecorder: failed to start recording")
      cleanup()
      false
    }
  }

  /**
   * Stops the active recording and returns the file path if successful.
   */
  fun stopRecording(): String? {
    if (!isRecording) return null
    val file = currentOutputFile
    try {
      mediaRecorder?.stop()
      mediaRecorder?.release()
    } catch (e: Exception) {
      Timber.e(e, "RecitationRecorder: error stopping recorder")
    } finally {
      mediaRecorder = null
      isRecording = false
    }

    return if (file != null && file.exists() && file.length() > 0) {
      Timber.d("RecitationRecorder: recording stopped successfully, size = ${file.length()} bytes")
      file.absolutePath
    } else {
      null
    }
  }

  /**
   * Cancels recording and deletes any partial output file.
   */
  fun cancelRecording() {
    stopRecording()
    cleanupFile()
  }

  /**
   * Plays back the recorded recitation.
   */
  fun playRecording(audioPath: String, onCompletion: () -> Unit): Boolean {
    stopPlayback()
    val file = File(audioPath)
    if (!file.exists() || file.length() == 0L) return false

    return try {
      val player = MediaPlayer().apply {
        setDataSource(file.absolutePath)
        setOnCompletionListener {
          it.release()
          if (activePlayer == it) activePlayer = null
          onCompletion()
        }
        prepare()
        start()
      }
      activePlayer = player
      true
    } catch (e: Exception) {
      Timber.e(e, "RecitationRecorder: playback error")
      false
    }
  }

  /**
   * Stops active playback.
   */
  fun stopPlayback() {
    try {
      activePlayer?.stop()
      activePlayer?.release()
    } catch (e: Exception) {
      Timber.e(e, "RecitationRecorder: error stopping playback")
    } finally {
      activePlayer = null
    }
  }

  fun isCurrentlyRecording(): Boolean = isRecording

  fun isCurrentlyPlaying(): Boolean = activePlayer?.isPlaying == true

  private fun cleanup() {
    try {
      mediaRecorder?.release()
    } catch (ignored: Exception) {}
    mediaRecorder = null
    isRecording = false
    cleanupFile()
  }

  private fun cleanupFile() {
    try {
      currentOutputFile?.delete()
    } catch (ignored: Exception) {}
    currentOutputFile = null
  }
}
