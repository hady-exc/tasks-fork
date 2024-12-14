package org.tasks.compose.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import org.tasks.R
import org.tasks.compose.pickers.DatePickerDialog
import org.tasks.themes.TasksTheme
import org.tasks.date.DateTimeUtils.newDateTime


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

@Composable
fun InputPanel(
    showPopup: MutableState<Boolean>,
    rootView: CoordinatorLayout,
    switchOff: () -> Unit,
    save: (String) -> Unit,
    edit: (String) -> Unit
) {
    val fadeColor = colorResource(R.color.input_popup_foreground).copy(alpha = 0.12f)
    val getViewY: (view: CoordinatorLayout) -> Int = {
        val rootViewXY = intArrayOf(0, 0)
        rootView.getLocationOnScreen(rootViewXY)
        rootView.height + rootViewXY[1]   /* rootViewXY[1] == rootView.y */
    }

    if (showPopup.value) {
        TasksTheme {
            Popup(
                popupPositionProvider = WindowBottomPositionProvider(remember { getViewY(rootView) }),
                onDismissRequest = switchOff,
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) {
                AnimatedVisibility(
                    visible = showPopup.value,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = shrinkVertically()
                ) {
                    /* Modifier.fillMaxSize() gives height not covering the system status bar,
                     * so screenHeight used as a workaround to prevent flicking on top  */
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenHeight)
                            .clickable { switchOff() }
                            .background(fadeColor),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        PopupContent(save, { switchOff(); edit(it) }, switchOff)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PopupContent(
    save: (String) -> Unit = {},
    edit: (String) -> Unit = {},
    close: () -> Unit = {}
) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val background = colorResource(id = R.color.input_popup_background)
    val foreground = colorResource(id = R.color.input_popup_foreground)
    val padding = keyboardHeight()

    val opened = remember { mutableStateOf(false) }

    val dtPicker = remember { mutableStateOf(false) }
    val date = rememberSaveable { mutableStateOf<Long?>(null) }

    Card(
        colors = CardDefaults.cardColors(containerColor = background, contentColor = foreground),
        shape = RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val text = rememberSaveable { mutableStateOf("") }
            val requester = remember { FocusRequester() }

            val doSave = {
                val string = text.value.trim()
                if (string != "") save(string)
                text.value = ""
            }
            val doEdit = { edit(text.value.trim()) }

            TextField(
                value = text.value,
                onValueChange = { text.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp, 8.dp, 8.dp, 0.dp)
                    .focusRequester(requester),
                singleLine = true,
                enabled = true,
                readOnly = false,
                placeholder = { Text(stringResource(id = R.string.TEA_title_hint)) }, /* "Task name" */
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { doSave() }),
                colors = TextFieldDefaults.colors(
                    cursorColor = MaterialTheme.colorScheme.secondary,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = foreground,
                    unfocusedIndicatorColor = foreground.copy(alpha = 0.7f),
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = foreground
                )
            )
            LaunchedEffect(dtPicker.value == false) {
                requester.requestFocus()
                /* The delay below is a workaround trick necessary because
                  focus requester works via queue in some delayed coroutine and
                  the isFocused state is not set on yet on return from requestFocus() call.
                  As a consequence keyboardController.show() is ignored by system because
                  "the view is not served"
                  The delay period is not the guarantee but makes it working almost always
                * */
                delay(30)
                keyboardController!!.show()
                //opened.value = true
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { dtPicker.value = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_schedule_24px),
                        contentDescription = "Date Time"
                    )
                }
                IconButton(onClick = { doEdit() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_settings_24px),
                        contentDescription = "Details"
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    if (text.value == "") {
                        IconButton(onClick = { close() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_clear_24px),
                                contentDescription = "Close"
                            )
                        }
                    } else {
                        IconButton(onClick = { doSave() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_done_24px),
                                contentDescription = "Close"
                            )
                        }
                    }
                }
            }

            /* close the InputPanel when keyboard is explicitly closed */
            if (opened.value) {
                if (padding.value < 30.dp) {
                    if (dtPicker.value == true) opened.value = false
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

    if (dtPicker.value) {
        DatePickerDialog(
            initialDate = date.value ?: newDateTime().endOfDay().plusDays(1).millis,
            selected = { date.value = it; dtPicker.value = false },
            dismiss = { dtPicker.value = false } )
    }
}

@Composable
fun keyboardHeight(): State<Dp> {
    with(LocalDensity.current) {
        return rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current).toDp())
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun InputPanelPreview() {
    PopupContent()
}