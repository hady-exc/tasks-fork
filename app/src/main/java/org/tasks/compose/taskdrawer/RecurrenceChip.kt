package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.tasks.analytics.Firebase
import org.tasks.compose.pickers.CustomRecurrencePicker
import org.tasks.data.entity.Task
import org.tasks.preferences.Preferences
import org.tasks.repeats.CustomRecurrencePickerState
import org.tasks.repeats.RepeatRuleToString
import java.util.Locale

private val repeatIcon = Icons.Outlined.Repeat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceChip (
    recurrence: RecurrenceHelper,
    setRecurrence: (String?) -> Unit,
    repeatFrom: @Task.RepeatFrom Int,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
    pickCustomRecurrence: (String?) -> Unit,
) {
    val showDialog = remember { mutableIntStateOf(0) }

    if (recurrence.rrule == null) {
        IconChip(icon = repeatIcon, action = { showDialog.intValue = 1})
    } else {
        Chip(
            title = recurrence.title(recurrence.selectionIndex(), true),
            leading = repeatIcon,
            action = { showDialog.intValue = 1 },
            delete = { setRecurrence(null) }
        )
    }

    if (showDialog.intValue == 1) {
        RecurrenceDialog(
            dismiss = { showDialog.intValue = 0 },
            recurrence = recurrence,
            setRecurrence = setRecurrence,
            repeatFrom = repeatFrom,
            onRepeatFromChanged = onRepeatFromChanged,
            peekCustomRecurrence = { showDialog.intValue = 2 }
        )
    } else if (showDialog.intValue == 2) {
        val state = CustomRecurrencePickerState
            .rememberCustomRecurrencePickerState(
                rrule = recurrence.recurrence,
                dueDate = null,
                accountType = 0, //
                locale = Locale.getDefault()
            )
        val context = LocalContext.current
        val preferences = Preferences(context)

        CustomRecurrencePicker(
            state = state.state.collectAsStateWithLifecycle().value,
            save = {
                setRecurrence(state.getRecur())
                showDialog.intValue = 0
            },
            discard = { showDialog.intValue = 0 },
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

@Composable
fun rememberRepeatRuleToString(): RepeatRuleToString {
    val context = LocalContext.current
    return remember { RepeatRuleToString(context,Locale.getDefault(),Firebase(context, Preferences(context))) }
}