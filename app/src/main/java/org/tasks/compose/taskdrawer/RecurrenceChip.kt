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
    accountType: Int,
    pickCustomRecurrence: (String?) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }

    val NO_DIALOG = 0
    val BASIC_DIALOG = 1
    val CUSTOM_DIALOG = 2
    val showDialog = remember { mutableIntStateOf(NO_DIALOG) }

    if (recurrence.rrule == null) {
        IconChip(icon = repeatIcon, action = { showDialog.intValue = 1})
    } else {
        Chip(
            title = recurrence.title(recurrence.selectionIndex(), true),
            leading = repeatIcon,
            action = { showDialog.intValue = BASIC_DIALOG },
            delete = { setRecurrence(null) }
        )
    }

    when (showDialog.intValue) {
        BASIC_DIALOG -> RecurrenceDialog(
            dismiss = { showDialog.intValue = NO_DIALOG },
            recurrence = recurrence,
            setRecurrence = setRecurrence,
            repeatFrom = repeatFrom,
            onRepeatFromChanged = onRepeatFromChanged,
            peekCustomRecurrence = { showDialog.intValue = CUSTOM_DIALOG }
        )
        CUSTOM_DIALOG -> {
            val state = CustomRecurrencePickerState
                .rememberCustomRecurrencePickerState(
                    rrule = recurrence.recurrence,
                    dueDate = null,
                    accountType = accountType,
                    locale = Locale.getDefault()
                )

            CustomRecurrencePicker(
                state = state.state.collectAsStateWithLifecycle().value,
                save = {
                    setRecurrence(state.getRecur())
                    showDialog.intValue = 0
                },
                discard = { showDialog.intValue = NO_DIALOG },
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
        else -> Unit
    }
}

@Composable
fun rememberRepeatRuleToString(): RepeatRuleToString {
    val context = LocalContext.current
    return remember { RepeatRuleToString(context,Locale.getDefault(),Firebase(context, Preferences(context))) }
}