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
fun PriorityChip(current: Int, setValue: (Int) -> Unit)
{
    var priorityPicker by remember { mutableStateOf(false) }
    if (priorityPicker) {
        PriorityPickerDialog(
            selected = current,
            onClick = { setValue(it); priorityPicker = false },
            onDismissRequest = { priorityPicker = false }
        )
    }

    PriorityChip(current = current, action = { priorityPicker = true })
}

@Composable
private fun PriorityChip(current: Int, action: () -> Unit)
{
    val color = Color(
        priorityColor(
            priority = current,
            isDarkMode = isSystemInDarkTheme(),
        )
    )

    IconChip(icon = priorityIcon, contentColor = color, action = action)

}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PriorityChipRedPreview()
{
    PriorityChip(current = 0, setValue = {})
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PriorityChipBluePreview()
{
    PriorityChip(current = 2, action = {})
}