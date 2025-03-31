package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.todoroo.astrid.ui.StartDateControlSet.Companion.getRelativeDateString
import org.tasks.R
import org.tasks.dialogs.StartDatePicker

val startDateIcon = Icons.Outlined.PendingActions

@Composable
fun StartDateTimeChip(
    currentDate: Long,
    currentTime: Int,
    printDate: () -> String,
    pickValues: () -> Unit,
    delete: (() -> Unit)? = null
) {
    if (currentDate == 0L) {
        IconChip(icon = startDateIcon, action = pickValues)
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
            title = text,
            leading = startDateIcon,
            action = pickValues,
            delete = delete
        )
    }
}