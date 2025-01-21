package org.tasks.compose.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.coordinatorlayout.widget.CoordinatorLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.compose.ChipGroup
import org.tasks.compose.Constants
import org.tasks.compose.pickers.DatePickerDialog
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.kmp.org.tasks.time.getRelativeDay
import org.tasks.themes.TasksTheme

class TaskInputDrawerState (
    val rootView: CoordinatorLayout,
    val initialTitle: String = "",
    val initialDueDate: Long = 0L
) {
    val title = mutableStateOf(initialTitle)
    val dueDate = mutableLongStateOf(initialDueDate)
    internal val visible = mutableStateOf(false)

    fun isChanged(): Boolean = (title.value != initialTitle || dueDate.longValue != initialDueDate)
    fun clear() {
        title.value = initialTitle
        dueDate.longValue = initialDueDate
    }
}

@Composable
fun TaskInputDrawer(
    state: TaskInputDrawerState,
    switchOff: () -> Unit,
    save: () -> Unit,
    edit: () -> Unit
) {
    val fadeColor = colorResource(R.color.input_popup_foreground).copy(alpha = 0.12f)
    val getViewY: (view: CoordinatorLayout) -> Int = {
        val rootViewXY = intArrayOf(0, 0)
        state.rootView.getLocationOnScreen(rootViewXY)
        state.rootView.height + rootViewXY[1]   /* rootViewXY[1] == rootView.y */
    }

    if (state.visible.value) {
        TasksTheme {
            Popup(
                popupPositionProvider = WindowBottomPositionProvider(remember { getViewY(state.rootView) }),
                onDismissRequest = switchOff,
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) {
                AnimatedVisibility(
                    visible = state.visible.value,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = shrinkVertically()
                ) {
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight)
                            .clickable { switchOff() }
                            .background(fadeColor),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        PopupContent(state, save, { switchOff(); edit() }, switchOff)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PopupContent(
    state: TaskInputDrawerState,
    save: () -> Unit = {},
    edit: () -> Unit = {},
    close: () -> Unit = {}
) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val background = colorResource(id = R.color.input_popup_background)
    val foreground = colorResource(id = R.color.input_popup_foreground)
    val padding = keyboardHeight()

    val opened = remember { mutableStateOf(false) }
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

            val doSave: ()->Unit = {  // TODO(check "closability" with all values, as original Task.org do)
                val string = state.title.value.trim()
                if (string != "") save()
                state.clear()
            }
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
                keyboardActions = KeyboardActions(onDone = { doSave() }),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = Constants.textFieldColors(),
                shape = MaterialTheme.shapes.medium
            )

            LaunchedEffect(datePicker.value == false) {
                requester.requestFocus()
                delay(30) /* workaround for delay in the system between requestFocus and actual focused state */
                keyboardController!!.show()
                //opened.value = true
            }

            Row (modifier = Modifier.padding(8.dp)) {
                ChipGroup {
                    /* Due Date */
                    if (state.dueDate.longValue != 0L) {
                        Chip(
                            title = runBlocking { getRelativeDay(state.dueDate.longValue) },
                            leading = Values.schedule,
                            action = { datePicker.value = true },
                            delete = { state.dueDate.longValue = 0L }
                        )
                    } else {
                        IconChip(Values.schedule) { datePicker.value = true }
                    }
                    IconChip(Values.more, doEdit)
                }
            }

            /* close the InputPanel when keyboard is explicitly closed */
            if (opened.value) {
                if (padding.value < 30.dp) {
                    if (datePicker.value == true) opened.value = false
                    else close()
                }
            } else {
                if (padding.value > 60.dp) opened.value = true
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(padding.value)
            )
        }
    }

    if (datePicker.value) {
        DatePickerDialog(
            initialDate = if (state.dueDate.longValue != 0L) state.dueDate.longValue else newDateTime().startOfDay().plusDays(1).millis,
            selected = { state.dueDate.longValue = it; datePicker.value = false },
            dismiss = { datePicker.value = false } )
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
fun keyboardHeight(): State<Dp> {
    with(LocalDensity.current) {
        return rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current).toDp())
    }
}

/*
* Aligns the popup bottom with the bottom of the coordinator_layout
* which is aligned with the top of the IME by the system
*/
private class WindowBottomPositionProvider(
    val rootViewBottomY: Int    /* positioning anchor point */
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return IntOffset(0, rootViewBottomY - popupContentSize.height)
    }
}

private object Values {
    val clear = Icons.Outlined.Close
    val save = Icons.Outlined.Save
    val more = Icons.Outlined.MoreHoriz
    val schedule = Icons.Outlined.Schedule
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun TaskInputDrawerPreview() {
    val context = LocalContext.current
    PopupContent(
        state = remember {
            TaskInputDrawerState(
                rootView = CoordinatorLayout(context),
                initialDueDate = newDateTime().startOfDay().millis
            )
        }
    )
}

