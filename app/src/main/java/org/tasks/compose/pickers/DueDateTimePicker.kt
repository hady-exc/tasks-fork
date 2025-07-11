package org.tasks.compose.pickers

import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.tasks.data.entity.Task
import org.tasks.date.DateTimeUtils.newDateTime
import org.tasks.date.DateTimeUtils.toDateTime
import org.tasks.dialogs.DateTimePicker.Companion.MULTIPLE_TIMES
import org.tasks.extensions.Context.is24HourFormat
import org.tasks.preferences.Preferences
import org.tasks.time.DateTime
import org.tasks.time.millisOfDay
import org.tasks.time.startOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateTimePicker(
    sheetState: SheetState,
    current: Long,
    updateCurrent: (Long) -> Unit,
    accept: () -> Unit,
    dismiss: () -> Unit,
    autoclose: Boolean,
    showNoDate: Boolean,
    setTimeDisplayMode: (DisplayMode) -> Unit,
    setDateDisplayMode: (DisplayMode) -> Unit
) {

    val context = LocalContext.current
    val preferences = remember { Preferences(context) }
    val is24Hour = remember { context.is24HourFormat }
    val datePickerState = rememberDatePickerState(
        initialDisplayMode = remember { preferences.calendarDisplayMode },
    )
    val today = remember { newDateTime().startOfDay() }

    var selectedDay  by remember { mutableLongStateOf(current.startOfDay()) }
    var selectedTime by remember { mutableIntStateOf(current.millisOfDay) }

    fun sendSelected() {
        val dateTime = when {
            selectedDay == 0L -> 0L
            selectedTime == 0 -> selectedDay
            else -> selectedDay.toDateTime().withMillisOfDay(selectedTime).millis
        }
        updateCurrent(dateTime)
        dismiss()
    }

    fun returnDate(day: Long = selectedDay, time: Int = selectedTime) {
        selectedDay = day
        selectedTime = time
        if (autoclose) {
            sendSelected()
        }
    }

    fun returnSelectedTime(millisOfDay: Int) {
        val day = when {
            millisOfDay == 0 -> selectedDay
            selectedDay > 0 -> selectedDay
            today.withMillisOfDay(millisOfDay).isAfterNow -> today.millis
            else -> today.plusDays(1).millis
        }
        returnDate(day = day, time = millisOfDay)
    }

    DatePickerBottomSheet(
        sheetState = sheetState,
        state = datePickerState,
        showButtons = !autoclose,
        setDisplayMode = setDateDisplayMode,
        cancel = dismiss,
        accept = { sendSelected(); accept() },
        dateShortcuts = {
            DueDateShortcuts(
                today = today.millis,
                tomorrow = remember { today.plusDays(1).millis },
                nextWeek = remember { today.plusDays(7).millis },
                selected = selectedDay,
                showNoDate = showNoDate,
                selectedDay = { returnDate(it.startOfDay(), selectedTime) },
                clearDate = { returnDate(0, 0) },
            )
        },
        timeShortcuts = {
            var showTimePicker by rememberSaveable {
                mutableStateOf(
                    false
                )
            }
            if (showTimePicker) {
                val time = if (selectedTime == MULTIPLE_TIMES
                    || !Task.hasDueTime(
                        today.withMillisOfDay(
                            selectedTime
                        ).millis
                    )
                ) {
                    today.noon().millisOfDay
                } else {
                    selectedTime
                }
                TimePickerDialog(
                    state = rememberTimePickerState(
                        initialHour = time / (60 * 60_000),
                        initialMinute = (time / (60_000)) % 60,
                        is24Hour = is24Hour,
                    ),
                    initialDisplayMode = remember { preferences.timeDisplayMode },
                    setDisplayMode = setTimeDisplayMode,
                    selected = { returnSelectedTime(it + 1000) },
                    dismiss = { showTimePicker = false },
                )
            }
            TimeShortcuts(
                day = 0,
                selected = selectedTime,
                morning = remember { preferences.dateShortcutMorning + 1000 },
                afternoon = remember { preferences.dateShortcutAfternoon + 1000 },
                evening = remember { preferences.dateShortcutEvening + 1000 },
                night = remember { preferences.dateShortcutNight + 1000 },
                selectedMillisOfDay = { returnSelectedTime(it) },
                pickTime = { showTimePicker = true },
                clearTime = { returnSelectedTime(0) },
            )
        }
    )
    LaunchedEffect(selectedDay) {
        if (selectedDay > 0) {
            (selectedDay + (DateTime(selectedDay).offset)).let {
                datePickerState.displayedMonthMillis = it
                datePickerState.selectedDateMillis = it
            }
        } else {
            datePickerState.selectedDateMillis = null
        }
    }
    LaunchedEffect(datePickerState.selectedDateMillis) {
        if (datePickerState.selectedDateMillis == selectedDay + (DateTime(selectedDay).offset)) {
            return@LaunchedEffect
        }
        datePickerState.selectedDateMillis?.let {
            returnDate(day = it - DateTime(it).offset)
        }
    }
}


