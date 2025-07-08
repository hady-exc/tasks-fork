package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.runBlocking
import org.tasks.compose.pickers.DatePickerDialog
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDay
import org.tasks.time.DateTime
import timber.log.Timber

private val dueDateIcon = Icons.Outlined.Schedule


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateChip(
    current: Long,
    setValue: (Long) -> Unit
) {
    Timber.d("**** RECOMPOSE DueDateChip")

    var datePicker by remember { mutableStateOf(false) }
    if (datePicker) {
        DatePickerDialog(
            initialDate = if (current != 0L) current else newDateTime().startOfDay().millis,
            displayMode = DisplayMode.Picker,
            setDisplayMode = {},
            selected = { setValue(it); datePicker = false },
            dismiss = { datePicker = false } )
    }

    DueDateChip(
        current = current,
        action = { datePicker = true },
        delete = { setValue(0L) }
    )

}

@Composable
private fun DueDateChip(
    current: Long,
    action: () -> Unit,
    delete: (() -> Unit)?
) {
    if (current != 0L) {
        Chip(
            title = runBlocking {
                getRelativeDay(
                    date = current,
                    style = DateStyle.SHORT
                )
            },
            leading = dueDateIcon,
            action = action,
            delete = delete
        )
    } else {
        IconChip(icon = dueDateIcon, action = action)
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun DueDateChipPreview()
{
    DueDateChip(current = 345678912345L, action = {}, delete = { })
}

