package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.todoroo.astrid.ui.StartDateControlSet.Companion.getRelativeDateString
import org.tasks.R
import org.tasks.data.entity.Task
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay

val startDateIcon = Icons.Outlined.PendingActions

@Composable
fun StartDateTimeChip(
    currentDate: Long,
    printDate: () -> String,
    pickValues: () -> Unit,
    delete: (() -> Unit)? = null
) {

    if (currentDate == 0L) {
        IconChip(icon = startDateIcon, action = pickValues)
    } else {
        val day =  when (currentDate) {
            Task.HIDE_UNTIL_DUE.toLong() -> DUE_DATE
            Task.HIDE_UNTIL_DUE_TIME.toLong() -> DUE_TIME
            Task.HIDE_UNTIL_DAY_BEFORE.toLong() -> DAY_BEFORE_DUE
            Task.HIDE_UNTIL_WEEK_BEFORE.toLong() -> WEEK_BEFORE_DUE
            else -> currentDate.startOfDay()
        }
        val time = currentDate.millisOfDay
        val context = LocalContext.current
        val text = when (currentDate) {
            StartDatePicker.DUE_DATE -> context.getRelativeDateString(R.string.due_date, time)
            StartDatePicker.DUE_TIME -> context.getString(R.string.due_time)
            StartDatePicker.DAY_BEFORE_DUE -> context.getRelativeDateString(R.string.day_before_due, time)
            StartDatePicker.WEEK_BEFORE_DUE -> context.getRelativeDateString(R.string.week_before_due, time)
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