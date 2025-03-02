package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.kmp.org.tasks.themes.ColorProvider.priorityColor

private val priorityIcon = Icons.Outlined.Flag

@Composable
fun PriorityChip(current: Int, setValue: (Int) -> Unit, dialogStarted: (Boolean) -> Unit)
{
    var priorityPicker by remember { mutableStateOf(false) }
    if (priorityPicker) {
        PriorityPickerDialog(
            selected = current,
            onClick = { setValue(it); dialogStarted(false); priorityPicker = false },
            onDismissRequest = { dialogStarted(false); priorityPicker = false }
        )
    }

    PriorityChip(current) { dialogStarted(true); priorityPicker = true }
}

@Composable
private fun PriorityChip(current: Int, action: () -> Unit)
{
    val color = Color(
        priorityColor(
            priority = current,
            isDarkMode = isSystemInDarkTheme(),
            desaturate = false, // TODO -- get from preferences
        )
    )

    IconChip(icon = priorityIcon, iconColor = color, action = action)

}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PriorityChipRedPreview()
{
    PriorityChip(0,{})
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PriorityChipBluePreview()
{
    PriorityChip(2,{})
}