package org.tasks.compose

import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.view.View
import android.widget.EditText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBindings
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.composethemeadapter.MdcTheme
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
               callback: (String) -> Unit) {
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
                PopupContent(callback)
                // Composable content to be shown in the Popup
            }
        }
    }
}

@Composable
private fun PopupContent(callback: (String) -> Unit = {}) {
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
            /*Text(
                modifier = Modifier.padding(16.dp),
                text = "Some label",
                style = MaterialTheme.typography.h6
            )*/
            TextField(
                value = text.value,
                onValueChange = { changed: String -> text.value = changed },
                modifier = Modifier
                    .fillMaxWidth()
                    //.padding(8.dp)
                    .focusRequester(requester),
                singleLine = true,
                enabled = true,
                readOnly = false,
                placeholder = { Text("Title") },
                keyboardActions = KeyboardActions(onDone =  {
                    if (text.value != "") callback(text.value)
                    text.value = ""
                }
                )
            )
            LaunchedEffect(Unit) {
                requester.requestFocus()
            }
            Row (
                modifier = Modifier.fillMaxWidth()
            )
            {
                IconButton(
                    //modifier = Modifier.padding(10.dp),
                    onClick = { }
                ) {
                    Icon(Icons.Outlined.List, contentDescription = "Details")
                }
                IconButton(
                    //modifier = Modifier.padding(10.dp),
                    onClick = { callback("This is a new task (DEBUG)") }
                ) {
                    Icon(Icons.Outlined.DoneAll, contentDescription = "Done")
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