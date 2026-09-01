package com.example.habbittracker.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.habbittracker.MainActivity
import com.example.habbittracker.R

/**
 * Posts a reminder as a local notification (F5).
 *
 * From Android 13 on the user may have refused the permission, so posting is
 * checked first and simply skipped: a reminder is not worth crashing over, and
 * there is nothing useful to do about a refusal at this point.
 */
class ReminderNotifier(private val context: Context) {
    fun ensureChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.reminder_channel_description) }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun notify(id: Long, title: String, text: String, habitId: Long? = null) {
        // The check sits here rather than in a helper because that is the only
        // shape the lint check can follow.
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (granted != PackageManager.PERMISSION_GRANTED) return
        ensureChannel()
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(openApp())
                // A reminder about one habit can be answered without opening the app (F9).
                .apply {
                    habitId?.let {
                        addAction(0, context.getString(R.string.reminder_done), complete(it, id.toInt()))
                    }
                }.build()
        NotificationManagerCompat.from(context).notify(id.toInt(), notification)
    }

    private fun complete(habitId: Long, notificationId: Int) =
        PendingIntent.getBroadcast(
            context,
            notificationId,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_COMPLETE
                putExtra(ReminderActionReceiver.EXTRA_HABIT_ID, habitId)
                putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun openApp() =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val CHANNEL_ID = "habit_reminders"
    }
}
