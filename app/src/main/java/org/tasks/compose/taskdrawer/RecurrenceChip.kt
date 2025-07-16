package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tasks.compose.pickers.CustomRecurrencePicker
import org.tasks.data.entity.Task
import org.tasks.preferences.Preferences
import org.tasks.repeats.CustomRecurrencePickerState
import java.util.Locale

private val repeatIcon = Icons.Outlined.Repeat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceChip (
    recurrence: RecurrenceHelper,
    setRecurrence: (String?) -> Unit,
    repeatFrom: @Task.RepeatFrom Int,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
    accountType: Int,
    pickCustomRecurrence: (String?) -> Unit,
) {
    val showPicker = remember { mutableStateOf(false) }

    if (recurrence.rrule == null) {
        IconChip(icon = repeatIcon, action = { showPicker.value = true })
    } else {
        Chip(
            title = recurrence.title(recurrence.selectionIndex(), true),
            leading = repeatIcon,
            action = { showPicker.value = true },
            delete = { setRecurrence(null) }
        )
    }

    if (showPicker.value) {
        RecurrencePickerDialog(
            dismiss = { showPicker.value = false },
            recurrence = recurrence.recurrence,
            onRecurrenceChanged = setRecurrence,
            repeatFrom = repeatFrom,
            onRepeatFromChanged = onRepeatFromChanged,
            accountType = accountType,
            recurrenceHelper = recurrence
        )
    }
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
        RecurrenceDialog(
            dismiss = dismiss,
            recurrence = recurrence,
            setRecurrence = onRecurrenceChanged,
            repeatFrom = repeatFrom,
            onRepeatFromChanged = { onRepeatFromChanged(it) },
            peekCustomRecurrence = { basicDialog.value = false },
            recurrenceHelper = recurrenceHelper
        )
    } else {
        val state = CustomRecurrencePickerState
            .rememberCustomRecurrencePickerState(
                rrule = recurrence,
                dueDate = null,
                accountType = accountType,
                locale = Locale.getDefault()
            )

        CustomRecurrencePicker(
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

/*
@Composable
fun rememberRepeatRuleToString(): RepeatRuleToString {
    val context = LocalContext.current
    return remember { RepeatRuleToString(context,Locale.getDefault(),Firebase(context, Preferences(context))) }
}
*/
