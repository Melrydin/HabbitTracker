package com.example.habbittracker.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.habbittracker.HabbitTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Ticks a habit off straight from the notification (F9).
 *
 * The point is a check-in without opening the app, so the work happens here and
 * the notification goes away rather than leading somewhere.
 */
class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_COMPLETE) return
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L).takeIf { it > 0 } ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val container = (context.applicationContext as HabbitTrackerApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.habitRepository.completeHabit(LocalDate.now(), habitId)
                if (notificationId >= 0) NotificationManagerCompat.from(context).cancel(notificationId)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.example.habbittracker.COMPLETE_HABIT"
        const val EXTRA_HABIT_ID = "habitId"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }
}
