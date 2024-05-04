package org.tasks.compose.drawer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.composethemeadapter.MdcTheme
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.compose.DeleteButton

@Composable
fun BaseSettingsDrawer(
    title: String,
    isNew: Boolean,
    text: MutableState<String>,
    error: MutableState<String>,
    color: State<Color>,
    icon: State<Int>,
    save: () -> Unit,
    delete: () -> Unit,
    selectIcon: () -> Unit,
    clearColor: () -> Unit,
    selectColor: () -> Unit
) {

    val textsPaddingLeft = 20.dp

    MdcTheme {
        ProvideTextStyle( LocalTextStyle.current.copy(fontSize = 18.sp) ) {
            Surface(
                color = colorResource(id = R.color.window_background),
                contentColor = colorResource(id = R.color.text_primary)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    /* Toolbar */
                    Surface(elevation = 4.dp, modifier = Modifier.requiredHeight(56.dp))
                    {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        {
                            IconButton(onClick = save) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_outline_save_24px),
                                    contentDescription = stringResource(id = R.string.save),
                                )
                            }
                            Text(
                                text = title,
                                fontWeight = FontWeight.Medium,
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .weight(0.8f)
                                    .paddingFromBaseline(bottom = 16.dp)
                                    .padding(start = textsPaddingLeft)
                            )
                            if (!isNew) DeleteButton(onClick = delete)
                        }
                    } /* end Toolbar*/

                    Column(modifier = Modifier.padding(horizontal = 0.dp)) {

                        SimpleTextInput(
                            text = text,
                            error = error,
                            requestKeyboard = isNew,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        /* color selection */
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        {
                            IconButton(
                                onClick = { selectColor() }
                            ) {
                                if (color.value == Color.Unspecified) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_outline_not_interested_24px),
                                        tint = colorResource(R.color.icon_tint_with_alpha),
                                        contentDescription = null
                                    )
                                } else {
                                    val borderColor =
                                        colorResource(R.color.icon_tint_with_alpha)// colorResource(R.color.text_tertiary)
                                    Canvas(modifier = Modifier.size(24.dp)) {
                                        drawCircle(color = color.value)
                                        drawCircle(
                                            color = borderColor,
                                            style = Stroke(width = 4.0f)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = LocalContext.current.getString(R.string.color),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .padding(start = textsPaddingLeft, bottom = 12.dp)
                            )
                            if (color.value != Color.Unspecified) {
                                IconButton(onClick = clearColor) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_outline_clear_24px),
                                        contentDescription = null
                                    )
                                }
                            }
                        } /* end color selection */

                        /* icon selection */
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        {
                            IconButton(onClick = selectIcon) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(icon.value),
                                    contentDescription = null,
                                    tint = colorResource(R.color.icon_tint_with_alpha)
                                )
                            }
                            Text(
                                text = LocalContext.current.getString(R.string.icon),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .padding(start = textsPaddingLeft)
                            )
                        } /* end icon selection */
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleTextInput(
    text: MutableState<String>,
    error: MutableState<String>,
    requestKeyboard: Boolean,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.display_name),
    errorState: Color = colorResource(R.color.red_a400),  /* TODO(find correct accent color *) */
    activeState: Color = LocalContentColor.current.copy(alpha = 0.75f),
    inactiveState: Color = LocalContentColor.current.copy(alpha = 0.5f),
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val requester = remember { FocusRequester() }

    /*  text input */
    Row(
        modifier = modifier
    ) {
        Column {
            val focused = remember { mutableStateOf( false ) }
            val labelColor = when {
                (error.value != "") -> errorState
                (focused.value) -> activeState
                else -> inactiveState
            }
            val labelText = if (error.value != "") error.value else label
            Text(
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                text = labelText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor
            )

            BasicTextField(
                value = text.value,
                textStyle = TextStyle(
                    fontSize = LocalTextStyle.current.fontSize,
                    color = LocalContentColor.current
                ),
                onValueChange = {
                    text.value = it
                    if (error.value != "") error.value = ""
                },
                cursorBrush = SolidColor(LocalContentColor.current),
                modifier = Modifier
                    .padding(bottom = 3.dp)
                    .focusRequester(requester)
                    .onFocusChanged {focused.value = (it.isFocused) }
            )
            Divider(
                color = labelColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    } /* end text input */

    if (requestKeyboard) {
        LaunchedEffect(null) {
            requester.requestFocus()

            /* Part of requester.requestFocus logic is performed in separate coroutine,
            so the actual view may not be really focused right upon return
            from it, what makes the subsequent "IME.show" call to be ignored by the system.
            The delay below is a workaround trick for it.
            30ms period is not the guarantee but makes it working almost always */
            delay(30)

            keyboardController!!.show()
        }
    }

}

@Composable
@Preview
fun BaseSettingsDrawerPreview () {
    BaseSettingsDrawer(
        title ="Create New Tag",
        isNew = false,
        text = remember { mutableStateOf("Tag Name") },
        error = remember { mutableStateOf("") },
        color = remember { mutableStateOf(Color.Red) },
        icon = remember { mutableStateOf(R.drawable.ic_outline_label_24px) },
        delete = {},
        save = {},
        selectColor = { Color.Red },
        clearColor = { },
        selectIcon = { 1 }
    )
}


/*
                Row(verticalAlignment = Alignment.CenterVertically)
                {
                    TextField(
                        value = param.text,
                        onValueChange = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        label = { Text(stringResource(id = R.string.display_name)) }
                    )
                }
*/
