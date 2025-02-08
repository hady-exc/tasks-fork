package org.tasks.compose.edit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.compose.ChipGroup
import org.tasks.compose.pickers.DatePickerDialog
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.filters.Filter
import org.tasks.kmp.org.tasks.time.getRelativeDay

class TaskEditDrawerState (
    val originalFilter: Filter
) {
    val title = mutableStateOf("")
    val dueDate = mutableLongStateOf(0L)
    internal var initialFilter = originalFilter
    val filter = mutableStateOf(initialFilter)
    internal val visible = mutableStateOf(false)
    internal val externalActivity = mutableStateOf(false)

    private var _task: Task? = null
    val task get() = _task
    private val initialTitle get() = _task?.title ?: ""

    fun setFilter(new: Filter) {
        if ( initialFilter == originalFilter ) initialFilter = new
        filter.value = new
    }

    fun setTask(new: Task) {
        _task = new
        title.value = initialTitle
        dueDate.longValue = _task!!.dueDate
    }

    fun isChanged(): Boolean =
        (title.value.trim() != initialTitle.trim()
                || dueDate.longValue != _task!!.dueDate
                || filter.value != initialFilter
                )
    fun clear() {
        title.value = initialTitle
        dueDate.longValue = _task!!.dueDate
        filter.value = initialFilter
    }

    fun retrieveTask(): Task =
        _task!!.copy().let {
            it.title = title.value
            it.dueDate = dueDate.longValue
            it.uuid = Task.NO_UUID
            it
        }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TaskEditDrawer(
    state: TaskEditDrawerState,
    save: () -> Unit = {},
    edit: () -> Unit = {},
    close: () -> Unit = {},
    getList: (() -> Unit),
    ) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val background = colorResource(id = R.color.input_popup_background)
    val foreground = colorResource(id = R.color.input_popup_foreground)

    val datePicker = remember { mutableStateOf(false) }

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
                value = state.title.value,
                onValueChange = { state.title.value = it },
                trailingIcon = {
                    if (state.isChanged()) {
                        IconButton(onClick = doSave) { Icon(Values.save, "Save") }
                    } else {
                        IconButton(onClick = close) { Icon(Values.clear, "Close") }
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

            LaunchedEffect(state.externalActivity.value == false) {
                requester.requestFocus()
                delay(30) /* workaround for delay in the system between requestFocus and actual focused state */
                keyboardController!!.show()
            }

            Row (modifier = Modifier.padding(8.dp)) {
                ChipGroup {
                    /* Due Date */
                    if (state.dueDate.longValue != 0L) {
                        Chip(
                            title = runBlocking { getRelativeDay(state.dueDate.longValue) },
                            leading = Values.schedule,
                            action = { datePicker.value = true; state.externalActivity.value = true },
                            delete = { state.dueDate.longValue = state.task!!.dueDate }
                        )
                    } else {
                        IconChip(Values.schedule) { datePicker.value = true; state.externalActivity.value = true }
                    }

                    /* Target List */
                    if (state.initialFilter == state.originalFilter && state.filter.value == state.initialFilter) {
                        IconChip(Values.list) { state.externalActivity.value = true; getList() }
                    } else {
                        Chip(
                            title = state.filter.value.title!!,
                            leading = Values.list,
                            action = { state.externalActivity.value = true; getList() },
                            delete =
                                if (state.initialFilter == state.originalFilter || state.filter.value ==  state.initialFilter) null
                                else {{ state.filter.value = state.initialFilter }}
                        )
                    }

                    /* Main Task Edit launch - must be the last */
                    IconChip(Values.more, doEdit)
                }
            }
        }
    }

    if (datePicker.value) {
        DatePickerDialog(
            initialDate = if (state.dueDate.longValue != 0L) state.dueDate.longValue else newDateTime().startOfDay().plusDays(1).millis,
            selected = { state.dueDate.longValue = it; datePicker.value = false; state.externalActivity.value = false },
            dismiss = { datePicker.value = false; state.externalActivity.value = false } )
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
) = InputChip ( //FilterChip(
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
            delete?.let { Icon(Values.clear, null, Modifier.clickable(onClick = delete)) }
        }
    )

@Composable
fun rememberKeyboardHeight(): State<Dp> {
    with(LocalDensity.current) {
        return rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current).toDp())
    }
}

private object Values {
    val clear = Icons.Outlined.Close
    val save = Icons.Outlined.Save
    val more = Icons.Outlined.MoreHoriz
    val schedule = Icons.Outlined.Schedule
    val list = Icons.AutoMirrored.Outlined.List
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


