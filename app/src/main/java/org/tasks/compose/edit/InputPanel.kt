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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import org.tasks.compose.taskdrawer.StartDateTimeChip
import org.tasks.compose.taskdrawer.TagsChip
import org.tasks.compose.taskdrawer.TitleRow
import org.tasks.data.GoogleTask
import org.tasks.data.Location
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.Tag
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.StartDatePicker
import org.tasks.dialogs.StartDatePicker.Companion.DAY_BEFORE_DUE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_DATE
import org.tasks.dialogs.StartDatePicker.Companion.DUE_TIME
import org.tasks.dialogs.StartDatePicker.Companion.WEEK_BEFORE_DUE
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.GtasksFilter
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay

class TaskEditDrawerState (
    val originalFilter: Filter
) {
    private var initialTask: Task? = null
    val task get() = initialTask!!

    private val initialTitle get() = task.title ?: ""
    private val initialDescription get() = task.notes ?: ""
    private var initialLocation: Location? = null
    internal var initialFilter = originalFilter
    private var _initialTags = ArrayList<TagData>()
    val initialTags get() = _initialTags

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var dueDate by mutableLongStateOf(0L)
    var priority by mutableIntStateOf(0)
    var selectedTags by mutableStateOf<ArrayList<TagData>>( ArrayList() )
    var location by mutableStateOf<Location?>(null)
    val filter = mutableStateOf(initialFilter)
    var startDay by mutableLongStateOf(0L)
    var startTime by mutableIntStateOf(0)

    val startDate: Long
        get() {
            if (dueDate == 0L && startDay < 0) return 0L
            val due = dueDate.takeIf { it > 0 }?.toDateTime()
            return when (startDay) {
                DUE_DATE -> due?.withMillisOfDay(startTime)?.millis ?: 0
                DUE_TIME -> due?.millis ?: 0
                DAY_BEFORE_DUE -> due?.minusDays(1)?.withMillisOfDay(startTime)?.millis ?: 0
                WEEK_BEFORE_DUE -> due?.minusDays(7)?.withMillisOfDay(startTime)?.millis ?: 0
                else -> startDay + startTime
            }
        }

    private fun setStartDate(dueDate: Long, startDate: Long)
    {
        if (startDate < 0) {
            startDay = startDate
        } else if (startDate > 0) {
            val dateTime = startDate.toDateTime()
            val dueDay = dueDate.startOfDay()
            startDay = dateTime.startOfDay().millis
            startTime = dateTime.millisOfDay
            startDay = when (startDay) {
                dueDay -> if (startTime == dueDate.millisOfDay) {
                    startTime = StartDatePicker.NO_TIME
                    DUE_TIME
                } else {
                    DUE_DATE
                }
                dueDay.toDateTime().minusDays(1).millis ->
                    DAY_BEFORE_DUE
                dueDay.toDateTime().minusDays(7).millis ->
                    WEEK_BEFORE_DUE
                else -> startDay
            }
        }
    }

    internal val visible = mutableStateOf(false)

    fun tagsChanged(): Boolean = (initialTags.toHashSet() != selectedTags.toHashSet())

    fun setTask(
        newTask: Task,
        targetFilter: Filter,
        currentLocation: Location?,
        currentTags: ArrayList<TagData>,
        currentAlarms: ArrayList<Alarm>
    ) {
        initialTask = newTask
        if (initialFilter == originalFilter) initialFilter = targetFilter
        filter.value = targetFilter
        title = initialTitle
        description = initialDescription
        dueDate = newTask.dueDate
        initialLocation = currentLocation
        location = currentLocation
        priority = newTask.priority
        _initialTags = currentTags
        selectedTags = currentTags
        setStartDate(newTask.dueDate, newTask.hideUntil)
    }

    fun isChanged(): Boolean {
        return title.trim() != initialTitle.trim()
                || description.trim() != initialDescription.trim()
                || dueDate != initialTask!!.dueDate
                || filter.value != initialFilter
                || initialLocation != location
                || initialTask!!.priority != priority
                || initialTags.toHashSet() != selectedTags.toHashSet()
                || initialTask!!.hideUntil != startDay + startTime

    }

    fun clear() {
        title = initialTitle
        description = ""
        dueDate = task.dueDate
        filter.value = initialFilter
        location = initialLocation
        priority = task.priority
        selectedTags = initialTags
        setStartDate(task.dueDate, task.hideUntil)
    }

    fun retrieveTask(): Task =
        initialTask!!.copy(
            title = title,
            notes = description,
            dueDate = dueDate,
            priority = priority,
            hideUntil = startDate,
            remoteId = Task.NO_UUID )
            .also { task ->
                location?.let { location ->
                    task.putTransitory(Place.KEY, location.place.uid!!)
                }
                when (filter.value) {
                    is GtasksFilter -> task.putTransitory(GoogleTask.KEY, (filter.value as GtasksFilter).remoteId)
                    is CaldavFilter -> task.putTransitory(CaldavTask.KEY, (filter.value as CaldavFilter).uuid)
                    else -> {}
                }
                task.putTransitory(Tag.KEY, selectedTags.mapNotNull{ it.name })
                if (isChanged()) task.putTransitory(Task.TRANS_IS_CHANGED, "")
            }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditDrawer(
    state: TaskEditDrawerState,
    save: () -> Unit = {},
    edit: () -> Unit = {},
    close: () -> Unit = {},
    pickList: () -> Unit,
    pickTags: () -> Unit,
    pickLocation: () -> Unit,
    pickStartDateTime: () -> Unit
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
            modifier = Modifier.height(24.dp).fillMaxWidth(),
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
            save = { save(); state.clear() },
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
                    originalFilter = state.originalFilter,
                    initialFilter = state.initialFilter,
                    currentFiler = state.filter.value,
                    setFilter = { filter -> state.filter.value = filter},
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
                IconChip(icon = Icons.Outlined.MoreHoriz, action = { edit(); state.clear() })
            }
        }
    }
}

/*
@Preview(showBackground = true, widthDp = 320)
@Composable
fun TaskEditDrawerPreview() {
    TaskEditDrawer (
        state = remember {
            TaskEditDrawerState(
                originalFilter = Filter()
            )
        },
        getList = {}
    )
}
*/


