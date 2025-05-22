package org.tasks.compose.taskdrawer

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.compose.AndroidFragment
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.StartDateControlSet
import kotlinx.coroutines.runBlocking
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.taskedit.TaskEditViewState
import org.tasks.ui.TaskEditViewModel.Companion.TAG_DESCRIPTION
import org.tasks.ui.TaskEditViewModel.Companion.TAG_DUE_DATE
import org.tasks.ui.TaskEditViewModel.Companion.TAG_LIST
import org.tasks.ui.TaskEditViewModel.Companion.TAG_PRIORITY
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.ui.CalendarControlSet
import org.tasks.ui.LocationControlSet
import org.tasks.ui.TaskEditViewModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun TaskEditDrawer(
    state: TaskEditViewModel,
    viewState: TaskEditViewState,
    save: () -> Unit = {},
    edit: () -> Unit = {},
    close: () -> Unit = {},
    pickList: () -> Unit,
    pickTags: () -> Unit,
    pickLocation: () -> Unit,
    pickStartDateTime: () -> Unit,
    pickCustomRecurrence: (String?) -> Unit,
    pickCalendar: () -> Unit,
    setTimer: (Boolean) -> Unit
) {
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
            current = viewState.task.title ?: "",
            onValueChange = { state.setTitle(it.trim { it <= ' ' }) },
            changed = state.hasChanges(),
            save = { save(); /*state.resetTask()*/ },
            close = close
        )

        var showDescription by remember { mutableStateOf(false) }
        Description(
            show = showDescription,
            current = viewState.task.notes ?: "",
            onValueChange = { state.setDescription(it.trim{ it <= ' ' }) },
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
                DescriptionChip(
                    show = !((viewState.task.notes?:"") != "" || showDescription),
                    action = { showDescription = true }
                )

                var total = 0
                var index = 0
                /*
                while (total < 4 && index < state.chipsOrder.size) {
                    when (state.chipsOrder[index++]) {
                        TAG_DESCRIPTION -> {
                            DescriptionChip(
                                show = !((viewState.task.notes?:"") != "" || showDescription),
                                action = { showDescription = true }
                            )
                            total++
                        }
                        TAG_DUE_DATE -> {
                            DueDateChip(
                                current = state.dueDate,
                                setValue = { value -> state.dueDate = value }
                            )
                            total++
                        }
                        TAG_LIST -> {
                            ListChip(
                                initialFilter = state.initialFilter,
                                defaultFilter = state.defaultFilter,
                                currentFiler = state.filter.value,
                                setFilter = { filter -> state.setFilter(filter) },
                                pickList = pickList
                            )
                            total++
                        }
                        TAG_PRIORITY -> {
                            PriorityChip(
                                current = state.priority,
                                setValue = { value -> state.priority = value }
                            )
                            total++
                        }
                        RepeatControlSet.TAG -> {
                            RecurrenceChip(
                                recurrence = RecurrenceHelper (
                                    LocalContext.current,
                                    rememberRepeatRuleToString(),
                                    state.recurrence ),
                                setRecurrence = { state.recurrence = it },
                                repeatFrom = state.repeatFrom,
                                onRepeatFromChanged = { state.repeatFrom = it },
                                pickCustomRecurrence = pickCustomRecurrence
                            )
                            total++
                        }
                        StartDateControlSet.TAG -> {
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
                            total++
                        }
                        TagsControlSet.TAG -> AndroidFragment<TagsControlSet>()
                        /*
                            {
                            TagsChip(
                                current = state.selectedTags,
                                action = pickTags,
                                //delete = if (state.tagsChanged()) { { state.selectedTags = state.initialTags } } else null  // TODO(debug)
                            )
                            total++
                       }
                         */
                        LocationControlSet.TAG -> {
                            LocationChip(
                                current = state.location,
                                setLocation = { location -> state.location = location },
                                pickLocation = pickLocation
                            )
                            total++
                        }
                        CalendarControlSet.TAG -> {
                            CalendarChip(
                                selected = state.selectedCalendarName,
                                select = pickCalendar,
                                clear = { state.selectedCalendar = null }
                            )
                            total++
                        }
                        TimerControlSet.TAG -> {
                            TimerChip(
                                started = state.timerStarted,
                                estimated = state.timerEstimated,
                                elapsed = state.timerElapsed,
                                setTimer = setTimer,
                                setValues = { estimated, elapsed ->
                                    state.timerEstimated = estimated
                                    state.timerElapsed = elapsed
                                }
                            )
                            total++
                        }
                        else -> Unit
                    }
                }
                */
                /* Main TaskEditFragment launch - must be the last */
                IconChip(icon = Icons.Outlined.MoreHoriz, action = { edit(); /*state.resetTask()*/ })
            }
        }
    }
}
