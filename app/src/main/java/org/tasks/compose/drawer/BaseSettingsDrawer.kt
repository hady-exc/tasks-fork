package org.tasks.compose.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.composethemeadapter.MdcTheme
import org.tasks.R
import org.tasks.compose.DeleteButton
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
fun BaseSettingsDrawer(param: BaseSettingsDrawerParam)
{

    val textsPaddingLeft = 18.dp

    MdcTheme {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize(),
                //.padding(6.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Surface (shadowElevation = 8.dp, modifier = Modifier.requiredHeight(56.dp))
            {
                Row(
                    verticalAlignment = Alignment.Bottom,
                )
                {
                    IconButton( onClick = { TODO() } ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_outline_save_24px),
                            contentDescription = stringResource(id = R.string.save),
                        )
                    }
                    Text(
                        text = param.title,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(0.8f)
                            .padding(start = textsPaddingLeft, bottom = 12.dp)
                    )
                    if (!param.isNew)
                        Box(modifier = Modifier.align(Alignment.Bottom)) {
                            DeleteButton{ TODO() }
                        }
                    }
            }
            Column ( modifier = Modifier.padding(horizontal = 0.dp) ){

                val text = remember { mutableStateOf(param.text) }
                val color = remember { mutableStateOf(Color.Red) }

                Row(
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Column {
                        Text(
                            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                            text = stringResource(id = R.string.display_name),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        BasicTextField(
                            value = param.text,
                            onValueChange =
                            {
                                text.value = it
                                if (it.trim() == "") color.value = Color.Gray else color.value = Color.Red
                            },
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Divider(
                            color = color.value,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
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
                Row(verticalAlignment = Alignment.CenterVertically)
                {
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_outline_not_interested_24px),
                            contentDescription = null
                        )
                    }
                    Text(
                        text = LocalContext.current.getString(R.string.color),
                        modifier = Modifier
                            .weight(0.8f)
                            .padding(start = textsPaddingLeft)
                    )
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_outline_clear_24px),
                            contentDescription = null
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically)
                {
                    val icon = CustomIcons.getIconResId(param.icon) ?: CustomIcons.getIconResId(
                        CustomIcons.LABEL
                    )
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(icon!!),
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
                }
            }
        }
    }
}

@Composable
@Preview
fun BaseSettingsDrawerPreview () {
    BaseSettingsDrawer(BaseSettingsDrawerParam("Create New Tag", false, "Tag Name", R.color.blue_500, R.drawable.ic_round_icon))
}


