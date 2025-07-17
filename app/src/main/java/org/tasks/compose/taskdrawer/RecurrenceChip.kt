package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.fortuna.ical4j.model.Recur
import net.fortuna.ical4j.model.WeekDay
import org.tasks.data.entity.Task
import org.tasks.preferences.Preferences
import org.tasks.repeats.BasicRecurrencePicker
import org.tasks.repeats.CustomRecurrenceEdit
import org.tasks.repeats.RecurrenceHelper
import org.tasks.repeats.CustomRecurrenceEditState
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.time.DateTime
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import java.util.Locale

private val repeatIcon = Icons.Outlined.Repeat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceChip (
    recurrenceHelper: RecurrenceHelper,
    setRecurrence: (String?) -> Unit,
    repeatFrom: @Task.RepeatFrom Int,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
    accountType: Int,
    dueDate: Long
) {
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
            recurrenceHelper = recurrenceHelper
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrencePickerDialog (
    dismiss: () -> Unit,
    recurrence: String?,
    onRecurrenceChanged: (String?) -> Unit,
    repeatFrom: @Task.RepeatFrom Int,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
    accountType: Int,
    recurrenceHelper: RecurrenceHelper? = null
) {
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }

    val basicDialog = remember { mutableStateOf(true) }
    if (basicDialog.value) {
        BasicRecurrencePicker(
            dismiss = dismiss,
            recurrence = recurrence,
            setRecurrence = onRecurrenceChanged,
            repeatFrom = repeatFrom,
            onRepeatFromChanged = { onRepeatFromChanged(it) },
            peekCustomRecurrence = { basicDialog.value = false },
            recurrenceHelper = recurrenceHelper
        )
    } else {
        val state = CustomRecurrenceEditState
            .rememberCustomRecurrencePickerState(
                rrule = recurrence,
                dueDate = null,
                accountType = accountType,
                locale = Locale.getDefault()
            )

        CustomRecurrenceEdit(
            state = state.state.collectAsStateWithLifecycle().value,
            save = {
                onRecurrenceChanged(state.getRecur())
                dismiss()
            },
            discard = dismiss,
            setInterval = { state.setInterval(it) },
            setSelectedFrequency = { state.setFrequency(it) },
            setEndDate = { state.setEndDate(it) },
            setSelectedEndType = { state.setEndType(it) },
            setOccurrences = { state.setOccurrences(it) },
            toggleDay = { state.toggleDay(it) },
            setMonthSelection = { state.setMonthSelection(it) },
            calendarDisplayMode = preferences.calendarDisplayMode,
            setDisplayMode = { preferences.calendarDisplayMode = it }
        )
    }
}

