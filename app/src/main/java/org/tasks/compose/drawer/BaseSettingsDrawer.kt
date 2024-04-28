package org.tasks.compose.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
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
internal fun BaseSettingsDrawer(param: BaseSettingsDrawerParam)
{
    MdcTheme {
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically)
            {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_outline_save_24px),
                    contentDescription = "Save",
                    modifier = Modifier.padding(6.dp)
                )
                Text(
                    text = param.title,
                    fontSize = 24.sp,
                    modifier = Modifier.weight(0.8f)
                )
                if (!param.isNew)
                    DeleteButton() {} /* TODO(setup delete action) */
            }
            Row (verticalAlignment = Alignment.CenterVertically)
            {
                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = param.text,
                    onValueChange = { }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically)
            {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_outline_not_interested_24px),
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp)
                )
                Text(
                    text = LocalContext.current.getString(R.string.color),
                    modifier = Modifier.weight(0.8f)
                )
                Icon (
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_outline_clear_24px),
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically)
            {
                val icon = CustomIcons.getIconResId(param.icon) ?: CustomIcons.getIconResId(
                    CustomIcons.LABEL)
                Icon (
                    imageVector = ImageVector.vectorResource(icon!!),
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp)
                )
                Text(
                    text = LocalContext.current.getString(R.string.icon),
                    modifier = Modifier.weight(0.8f)
                )
            }
        }
    }
}

@Composable
@Preview
fun BaseSettingsDrawerPreview () {
    BaseSettingsDrawer(BaseSettingsDrawerParam("Title", false, "Tag Name", R.color.blue_500, R.drawable.ic_round_icon))
}


