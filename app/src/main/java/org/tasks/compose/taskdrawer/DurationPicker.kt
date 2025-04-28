package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val upIcon = Icons.Outlined.ArrowDropUp
private val downIcon = Icons.Outlined.ArrowDropDown

@Composable
fun DurationPicker(
    current: Int,
    setValue: (Int) -> Unit = {}
) {

    val hours = current / 3600
    val minutes = current / 60 % 60
    val MAX_VALUE: Int = 1000*3600 - 1

    fun change(delta: Int) {
        var newValue = current + delta
        if (newValue > MAX_VALUE) {
            newValue = newValue - (MAX_VALUE+1)
            assert(newValue in 0.. MAX_VALUE )
        } else if (newValue < 0 ) {
            newValue = 0
        }
        setValue(newValue)
    }

    Row (
        modifier = Modifier.wrapContentSize().padding(12.dp)
    ) {
        TColumn {
            TCell (onClick = { change(3600) }) { Icon(upIcon,null) }
            TCell (height = 36.dp) { TText(String.format("%d", hours)) }
            TCell (onClick = { change(-3600) }) { Icon(downIcon,null) }
        }
        TColumn {
            TCell { Spacer(Modifier.size(4.dp)) }
            TCell (height = 36.dp) { TText(":") }
            TCell { Spacer(Modifier.size(4.dp)) }
        }
        TColumn {
            TCell (onClick = { change(300) }) { Icon(upIcon,null) }
            TCell (height = 36.dp) { TText(String.format("%02d", minutes)) }
            TCell (onClick = { change(-300) }) { Icon(downIcon,null) }
        }
    }
}

@Composable
private fun TCell(
    height: Dp = 18.dp,
    onClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) =
    Row (
        modifier = Modifier
            .requiredHeight(height)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )

@Composable
private fun TColumn(content: @Composable ColumnScope.() -> Unit) =
    Column (
        modifier = Modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )

@Composable
fun TText(text: String) =
    Text(text = text, style = MaterialTheme.typography.headlineMedium)

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun DurationPickerPreview() {
    DurationPicker(3900,{})
}