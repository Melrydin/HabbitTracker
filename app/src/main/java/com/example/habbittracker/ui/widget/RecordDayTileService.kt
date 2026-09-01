package com.example.habbittracker.ui.widget

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import com.example.habbittracker.HabbitTrackerApp
import com.example.habbittracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * A quick settings tile that finishes the day in one tap (F9).
 *
 * It ticks off everything the day still asks for rather than a single habit: the
 * tile has nowhere to choose one, and "record the day" is what the shade is good
 * for.
 */
class RecordDayTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { refresh() }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val repository = (applicationContext as HabbitTrackerApp).container.habitRepository
            val today = LocalDate.now()
            repository
                .observeDay(today)
                .first()
                .entries
                .filterNot { it.fulfilled }
                .forEach { repository.completeHabit(today, it.habit.id) }
            TodayWidget().updateAll(applicationContext)
            refresh()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun refresh() {
        val repository = (applicationContext as HabbitTrackerApp).container.habitRepository
        val open =
            repository
                .observeDay(LocalDate.now())
                .first()
                .entries
                .count { !it.fulfilled }
        qsTile?.apply {
            state = if (open == 0) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(R.string.tile_label)
            // A subtitle is only shown from Android 10 on; the label carries it before that.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = resources.getQuantityString(R.plurals.tile_open, open, open)
            }
            updateTile()
        }
    }
}
