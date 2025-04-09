package org.tasks.compose.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.tasks.compose.taskdrawer.Description
import org.tasks.compose.taskdrawer.DescriptionChip
import org.tasks.compose.taskdrawer.DueDateChip
import org.tasks.compose.taskdrawer.IconChip
import org.tasks.compose.taskdrawer.ListChip
import org.tasks.compose.taskdrawer.LocationChip
import org.tasks.compose.taskdrawer.PriorityChip
import org.tasks.compose.taskdrawer.RecurrenceChip
import org.tasks.compose.taskdrawer.RecurrenceDialog
import org.tasks.compose.taskdrawer.RecurrenceHelper
import org.tasks.compose.taskdrawer.StartDateTimeChip
import org.tasks.compose.taskdrawer.TagsChip
import org.tasks.compose.taskdrawer.TaskDrawerViewModel
import org.tasks.compose.taskdrawer.TitleRow
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.repeats.RepeatRuleToString

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditDrawer(
    state: TaskDrawerViewModel,
    save: () -> Unit = {},
    edit: () -> Unit = {},
    close: () -> Unit = {},
    pickList: () -> Unit,
    pickTags: () -> Unit,
    pickLocation: () -> Unit,
    pickStartDateTime: () -> Unit,
    repeatRuleToString: RepeatRuleToString,
    peekCustomRecurrence: (String?) -> Unit
)
{
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 8.dp)
    ) {
        /* Custom drag handle, because the standard one is too high and so looks ugly */
        Box(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = RoundedCornerShape(corner = CornerSize(3.dp))
                    )
            )
        }

        TitleRow(
            current = state.title,
            onValueChange = { state.title = it },
            changed = state.isChanged(),
            save = { save(); state.resetTask() },
            close = close
        )

        var showDescription by remember { mutableStateOf(false) }
        Description(
            show = showDescription,
            current = state.description,
            onValueChange = { state.description = it },
            onFocusChange = { showDescription = it }
        )

        Row (modifier = Modifier.padding(8.dp)) {
            val context = LocalContext.current
            FlowRow (
                Modifier.wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.Center,
                overflow = FlowRowOverflow.Clip
            ) {
                /* description */
                DescriptionChip(
                    show = !(state.description != "" || showDescription),
                    action = { showDescription = true }
                )
                /* Due Date */
                DueDateChip(
                    current = state.dueDate,
                    setValue = { value -> state.dueDate = value }
                )
                /* Recurrence */
                val showRecurrenceDialog = remember { mutableStateOf(false) }
                if (showRecurrenceDialog.value) {
                    RecurrenceDialog(
                        dismiss = { showRecurrenceDialog.value = false },
                        recurrence = RecurrenceHelper(LocalContext.current, repeatRuleToString, state.recurrence),
                        setRecurrence = { state.recurrence = it },
                        repeatFromCompletion = state.repeatAfterCompletion,
                        onRepeatFromChanged = { state.repeatAfterCompletion = it },
                        peekCustomRecurrence = peekCustomRecurrence
                    )
                }
                RecurrenceChip(
                    recurrence = RecurrenceHelper(LocalContext.current, repeatRuleToString, state.recurrence),
                    onClick = { showRecurrenceDialog.value = true }
                )
                /* Start Date */
                StartDateTimeChip(
                    state.startDay,
                    state.startTime,
                    { runBlocking {
                        getRelativeDateTime(
                            state.startDay + state.startTime,
                            context.is24HourFormat,
                            DateStyle.SHORT,
                            alwaysDisplayFullDate = false //preferences.alwaysDisplayFullDate
                        )
                    }},
                    pickStartDateTime,
                    delete = { state.startDay = 0L; state.startDay = 0}
                )
                /* Target List */
                ListChip(
                    initialFilter = state.initialFilter,
                    currentFiler = state.filter.value,
                    setFilter = { filter -> state.setFilter(filter) },
                    pickList = pickList
                )
                /* Tags */
                TagsChip(
                    current = state.selectedTags,
                    action = pickTags,
                    delete = if (state.tagsChanged()) { { state.selectedTags = state.initialTags } } else null
                )
                /* location */
                LocationChip(
                    current = state.location,
                    setLocation = { location -> state.location = location },
                    pickLocation = pickLocation
                )
                /* priority */
                PriorityChip(
                    current = state.priority,
                    setValue = { value -> state.priority = value }
                )
                /* Main TaskEditFragment launch - must be the last */
                IconChip(icon = Icons.Outlined.MoreHoriz, action = { edit(); state.resetTask() })
            }
        }
    }
}


