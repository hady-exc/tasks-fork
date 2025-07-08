package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.todoroo.astrid.ui.StartDateControlSet.Companion.getRelativeDateString
import org.tasks.R
import org.tasks.compose.pickers.StartDateTimePicker
import org.tasks.dialogs.StartDatePicker

val startDateIcon = Icons.Outlined.PendingActions

@Composable
fun StartDateTimeChip(
    currentDate: Long,
    currentTime: Int,
    dueDate: Long,
    sendValues: (Long, Int) -> Unit,
    printDate: () -> String,
    autoclose: Boolean,
    showDueDate: Boolean
) {
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        StartDateTimePicker(
            accept = {
                sendValues(currentDate, currentTime)
                showPicker = false
            },
            dismiss = { showPicker = false },
            selectedDay = currentDate,
            selectedTime = currentTime,
            updateValues = { date, time ->
                sendValues(date, time)
            },
            autoclose = autoclose,
            showDueDate = showDueDate
        )
    }

    if (currentDate == 0L && currentTime == 0) {
        IconChip(icon = startDateIcon, action = { showPicker = true })
    } else {
        val context = LocalContext.current
        val text = when (currentDate) {
            StartDatePicker.DUE_DATE -> context.getRelativeDateString(R.string.due_date, currentTime)
            StartDatePicker.DUE_TIME -> context.getString(R.string.due_time)
            StartDatePicker.DAY_BEFORE_DUE -> context.getRelativeDateString(R.string.day_before_due, currentTime)
            StartDatePicker.WEEK_BEFORE_DUE -> context.getRelativeDateString(R.string.week_before_due, currentTime)
            in 1..Long.MAX_VALUE -> printDate()
            else -> stringResource(id = R.string.no_start_date)
        }

        Chip(
            title = if (text.length > 16) text.substring(0..12)+"..." else text,
            leading = startDateIcon,
            action = { showPicker = true },
            delete = { sendValues(0L,0) },
            contentColor = if (dueDate == 0L) MaterialTheme.colorScheme.error else Color.Unspecified
        )
    }
}