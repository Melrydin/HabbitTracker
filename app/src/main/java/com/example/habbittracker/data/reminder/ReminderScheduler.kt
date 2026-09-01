package com.example.habbittracker.data.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.habbittracker.domain.ReminderSchedule
import com.example.habbittracker.domain.model.Reminder
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Books reminders as one-off work, one occurrence at a time (F5).
 *
 * Everything is local: WorkManager only wakes the app, which then posts the
 * notification itself.
 */
class ReminderScheduler(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val workManager = WorkManager.getInstance(context)

    /** Books the next occurrence, or cancels the reminder if it has none. */
    fun schedule(reminder: Reminder) {
        val next = ReminderSchedule.nextOccurrence(reminder, LocalDateTime.now(clock))
        if (next == null) {
            cancel(reminder.id)
            return
        }
        val delay = Duration.between(LocalDateTime.now(clock), next)
        workManager.enqueueUniqueWork(
            workName(reminder.id),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay.toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putLong(ReminderWorker.KEY_REMINDER_ID, reminder.id).build())
                .build(),
        )
    }

    fun cancel(reminderId: Long) {
        workManager.cancelUniqueWork(workName(reminderId))
    }

    private fun workName(reminderId: Long) = "$WORK_PREFIX$reminderId"

    private companion object {
        const val WORK_PREFIX = "reminder-"
    }
}
