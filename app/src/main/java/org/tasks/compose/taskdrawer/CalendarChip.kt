package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.runtime.Composable

private val calendarIcon = Icons.Outlined.Event

@Composable
fun CalendarChip (
    selected: String?,
    select: () -> Unit,
    clear: () -> Unit
) {
    if (selected.isNullOrBlank()) {
        IconChip(icon = calendarIcon, action = select)
    } else {
        Chip(
            title = selected,
            leading = calendarIcon,
            delete = clear,
            action = select
        )
    }
}