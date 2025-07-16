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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.todoroo.astrid.repeats.RepeatControlSet
import com.todoroo.astrid.tags.TagsControlSet
import com.todoroo.astrid.timers.TimerControlSet
import com.todoroo.astrid.ui.StartDateControlSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.StartDatePicker
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.taskedit.TaskEditViewState
import org.tasks.ui.TaskEditViewModel.Companion.TAG_DESCRIPTION
import org.tasks.ui.TaskEditViewModel.Companion.TAG_DUE_DATE
import org.tasks.ui.TaskEditViewModel.Companion.TAG_LIST
import org.tasks.ui.TaskEditViewModel.Companion.TAG_PRIORITY
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.preferences.Preferences
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay
import org.tasks.ui.CalendarControlSet
import org.tasks.ui.LocationControlSet
import org.tasks.ui.TaskEditViewModel

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun TaskEditDrawer(
    vm: TaskEditViewModel,
    state: State<TaskEditViewState>,
    save: () -> Unit = {},
    edit: () -> Unit = {},
    close: () -> Unit = {},
    pickList: () -> Unit,
    pickTags: () -> Unit,
    pickLocation: () -> Unit,
    pickCustomRecurrence: (String?) -> Unit,
    pickCalendar: () -> Unit,
    setTimer: (Boolean) -> Unit
) {
    /* This is a state for StartDateChip hoisted here.
    * The only reason for hoisting is to be able to reset it together with TaskEditViewModel after save
    *
    * Better solution would be to use the viewModel itself, but it needs changes of its logic,
    * namely to allow to save startDate there after each change regardless of dueDate, and
    * calculate its final correct value during save.
    * This solution will also eliminates the problem that startDate value set in the chip
    * will not be transferred to the TaskEditFragment if bueDate is not set */
    val dueDate = vm.dueDate.collectAsStateWithLifecycle()
    val initialDay = rememberSaveable {
        when (state.value.task.hideUntil) {
            Task.HIDE_UNTIL_DUE.toLong() -> StartDatePicker.DUE_DATE
            Task.HIDE_UNTIL_DUE_TIME.toLong() -> StartDatePicker.DUE_TIME
            Task.HIDE_UNTIL_DAY_BEFORE.toLong() -> StartDatePicker.DAY_BEFORE_DUE
            Task.HIDE_UNTIL_WEEK_BEFORE.toLong() -> StartDatePicker.WEEK_BEFORE_DUE
            else -> state.value.task.hideUntil.startOfDay()
        }
    }
    val initialTime = rememberSaveable { state.value.task.hideUntil.millisOfDay }
    var startDate by rememberSaveable(
        saver = Saver(
            save = { it.longValue },
            restore = { mutableLongStateOf(it) }
        )
    ) { mutableLongStateOf(initialDay) }
    var startTime: Int by rememberSaveable(
        saver = Saver(
            save = { it.intValue },
            restore = { mutableIntStateOf(it) }
        )
    ) { mutableIntStateOf(initialTime) }

    fun packDateTime(dueDate: Long): Long {
        val due = dueDate.takeIf { it > 0 }?.toDateTime()
        return when (startDate) {
            StartDatePicker.DUE_DATE -> due?.withMillisOfDay(startTime)?.millis ?: 0L
            StartDatePicker.DUE_TIME -> due?.millis ?: 0L
            StartDatePicker.DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(startTime)?.millis ?: 0L
            StartDatePicker.WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(startTime)?.millis ?: 0L
            else -> startDate + startTime
        }
    }

    LaunchedEffect(dueDate.value) {
        vm.setStartDate(packDateTime(dueDate.value))
    }

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
            current = vm.viewState.collectAsStateWithLifecycle().value.task.title ?: "",
            onValueChange = { vm.setTitle(it) },
            changed = vm.hasChanges(),
            save = {
                save()
                startDate = initialDay
                startTime = initialTime
            },
            close = close
        )

        var showDescription by remember { mutableStateOf(false) }
        Description(
            show = showDescription,
            current = vm.viewState.collectAsStateWithLifecycle().value.task.notes ?: "",
            onValueChange = { vm.setDescription(it) },
            onFocusChange = { showDescription = it }
        )

        Row (modifier = Modifier.padding(8.dp)) {
            val context = LocalContext.current
            val preferences = remember { Preferences(context) }
            FlowRow (
                Modifier.wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.Center,
                overflow = FlowRowOverflow.Clip
            ) {
                var total = 0
                var index = 0
                val chipsOrder = remember { vm.viewState.value.displayOrder }
                while (total < 4 && index < chipsOrder.size) {
                    when (chipsOrder[index++]) {
                        TAG_DESCRIPTION -> {
                            DescriptionChip(
                                show = !((vm.viewState.value.task.notes?: "") != "" || showDescription),
                                action = { showDescription = true }
                            )
                            total++
                        }
                        TAG_DUE_DATE -> {
                            DueDateChip(
                                current = vm.dueDate.collectAsStateWithLifecycle().value,
                                setValue = { vm.setDueDate(it) }
                            )
                            total++
                        }
                        TAG_LIST -> {
                            ListChip(
                                //initialFilter = state.initialFilter,
                                defaultFilter = vm.originalState.collectAsStateWithLifecycle().value.list,
                                currentFiler = state.value.list,
                                setFilter = { vm.setList(it) },
                                pickList = pickList
                            )
                            total++
                        }
                        TAG_PRIORITY -> {
                            PriorityChip(
                                current = vm.viewState.collectAsStateWithLifecycle().value.task.priority,
                                setValue = { value -> vm.setPriority(value) }
                            )
                            total++
                        }
                        RepeatControlSet.TAG -> {
                            RecurrenceChip(
                                recurrence = RecurrenceHelper(
                                    LocalContext.current,
                                    state.value.task.recurrence ),
                                setRecurrence = { vm.setRecurrence(it) },
                                repeatFrom = vm.viewState.collectAsStateWithLifecycle().value.task.repeatFrom,
                                onRepeatFromChanged = { vm.setRepeatFrom(it) },
                                accountType = state.value.list.account.accountType,
                                pickCustomRecurrence = pickCustomRecurrence
                            )
                            total++
                        }
                        StartDateControlSet.TAG -> {
                            StartDateTimeChip(
                                startDate,
                                startTime,
                                vm.dueDate.collectAsStateWithLifecycle().value,
                                { day, time ->
                                    startDate = day
                                    startTime = time
                                    vm.setStartDate(packDateTime(vm.dueDate.value))
                                },
                                { runBlocking {
                                    getRelativeDateTime(
                                        vm.startDate.value,
                                        context.is24HourFormat,
                                        DateStyle.SHORT,
                                        alwaysDisplayFullDate = false //preferences.alwaysDisplayFullDate
                                    )
                                }},
                                autoclose = preferences.getBoolean(
                                    R.string.p_auto_dismiss_datetime_edit_screen,
                                    false
                                ),
                                showDueDate = !vm.viewState.collectAsStateWithLifecycle().value.list.account.isOpenTasks,
                            )
                            total++
                        }
                        TagsControlSet.TAG -> {
                            TagsChip(
                                current = state.value.tags.toImmutableList(),
                                action = pickTags,
                                //delete = if (state.tagsChanged()) { { state.selectedTags = state.initialTags } } else null  // TODO(debug)
                            )
                            total++
                        }
                        LocationControlSet.TAG -> {
                            LocationChip(
                                current = state.value.location,
                                setLocation = { vm.setLocation(it) },
                                pickLocation = pickLocation
                            )
                            total++
                        }
                        CalendarControlSet.TAG -> {
                            CalendarChip(
                                selected = state.value.calendar,
                                select = pickCalendar,
                                clear = { vm.setCalendar(null) }
                            )
                            total++
                        }
                        TimerControlSet.TAG -> {
                            TimerChip(
                                started = vm.timerStarted.collectAsStateWithLifecycle().value,
                                estimated = vm.estimatedSeconds.collectAsStateWithLifecycle().value,
                                elapsed = vm.elapsedSeconds.collectAsStateWithLifecycle().value,
                                setTimer = setTimer,
                                setValues = { estimated, elapsed ->
                                    vm.estimatedSeconds.update { estimated }
                                    vm.elapsedSeconds.update { elapsed }
                                }
                            )
                            total++
                        }
                        else -> Unit
                    }
                }
                /* Main TaskEditFragment launch - must be the last */
                IconChip(icon = Icons.Outlined.MoreHoriz, action = { edit(); vm.resetToOriginal() /* TODO(): is it necessary?? */})
            }
        }
    }
}
