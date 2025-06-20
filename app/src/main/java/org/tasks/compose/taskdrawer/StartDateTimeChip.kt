package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.todoroo.astrid.ui.StartDateControlSet.Companion.getRelativeDateString
import org.tasks.R
import org.tasks.compose.pickers.StartDateTimePicker
import org.tasks.data.entity.Task
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay

val startDateIcon = Icons.Outlined.PendingActions

@Composable
fun StartDateTimeChip(
    currentDate: Long,
    dueDate: Long,
    setStartDate: (Long) -> Unit,
    printDate: () -> String,
    autoclose: Boolean,
    showDueDate: Boolean,
    delete: (() -> Unit)? = null
) {
    val day =  when (currentDate) {
        Task.HIDE_UNTIL_DUE.toLong() -> DUE_DATE
        Task.HIDE_UNTIL_DUE_TIME.toLong() -> DUE_TIME
        Task.HIDE_UNTIL_DAY_BEFORE.toLong() -> DAY_BEFORE_DUE
        Task.HIDE_UNTIL_WEEK_BEFORE.toLong() -> WEEK_BEFORE_DUE
        else -> currentDate.startOfDay()
    }
    val time = currentDate.millisOfDay
    var selectedDay by remember { mutableLongStateOf(day) }
    var selectedTime by remember { mutableIntStateOf(time) }

    fun getDateTimeValue(): Long {
        val due = dueDate.takeIf { it > 0 }?.toDateTime()
        return when (selectedDay) {
            DUE_DATE -> due?.withMillisOfDay(selectedTime)?.millis ?: 0L
            DUE_TIME -> due?.millis ?: 0L
            DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(selectedTime)?.millis ?: 0L
            WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(selectedTime)?.millis ?: 0L
            else -> selectedDay + selectedTime
        }
    }

    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        StartDateTimePicker(
            accept = {
                setStartDate(getDateTimeValue())
                showPicker = false
            },
            dismiss = { showPicker = false },
            selectedDay = selectedDay,
            selectedTime = selectedTime,
            updateValues = { date, time ->
                selectedDay = date
                selectedTime = time
            },
            autoclose = autoclose,
            showDueDate = showDueDate
        )
    }

    if (selectedDay == 0L) {
        IconChip(icon = startDateIcon, action = { showPicker = true })
    } else {
        val context = LocalContext.current
        val text = when (selectedDay) {
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
            action = { showPicker = true },
            delete = {
                selectedDay = 0L
                selectedTime = 0
                delete?.invoke()
            }
        )
    }

    LaunchedEffect(dueDate) {
        setStartDate(getDateTimeValue())
    }
}