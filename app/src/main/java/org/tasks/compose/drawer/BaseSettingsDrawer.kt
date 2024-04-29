package org.tasks.compose.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import org.tasks.themes.CustomIcons

data class BaseSettingsDrawerParam (
    val title: String,
    val isNew: Boolean,
    val text: String,
    val color: Int,
    val icon: Int
)

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
            Surface (elevation = 8.dp, modifier = Modifier.requiredHeight(56.dp))
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
                        fontSize = 18.sp,
                        modifier = Modifier
                            .weight(0.8f)
                            .padding(start = textsPaddingLeft, bottom = 9.dp)
                    )
                    if (!param.isNew)
                        Box(modifier = Modifier.align(Alignment.Bottom)) {
                            DeleteButton() {} /* TODO(setup delete action) */
                        }
                    }
            }
            Column ( modifier = Modifier.padding(horizontal = 6.dp) ){
                Row(verticalAlignment = Alignment.CenterVertically)
                {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = param.text,
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent
                        ),
                        label = { Text(stringResource(id = R.string.display_name)) },
                        onValueChange = { }
                    )
                }
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


