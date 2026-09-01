package com.example.habbittracker.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.habbittracker.HabbitTrackerApp
import com.example.habbittracker.R

/**
 * Fires one reminder and immediately books the next one (F5).
 *
 * Chaining one-off work rather than using periodic work keeps the reminder tied
 * to a wall-clock time: a periodic worker drifts and cannot honour a weekday
 * selection.
 */
class ReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_REMINDER_ID, -1L)
        val container = (applicationContext as HabbitTrackerApp).container
        val reminder = container.reminderRepository.get(id) ?: return Result.success()

        val habit = reminder.habitId?.let { container.habitRepository.getHabit(it) }
        ReminderNotifier(applicationContext).notify(
            id = id,
            title = habit?.name ?: applicationContext.getString(R.string.reminder_general_title),
            text = applicationContext.getString(R.string.reminder_general_text),
        )

        // Booking the next slot here keeps the chain alive without a periodic job.
        container.reminderScheduler.schedule(reminder)
        return Result.success()
    }

    companion object {
        const val KEY_REMINDER_ID = "reminderId"
    }
}
