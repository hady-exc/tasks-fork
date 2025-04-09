package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable

private val repeatIcon = Icons.Outlined.Repeat

@Composable
fun RecurrenceChip (
    recurrence: RecurrenceHelper,
    onClick: () -> Unit
) {
    if (recurrence.rrule == null) {
        IconChip(icon = repeatIcon, action = onClick)
    } else {
        Chip(
            title = recurrence.title(recurrence.selectionIndex(), true),
            leading = repeatIcon,
            action = onClick
        )
    }
}