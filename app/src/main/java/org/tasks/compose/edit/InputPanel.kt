package org.tasks.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBindings
import com.google.android.material.bottomappbar.BottomAppBar
import kotlinx.coroutines.delay
import org.tasks.R

private class WindowBottomPositionProvider(
    val rootView: CoordinatorLayout?
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val bottomBar = ViewBindings.findChildViewById<BottomAppBar>(rootView, R.id.bottomAppBar)
        val bY = if ( bottomBar!!.isVisible ) bottomBar.height else 154  /* TODO(find bottom bar height) */
        return IntOffset(0, (windowSize.height - popupContentSize.height + bY ) )
    }
}
@Composable
fun InputPanel(showPopup: MutableState<Boolean>,
               rootView: CoordinatorLayout?,
               save: (String) -> Unit,
               edit: (String) -> Unit )
{
    val showPopup = showPopup

    if ( showPopup.value ) {
        MaterialTheme {
            Popup(
                popupPositionProvider = WindowBottomPositionProvider(rootView),
                onDismissRequest = { showPopup.value = false },
                properties = PopupProperties( focusable = true, dismissOnClickOutside = true )
            ) {
                PopupContent(save, { showPopup.value = false; edit(it) }, { showPopup.value = false } )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PopupContent(save: (String) -> Unit = {},
                         edit: (String) -> Unit = {},
                         close: () -> Unit = {}) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val requester = remember { FocusRequester() }

    Card(
        backgroundColor = colorResource(R.color.input_popup_background),
        contentColor = colorResource(R.color.input_popup_foreground),
        shape = RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            val text = remember { mutableStateOf("") }

            val doSave = {
                if (text.value != "") save(text.value)
                text.value = ""
            }
            val doEdit = {
                if (text.value != "") edit(text.value)
                text.value = ""
            }

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
                placeholder = { Text( stringResource(id = R.string.TEA_title_hint) ) }, /* "Task name" */
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { doSave() } ),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.DarkGray
                )
            )

           LaunchedEffect(null) {
               requester.requestFocus()
               /* It's hard to believe but this delay is necessary because
                  focus requester works via queue in some delayed coroutine and
                  the isFocused state is not set on return from requestFocus.
                  As a consequence soft...Input.show() is ignored because "the view is not served"
               * */
               delay(12)
               keyboardController!!.show()
            }

            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            )
            {
                IconButton(
                    modifier = Modifier.padding(8.dp,0.dp,8.dp,8.dp),  //(8.dp, 8.dp),
                    onClick = { doEdit() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_settings_24px),
                        contentDescription = "Details"
                    )
                }
                Row(horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth() )
                {
                    if (text.value == "") {
                        IconButton(
                            modifier = Modifier.padding(8.dp,0.dp,8.dp,8.dp),
                            onClick = { close() }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_clear_24px),
                                contentDescription = "Close"
                            )
                        }
                    } else {
                        IconButton(
                            modifier = Modifier.padding(8.dp,0.dp,8.dp,8.dp),
                            onClick = { doSave() }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_done_24px),
                                contentDescription = "Close"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun InputPanelPreview()
{
    PopupContent()
}