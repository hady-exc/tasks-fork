package org.tasks.compose.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
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
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBindings
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.composethemeadapter.MdcTheme
import kotlinx.coroutines.delay
import org.tasks.R

/*
* Aligns the popup bottom with the bottom of the coordinator_layout
* which is aligned with the top of the IME by the system
*/
private class WindowBottomPositionProvider(
    val rootViewBottomY: Int    /* positioning anchor point */
) : PopupPositionProvider {

    /*
    * Aligns the popup bottom with the bottom of the coordinator_layout
    * which is aligned with the top of the IME by the system
    */
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return IntOffset(0, rootViewBottomY - popupContentSize.height )
    }
}
@Composable
fun InputPanel(showPopup: MutableState<Boolean>,
               rootView: CoordinatorLayout,
               switchOff: () -> Unit,
               save: (String) -> Unit,
               edit: (String) -> Unit )
{
    val fadeColor = colorResource(R.color.input_popup_foreground).copy(alpha = 0.12f)
    val getViewY: (view: CoordinatorLayout) -> Int = {
        val rootViewXY = intArrayOf(0,0)
        rootView.getLocationOnScreen(rootViewXY)
        rootView.height + rootViewXY[1]   /* rootViewXY[1] == rootView.y */
    }

    if ( showPopup.value ) {
        MdcTheme {
            Popup(
                popupPositionProvider = WindowBottomPositionProvider(remember { getViewY(rootView) }),
                onDismissRequest = switchOff,
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true,
                    clippingEnabled = false )
            ) {
                AnimatedVisibility(
                    visible = showPopup.value,
                    enter = fadeIn() + expandVertically( expandFrom = Alignment.Bottom),
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
private fun PopupContent(save: (String) -> Unit = {},
                         edit: (String) -> Unit = {},
                         close: () -> Unit = {}) {

    val keyboardController = LocalSoftwareKeyboardController.current
    val requester = remember { FocusRequester() }
    val background = colorResource(id = R.color.input_popup_background)
    val foreground = colorResource(id = R.color.input_popup_foreground)
    val padding = keyboardHeight()

    Card(
        //modifier = Modifier.padding(bottom = padding.value),
        backgroundColor = background,
        contentColor = foreground,
        shape = RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp),
        //border = BorderStroke( Dp.Hairline, foreground ),
        elevation = 16.dp
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
                placeholder = { Text( stringResource(id = R.string.TEA_title_hint) ) }, /* "Task name" */
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                keyboardActions = KeyboardActions(onDone = { doSave() } ),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = foreground,
                    placeholderColor = foreground
                )
            )

           LaunchedEffect(null) {
               requester.requestFocus()
               /* The delay below is a trick necessary because
                  focus requester works via queue in some delayed coroutine and
                  the isFocused state is not set yet on return from requestFocus.
                  As a consequence soft...Input.show() is ignored because "the view is not served"
                  The 12ms delay is not the guarantee but makes it working almost always
               * */
               delay(30)
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
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(padding.value))
        }
    }
}

@Composable
fun keyboardHeight(): State<Dp> {
    with ( LocalDensity.current ) {
        return rememberUpdatedState( WindowInsets.ime.getBottom(LocalDensity.current).toDp() )
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
fun InputPanelPreview()
{
    PopupContent()
}