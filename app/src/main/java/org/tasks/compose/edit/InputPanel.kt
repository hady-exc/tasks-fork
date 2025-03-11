package org.tasks.compose.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.compose.ChipGroup
import org.tasks.compose.KeyboardDetector
import org.tasks.compose.taskdrawer.DueDateChip
import org.tasks.compose.taskdrawer.IconChip
import org.tasks.compose.taskdrawer.ListChip
import org.tasks.compose.taskdrawer.LocationChip
import org.tasks.compose.taskdrawer.PriorityChip
import org.tasks.data.GoogleTask
import org.tasks.data.Location
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Place
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.CaldavFilter
import org.tasks.filters.Filter
import org.tasks.filters.GtasksFilter

class TaskEditDrawerState (
    val originalFilter: Filter
) {
    var title by mutableStateOf("")
    var dueDate by mutableLongStateOf(0L)
    var priority by mutableIntStateOf(0)

    private var originalLocation: Location? = null
    var location by mutableStateOf<Location?>(null)
    internal var initialFilter = originalFilter
    val filter = mutableStateOf(initialFilter)

    internal val visible = mutableStateOf(false)

    private var _task: Task? = null
    val task get() = _task
    private val initialTitle get() = _task?.title ?: ""

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
        dueDate = _task!!.dueDate
        originalLocation = currentLocation
        location = currentLocation
        priority = _task!!.priority
    }

    fun isChanged(): Boolean =
        (title.trim() != initialTitle.trim()
                || dueDate != _task!!.dueDate
                || filter.value != initialFilter
                || originalLocation != location
                || _task!!.priority != priority
                )

    fun clear() {
        title = initialTitle
        dueDate = _task!!.dueDate
        filter.value = initialFilter
        location = originalLocation
        priority = _task!!.priority
    }

    fun retrieveTask(): Task =
        _task!!.copy(
            title = title,
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
    pickLocation: () -> Unit,
    keyboardDetector: KeyboardDetector)
{
    val keyboardController = LocalSoftwareKeyboardController.current
    val background = colorResource(id = R.color.input_popup_background)
    val foreground = colorResource(id = R.color.input_popup_foreground)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 8.dp)
    ) {
        val requester = remember { FocusRequester() }

        val doSave: ()->Unit = { save(); state.clear() }
        val doEdit = { edit(); state.clear() }

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
        //Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = state.title,
            onValueChange = { state.title = it },
            trailingIcon = {
                if (state.isChanged()) {
                    IconButton(onClick = doSave) { Icon(IconValues.save, "Save") }
                } else {
                    IconButton(onClick = close) { Icon(IconValues.clear, "Close") }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp, 8.dp, 8.dp, 0.dp)
                .focusRequester(requester),
            placeholder = { Text(stringResource(id = R.string.TEA_title_hint)) }, /* "Task name" */
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            keyboardActions = KeyboardActions(onDone = { if (state.isChanged()) doSave() }),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = MaterialTheme.colorScheme.onSurface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.high),
                focusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.medium)
            ),
            shape = MaterialTheme.shapes.medium
        )
        LaunchedEffect(WindowInsets.isImeVisible == false) {
            requester.requestFocus()
            delay(30) /* workaround for delay in the system between requestFocus and actual focused state */
            keyboardController!!.show()
        }

        Row (modifier = Modifier.padding(8.dp)) {
            ChipGroup {
                /* Due Date */
                DueDateChip(
                    current = state.dueDate,
                    setValue = { value -> state.dueDate = value },
                    dialogStarted = { on -> keyboardDetector.blockDismiss(on) }
                )

                /* Target List */
                ListChip(
                    originalFilter = state.originalFilter,
                    initialFilter = state.initialFilter,
                    currentFiler = state.filter.value,
                    setFilter = { filter -> state.filter.value = filter},
                    pickList = pickList
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
                    setValue = { value -> state.priority = value },
                    dialogStarted = { on -> keyboardDetector.blockDismiss(on) }
                )

                /* Main Task Edit launch - must be the last */
                IconChip(icon = Icons.Outlined.MoreHoriz, action = doEdit)
            }
        }
    }
}

private object IconValues {
    val clear = Icons.Outlined.Close
    val save = Icons.Outlined.Save
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


