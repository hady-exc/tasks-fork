package org.tasks.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import org.tasks.R

class WindowBottomPositionProvider(
    val rootView: CoordinatorLayout?
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val bottomBar = ViewBindings.findChildViewById<BottomAppBar>(rootView, R.id.bottomAppBar)
        val by = if ( bottomBar!!.isVisible ) bottomBar!!.height else 154
        //return IntOffset(0, (windowSize.height - popupContentSize.height + (bottomBar?.height ?: 150) ) )
        return IntOffset(0, (windowSize.height - popupContentSize.height + by ) )
        //return IntOffset(0, (by) )
    }
}
@Composable
fun InputPanel(control: MutableState<Boolean>?,
               rootView: CoordinatorLayout?,
               save: (String) -> Unit,
               edit: (String) -> Unit) {
    val popupVisible = control
    if ( popupVisible!!.value ) {
        MaterialTheme {
            Popup(
                popupPositionProvider = WindowBottomPositionProvider(rootView),
                onDismissRequest = { popupVisible.value = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true )
            )
            {
                PopupContent(save, { edit(it); popupVisible.value = false })
                // Composable content to be shown in the Popup
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PopupContent(save: (String) -> Unit = {},
                         edit: (String) -> Unit = {}) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        backgroundColor = Color.LightGray,
        shape = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                //.background(MaterialTheme.colors.surface)
        ) {
            val text = remember { mutableStateOf("") }
            val requester = remember { FocusRequester() }

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
                onValueChange = { changed: String -> text.value = changed },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .onFocusChanged {
                        if (it.hasFocus || it.isFocused) keyboardController!!.show()
                    }
                    .focusRequester(requester),
                singleLine = true,
                enabled = true,
                readOnly = false,
                placeholder = { Text("Title") },
                keyboardActions = KeyboardActions(onDone =  {
                    doSave()
                } ),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.LightGray,
                    focusedIndicatorColor = Color.DarkGray
                )
            )

            LaunchedEffect(Unit) {
                requester.requestFocus()
            }
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            )
            {
                IconButton(
                    modifier = Modifier.padding(8.dp, 0.dp),
                    onClick = { doEdit() }
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Details")
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                )
                {
                    IconButton(
                        modifier = Modifier.padding(8.dp, 0.dp),
                        onClick = { doSave() }
                    ) {
                        Icon(Icons.Outlined.Done, contentDescription = "Done")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 320)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 320)
@Composable
fun InputPanelPreview()
{
    PopupContent()
}