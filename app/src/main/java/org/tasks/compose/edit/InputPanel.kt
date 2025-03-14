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
import androidx.compose.ui.unit.dp
import org.tasks.compose.taskdrawer.Description
import org.tasks.compose.taskdrawer.DescriptionChip
import org.tasks.compose.taskdrawer.DueDateChip
import org.tasks.compose.taskdrawer.IconChip
import org.tasks.compose.taskdrawer.ListChip
import org.tasks.compose.taskdrawer.LocationChip
import org.tasks.compose.taskdrawer.PriorityChip
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
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.GtasksFilter
import timber.log.Timber

class TaskEditDrawerState (
    val originalFilter: Filter
) {
    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var dueDate by mutableLongStateOf(0L)
    var priority by mutableIntStateOf(0)
    var selectedTags by mutableStateOf<ArrayList<TagData>>( ArrayList() )

    private var originalLocation: Location? = null
    var location by mutableStateOf<Location?>(null)
    internal var initialFilter = originalFilter
    val filter = mutableStateOf(initialFilter)

    internal val visible = mutableStateOf(false)

    private var _task: Task? = null
    val task get() = _task!!
    private val initialTitle get() = _task?.title ?: ""
    private var initialTags = ArrayList<TagData>()

    fun tagsChanged(): Boolean =
        initialTags.toHashSet() != selectedTags.toHashSet()
    fun resetTags() { selectedTags = initialTags }

    fun setTask(
        newTask: Task,
        targetFilter: Filter,
        currentLocation: Location?,
        currentTags: ArrayList<TagData>,
        currentAlarms: ArrayList<Alarm>
    ) {
        _task = newTask
        if (initialFilter == originalFilter) initialFilter = targetFilter
        filter.value = targetFilter
        title = initialTitle
        description = task.notes ?: ""
        dueDate = _task!!.dueDate
        originalLocation = currentLocation
        location = currentLocation
        priority = _task!!.priority
        initialTags = currentTags
        selectedTags = currentTags
    }

    fun isChanged(): Boolean =
        (title.trim() != initialTitle.trim()
                || description.trim() != (task.notes ?: "").trim()
                || dueDate != _task!!.dueDate
                || filter.value != initialFilter
                || originalLocation != location
                || _task!!.priority != priority
                || initialTags.toHashSet() != selectedTags.toHashSet()
                )

    fun clear() {
        title = initialTitle
        description = ""
        dueDate = _task!!.dueDate
        filter.value = initialFilter
        location = originalLocation
        priority = _task!!.priority
        selectedTags = initialTags
    }

    fun retrieveTask(): Task =
        _task!!.copy(
            title = title,
            notes = description,
            dueDate = dueDate,
            priority = priority,
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
    pickLocation: () -> Unit)
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
                    delete = if (state.tagsChanged()) { { state.resetTags() } } else null
                )
                /* location */
                LocationChip(
                    current = state.location,
                    setLocation = { location -> state.location = location},
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


