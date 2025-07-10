package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.tasks.calendars.CalendarProvider
import org.tasks.preferences.PermissionChecker

private val calendarIcon = Icons.Outlined.Event

@Composable
fun CalendarChip (
    selected: String?,
    select: () -> Unit,
    clear: () -> Unit
) {
    val context = LocalContext.current
    val calendarProvider = remember {
        CalendarProvider(context, PermissionChecker(context))
    }
    val title = selected?.let {
        calendarProvider.getCalendar(it)?.name
    }
    if (title == null) {
        IconChip(icon = calendarIcon, action = select)
    } else {
        Chip(
            title =  title,
            leading = calendarIcon,
            delete = clear,
            action = select
        )
    }
}