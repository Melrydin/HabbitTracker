package com.example.habbittracker.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.habbittracker.HabbitTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Books every reminder again after a reboot (F5).
 *
 * Scheduled work does not survive a restart, so without this a reminder set
 * before the reboot would simply stop firing, silently.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val container = (context.applicationContext as HabbitTrackerApp).container
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                container.reminderRepository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
