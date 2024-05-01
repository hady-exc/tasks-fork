package org.tasks.compose.drawer

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.compose.DeleteButton
import org.tasks.compose.border
import org.tasks.themes.CustomIcons

data class BaseSettingsDrawerParam (
    val title: String,
    val isNew: Boolean,
    val text: String,
    val color: Int,
    val icon: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseSettingsDrawer(
    title: String,
    isNew: Boolean,
    text: MutableState<String>,
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
        Column(
            modifier = Modifier
                .background(colorResource(id = R.color.window_background))
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            /* Toolbar */
            Surface (shadowElevation = 8.dp, modifier = Modifier.requiredHeight(56.dp))
            {
                Row(
                    verticalAlignment = Alignment.Bottom,
                )
                {
                    IconButton( onClick = save ) {
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
                            .padding(start = textsPaddingLeft, bottom = 12.dp)
                    )
                    if (!isNew)
                        Box(modifier = Modifier.align(Alignment.Bottom)) {
                            DeleteButton(onClick = delete)
                        }
                    }
            } /* end Toolbar*/
            Column ( modifier = Modifier.padding(horizontal = 0.dp) ){

                /*  text input */
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Column {
                        val color = remember { mutableStateOf(Color.Gray) }
                        Text(
                            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                            text = stringResource(id = R.string.display_name),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = color.value
                        )
                        BasicTextField(
                            value = text.value,
                            onValueChange = { text.value = it },
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .onFocusChanged { state ->
                                    color.value = if (state.hasFocus) Color.Red else Color.Gray
                                }
                        )
                        HorizontalDivider(
                            color = color.value,
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
                            val borderColor = colorResource(R.color.text_tertiary)
                            Canvas(modifier = Modifier.size(24.dp)) {
                                drawCircle( color = color.value )
                                drawCircle(
                                    color = borderColor,
                                    style = Stroke(width = 3f)
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
                        IconButton( onClick = clearColor ) {
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
                            contentDescription = null
                        )
                    }
                    Text(
                        text = LocalContext.current.getString(R.string.icon),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(0.8f)
                            .padding(start = textsPaddingLeft)
                    )
                } /* end icon selection */
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
