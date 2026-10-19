package com.quran.labs.androidquran.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.ui.QuranRevisionActivity
import java.util.Calendar

class HifzReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    HifzReminderScheduler.showReminderNotification(context)
    HifzReminderScheduler.scheduleNextDailyReminder(context)
  }
}

object HifzReminderScheduler {
  const val CHANNEL_ID = "hifz_daily_reminder_channel"
  const val NOTIFICATION_ID = 99101
  const val ACTION_HIFZ_REMINDER = "com.quran.labs.androidquran.action.HIFZ_REMINDER"

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      val channel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.hifz_reminder_channel_name),
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = context.getString(R.string.hifz_reminder_channel_desc)
        enableLights(true)
        enableVibration(true)
      }
      notificationManager?.createNotificationChannel(channel)
    }
  }

  fun showReminderNotification(context: Context) {
    ensureChannel(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val intent = QuranRevisionActivity.getIntent(context).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      NOTIFICATION_ID,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(context.getString(R.string.hifz_reminder_notif_title))
      .setContentText(context.getString(R.string.hifz_reminder_notif_body))
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText(context.getString(R.string.hifz_reminder_notif_big))
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
  }

  fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, HifzReminderReceiver::class.java).apply {
      action = ACTION_HIFZ_REMINDER
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      NOTIFICATION_ID,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    )

    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, hour)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (before(Calendar.getInstance())) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
      } else {
        alarmManager.setRepeating(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          AlarmManager.INTERVAL_DAY,
          pendingIntent
        )
      }
    } catch (e: SecurityException) {
      alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
  }

  fun scheduleNextDailyReminder(context: Context) {
    val prefs = context.getSharedPreferences("quran_intelligent_memorizer_prefs", Context.MODE_PRIVATE)
    val enabled = prefs.getBoolean("mem_reminder_enabled", false)
    if (enabled) {
      val hour = prefs.getInt("mem_reminder_hour", 9)
      val minute = prefs.getInt("mem_reminder_minute", 0)
      scheduleDailyReminder(context, hour, minute)
    }
  }

  fun cancelDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, HifzReminderReceiver::class.java).apply {
      action = ACTION_HIFZ_REMINDER
    }
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      NOTIFICATION_ID,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
    )
    alarmManager.cancel(pendingIntent)
  }
}
