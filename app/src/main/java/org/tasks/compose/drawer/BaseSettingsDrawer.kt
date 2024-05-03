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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.composethemeadapter.MdcTheme
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

    val textsPaddingLeft = 18.dp

    MdcTheme {
        ProvideTextStyle( LocalTextStyle.current.copy(fontSize = 22.sp) ) {
            Surface(color = colorResource(id = R.color.window_background)) {
                val fontSize = 24.sp
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    /* Toolbar */
                    Surface(elevation = 3.dp, modifier = Modifier.requiredHeight(56.dp))
                    {
                        Row(
                            verticalAlignment = Alignment.Bottom,
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
                                modifier = Modifier
                                    .weight(0.8f)
                                    .paddingFromBaseline(bottom = 15.dp)
                                    .padding(start = textsPaddingLeft)
                            )
                            if (!isNew)
                                Box(modifier = Modifier.align(Alignment.Bottom)) {
                                    DeleteButton(onClick = delete)
                                }
                        }
                    } /* end Toolbar*/

                    Column(modifier = Modifier.padding(horizontal = 0.dp)) {
                        /*  text input */
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            val normalColor = colorResource(R.color.text_primary)
                            val errorColor =
                                colorResource(R.color.red_a400)  /* TODO(find correct accent color *) */
                            Column {
                                val labelColor = if (error.value == "") normalColor else errorColor
                                val labelText =
                                    if (error.value != "") error.value else stringResource(R.string.display_name)

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
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                                Divider(
                                    color = labelColor,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        } /* end text input */

                        /* color selection */
                        Row(verticalAlignment = Alignment.CenterVertically)
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
                                    .padding(start = textsPaddingLeft)
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
                        Row(verticalAlignment = Alignment.CenterVertically)
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
