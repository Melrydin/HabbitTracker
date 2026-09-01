package com.example.habbittracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.habbittracker.domain.model.DayStatus
import com.example.habbittracker.ui.theme.HabitTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * A calendar grid of one month, each cell colored by the day status (F4).
 *
 * Passed is the only colored state. A missed day is neutral grey rather than red,
 * so the grid shows what happened without judging it, and a day nothing was asked
 * of stays an empty outline.
 */
@Composable
fun MonthHeatmap(
    month: YearMonth,
    statuses: Map<LocalDate, DayStatus>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val weekdays = remember(locale) { DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, locale) } }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        weeksOf(month).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        status = date?.let { statuses[it] } ?: DayStatus.NEUTRAL,
                        isToday = date == today,
                        isFuture = date != null && date > today,
                        onClick = { date?.let(onDayClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    status: DayStatus,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val marker = HabitTheme.status
    val scheme = MaterialTheme.colorScheme
    val background =
        when {
            date == null || isFuture -> Color.Transparent
            status == DayStatus.PASSED -> marker.passedContainer
            status == DayStatus.FAILED -> scheme.surfaceContainerHigh
            else -> Color.Transparent
        }
    val content =
        when {
            status == DayStatus.PASSED -> marker.onPassedContainer
            isFuture -> scheme.outlineVariant
            else -> scheme.onSurfaceVariant
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .background(background, CELL_SHAPE)
                .then(if (isToday) Modifier.border(1.5.dp, scheme.primary, CELL_SHAPE) else Modifier)
                .then(if (date != null && !isFuture) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = content,
            )
        }
    }
}

/**
 * The month split into weeks starting on Monday. Leading and trailing slots of the
 * first and last week are null so every row holds exactly seven cells.
 */
private fun weeksOf(month: YearMonth): List<List<LocalDate?>> {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - DayOfWeek.MONDAY.value
    val slots = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trailing = (DAYS_PER_WEEK - slots.size % DAYS_PER_WEEK) % DAYS_PER_WEEK
    return (slots + List(trailing) { null }).chunked(DAYS_PER_WEEK)
}

private const val DAYS_PER_WEEK = 7
private val CELL_SHAPE = RoundedCornerShape(8.dp)
