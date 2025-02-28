package org.tasks.compose.edit

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.compose.ChipGroup
import org.tasks.compose.KeyboardDetector
import org.tasks.compose.pickers.DatePickerDialog
import org.tasks.data.Location
import org.tasks.data.displayName
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.TagData
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.filters.Filter
import org.tasks.kmp.org.tasks.time.getRelativeDay

class TaskEditDrawerState (
    val originalFilter: Filter
) {
    var title by mutableStateOf("")
    var dueDate by mutableLongStateOf(0L)

    private var originalLocation: Location? = null
    var location by mutableStateOf<Location?>(null)
    internal var initialFilter = originalFilter
    val filter = mutableStateOf(initialFilter)

    internal val visible = mutableStateOf(false)
    internal val externalActivity = mutableStateOf(false)

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
    }

    fun isChanged(): Boolean =
        (title.trim() != initialTitle.trim()
                || dueDate != _task!!.dueDate
                || filter.value != initialFilter
                || originalLocation != location
                )
    fun clear() {
        title = initialTitle
        dueDate = _task!!.dueDate
        filter.value = initialFilter
        location = originalLocation
    }

    fun retrieveTask(): Task =
        _task!!.copy(
            title = title,
            dueDate = dueDate,
            remoteId = Task.NO_UUID )

}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditDrawer(
    state: TaskEditDrawerState,
    save: () -> Unit = {},
    edit: () -> Unit = {},
    close: () -> Unit = {},
    getList: (() -> Unit),
    getLocation: () -> Unit,
    keyboardDetector: KeyboardDetector)
{
    fun trunk(s: String, len: Int = 10): String =
        if (s.length > len) s.substring(0..len-3) + "..." else s

    val blockDismiss: (Boolean) -> Unit = { on -> keyboardDetector.blockDismiss(on) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val background = colorResource(id = R.color.input_popup_background)
    val foreground = colorResource(id = R.color.input_popup_foreground)

    var datePicker by remember { mutableStateOf(false) }

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
                    if (state.dueDate != 0L) {
                        Chip(
                            title = runBlocking { getRelativeDay(state.dueDate) },
                            leading = IconValues.schedule,
                            action = { datePicker = true; blockDismiss(true) },
                            delete = { state.dueDate = state.task!!.dueDate }
                        )
                    } else {
                        IconChip(IconValues.schedule) { datePicker = true; blockDismiss(true) }
                    }

                    /* Target List */
                    if (state.initialFilter == state.originalFilter && state.filter.value == state.initialFilter) {
                        IconChip(IconValues.list) { blockDismiss(true); getList() }
                    } else {
                        Chip(
                            title = state.filter.value.title!!,
                            leading = IconValues.list,
                            action = { blockDismiss(true); getList() },
                            delete =
                                if (state.initialFilter == state.originalFilter || state.filter.value ==  state.initialFilter) null
                                else {{ state.filter.value = state.initialFilter }}
                        )
                    }

                    /* location */
                    if (state.location == null) {
                        IconChip(IconValues.location, { blockDismiss(true); getLocation()} )
                    } else {
                        Chip(
                            title = trunk(state.location!!.displayName),
                            leading = IconValues.location,
                            action = { blockDismiss(true); getLocation()},
                            delete = { state.location = null }
                        )
                    }

                    /* Main Task Edit launch - must be the last */
                    IconChip(IconValues.more, doEdit)
                }
            }
        }
    }

    if (datePicker) {
        DatePickerDialog(
            initialDate = if (state.dueDate != 0L) state.dueDate else newDateTime().startOfDay().plusDays(1).millis,
            selected = { state.dueDate = it; datePicker = false; blockDismiss(false) },
            dismiss = { datePicker = false; blockDismiss(false) } )
    }
}


@Composable
fun IconChip(icon: ImageVector, action: (() -> Unit)) = Chip(null, null, action, null, icon)

@Composable
private fun Chip (
    title: String?,
    leading: ImageVector?,
    action: (() -> Unit),
    delete: (() -> Unit)? = null,
    titleIcon: ImageVector? = null
) = InputChip (
        selected = false,
        onClick = action,
        label = {
            title?.let {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            titleIcon?.let { Icon(titleIcon, null) }
        },
        leadingIcon = {
            leading?.let { Icon(leading, null) }
        },
        trailingIcon = {
            delete?.let { Icon(IconValues.clear, null, Modifier.clickable(onClick = delete)) }
        }
    )

private object IconValues {
    val clear = Icons.Outlined.Close
    val save = Icons.Outlined.Save
    val more = Icons.Outlined.MoreHoriz
    val schedule = Icons.Outlined.Schedule
    val list = Icons.AutoMirrored.Outlined.List
    val location = Icons.Outlined.LocationOn
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


