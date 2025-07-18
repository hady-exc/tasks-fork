package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.data.entity.Task
import org.tasks.repeats.RecurrenceHelper
import org.tasks.repeats.RecurrencePickerDialog
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis

private val repeatIcon = Icons.Outlined.Repeat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceChip (
    recurrence: String?,
    setRecurrence: (String?) -> Unit,
    repeatFrom: @Task.RepeatFrom Int,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
    accountType: Int,
    dueDate: Long
) {
    val context = LocalContext.current
    val recurrenceHelper = remember { RecurrenceHelper(context) }
    recurrenceHelper.setRecurrence(recurrence)

    val showPicker = remember { mutableStateOf(false) }

    if (recurrenceHelper.rrule == null) {
        IconChip(icon = repeatIcon, action = { showPicker.value = true })
    } else {
        Chip(
            title = recurrenceHelper.title(recurrenceHelper.selectionIndex(), true),
            leading = repeatIcon,
            action = { showPicker.value = true },
            delete = { setRecurrence(null) }
        )
    }

    if (showPicker.value) {
        RecurrencePickerDialog(
            dismiss = { showPicker.value = false },
            recurrence = recurrenceHelper.recurrence,
            onRecurrenceChanged = setRecurrence,
            repeatFrom = repeatFrom,
            onRepeatFromChanged = onRepeatFromChanged,
            accountType = accountType,
        )
    }

    fun onDueDateChanged() {
        // TODO: move to view model
        recurrenceHelper.recurrence?.takeIf { it.isNotBlank() }?.let { recurrence ->
            val recur = newRecur(recurrence)
            if (recur.frequency == Recur.Frequency.MONTHLY && recur.dayList.isNotEmpty()) {
                val weekdayNum = recur.dayList[0]
                val dateTime =
                    DateTime(dueDate.let { if (it > 0) it else currentTimeMillis() })
                val num: Int
                val dayOfWeekInMonth = dateTime.dayOfWeekInMonth
                num = if (weekdayNum.offset == -1 || dayOfWeekInMonth == 5) {
                    if (dayOfWeekInMonth == dateTime.maxDayOfWeekInMonth) -1 else dayOfWeekInMonth
                } else {
                    dayOfWeekInMonth
                }
                recur.dayList.let {
                    it.clear()
                    it.add(WeekDay(dateTime.weekDay, num))
                }
                setRecurrence(recur.toString())
            }
        }
    }

    LaunchedEffect(dueDate) { onDueDateChanged() }

}

