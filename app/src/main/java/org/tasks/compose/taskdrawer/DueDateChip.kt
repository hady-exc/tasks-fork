package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.runBlocking
import org.tasks.R
import org.tasks.compose.pickers.DueDateTimePicker
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.kmp.org.tasks.time.DateStyle
import org.tasks.kmp.org.tasks.time.getRelativeDateTime
import org.tasks.preferences.Preferences

private val dueDateIcon = Icons.Outlined.Schedule


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateChip(
    current: Long,
    setValue: (Long) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { Preferences(context) }

    var dateTimePicker by remember { mutableStateOf(false) }
    if (dateTimePicker) {
        DueDateTimePicker(
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            current = current,
            updateCurrent = setValue,
            accept = { dateTimePicker = false },
            dismiss = { dateTimePicker = false },
            autoclose = preferences.getBoolean(
                R.string.p_auto_dismiss_datetime_list_screen, false
            ),
            showNoDate = true,  // true is possible only in call from the main menu command
            setDateDisplayMode = { preferences.calendarDisplayMode = it },
            setTimeDisplayMode = { preferences.timeDisplayMode = it }
        )
    }

    DueDateChip(
        current = current,
        action = { dateTimePicker = true },
        delete = { setValue(0L) }
    )

}

@Composable
private fun DueDateChip(
    current: Long,
    action: () -> Unit,
    delete: (() -> Unit)?
) {
    val context = LocalContext.current
    if (current != 0L) {
        Chip(
            title = runBlocking {
                getRelativeDateTime(
                    date = current,
                    is24HourFormat = context.is24HourFormat,
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

