package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import org.tasks.analytics.Firebase
import org.tasks.data.entity.Task
import org.tasks.preferences.Preferences
import org.tasks.repeats.RepeatRuleToString

private val repeatIcon = Icons.Outlined.Repeat

@Composable
fun RecurrenceChip (
    recurrence: RecurrenceHelper,
    setRecurrence: (String?) -> Unit,
    repeatFrom: @Task.RepeatFrom Int,
    onRepeatFromChanged: (@Task.RepeatFrom Int) -> Unit,
    pickCustomRecurrence: (String?) -> Unit,
) {
    val showRecurrenceDialog = remember { mutableStateOf(false) }

    if (recurrence.rrule == null) {
        IconChip(icon = repeatIcon, action = { showRecurrenceDialog.value = true})
    } else {
        Chip(
            title = recurrence.title(recurrence.selectionIndex(), true),
            leading = repeatIcon,
            action = { showRecurrenceDialog.value = true },
            delete = { setRecurrence(null) }
        )
    }

    if (showRecurrenceDialog.value) {
        RecurrenceDialog(
            dismiss = { showRecurrenceDialog.value = false },
            recurrence = recurrence,
            setRecurrence = setRecurrence,
            repeatFrom = repeatFrom,
            onRepeatFromChanged = onRepeatFromChanged,
            peekCustomRecurrence = pickCustomRecurrence
        )
    }
}

@Composable
fun rememberRepeatRuleToString(): RepeatRuleToString {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val locale = config.locales.get(0)
    return remember { RepeatRuleToString(context,locale,Firebase(context, Preferences(context))) }
}