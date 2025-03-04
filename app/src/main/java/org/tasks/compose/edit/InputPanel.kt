package org.tasks.compose.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import org.tasks.data.Location
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.filters.Filter

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

    fun trunk(s: String, len: Int = 10): String =
        s.takeIf{ it.length <= len } ?: (s.substring(0..len-3) + "...")

    Card(
        colors = CardDefaults.cardColors(containerColor = background, contentColor = foreground),
        shape = RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 8.dp)
        ) {
            val requester = remember { FocusRequester() }

            val doSave: ()->Unit = { save(); state.clear() }
            val doEdit = { edit(); state.clear() }

            Spacer(Modifier.height(16.dp))
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

            LaunchedEffect(keyboardDetector.state.value.externalActivity == false) {
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


