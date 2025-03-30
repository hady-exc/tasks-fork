package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.runtime.Composable
import kotlinx.coroutines.runBlocking
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime

val startDateIcon = Icons.Outlined.PendingActions

@Composable
fun StartDateTimeChip(
    currentDate: Long,
    currentTime: Int,
    pickValues: () -> Unit
) {
    if (currentDate == 0L) {
        IconChip(icon = startDateIcon, action = pickValues)
    } else {
        Chip(
            title = runBlocking {
                getRelativeDateTime(
                    currentDate + currentTime,
                    true,
                    DateStyle.FULL,
                    alwaysDisplayFullDate = false //preferences.alwaysDisplayFullDate
                )
            },
            leading = startDateIcon,
            action = pickValues
        )
    }
}