package org.tasks.compose.pickers

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.themes.TasksTheme
import org.tasks.time.DateTime
import java.util.Calendar.FRIDAY
import java.util.Calendar.MONDAY
import java.util.Calendar.SATURDAY
import java.util.Calendar.SUNDAY
import java.util.Calendar.THURSDAY
import java.util.Calendar.TUESDAY
import java.util.Calendar.WEDNESDAY

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: Long,
    selected: (Long) -> Unit,
    dismiss: () -> Unit,
) {
    TasksTheme {
        val initialDateUTC by remember(initialDate) {
            derivedStateOf {
                DateTime(initialDate).let { it.millis + it.offset }
            }
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateUTC,
        )

        androidx.compose.material3.DatePickerDialog(
            modifier = Modifier.wrapContentSize(),
            onDismissRequest = { dismiss() },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState
                            .selectedDateMillis
                            ?.let { selected(it - DateTime(it).offset) }
                        dismiss()
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            }
        ) {
            val dateFormatter = remember { DatePickerDefaults.dateFormatter() }
            DatePicker(
                state = datePickerState,
                dateFormatter = dateFormatter,
                title = { TitleMenu() }
            )
        }
    }
}

@Composable
fun TitleMenu() {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, bottom = 12.dp, end = 24.dp, top = 20.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Column (modifier = Modifier.weight(1f)) {
            Label(stringResource(R.string.today))
            Label(stringResource(R.string.tomorrow))
        }
        Column (modifier = Modifier.weight(1f)) {
            Label(
                stringResource(
                    when (newDateTime().plusWeeks(1).dayOfWeek) {
                        SUNDAY -> R.string.next_sunday
                        MONDAY -> R.string.next_monday
                        TUESDAY -> R.string.next_tuesday
                        WEDNESDAY -> R.string.next_wednesday
                        THURSDAY -> R.string.next_thursday
                        FRIDAY -> R.string.next_friday
                        SATURDAY -> R.string.next_saturday
                        else -> throw IllegalArgumentException()
                    }
                )
            )
            Label(stringResource(R.string.no_date))
        }
        Column (
            modifier = Modifier.padding(start = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.padding(bottom = 8.dp))
            Text(stringResource(R.string.shortcut_pick_time))
        }

    }
}

@Composable
fun Label(
    text: String,
    icon: ImageVector = Icons.Outlined.WbSunny
) {
    Row (
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.Bottom
    ){
        Icon(icon, null, modifier = Modifier.padding(end = 4.dp))
        Text(
            text = text,
            maxLines = 1,
            //style = MaterialTheme.typography.labelMedium,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DatePickerPreview() {
    TasksTheme {
        DatePickerDialog(
            initialDate = DateTime().plusDays(1).millis,
            selected = {},
            dismiss = {}
        )
    }
}