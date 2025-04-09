package org.tasks.compose.taskdrawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.common.collect.Lists
import net.fortuna.ical4j.model.Recur
import org.tasks.R
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.repeats.RepeatRuleToString
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceDialog (
    dismiss: () -> Unit,
    recurrence: String?,
    setRecurrence: (String?) -> Unit,
    repeatFromCompletion: Boolean,
    onRepeatFromChanged: (Boolean) -> Unit,
    repeatRuleToString: RepeatRuleToString,
    peekCustomRecurrence: (String?) -> Unit
) {

    fun isCustomValue(rrule: Recur?): Boolean {
        if (rrule == null) {
            return false
        }
        val frequency = rrule.frequency
        return (frequency == Recur.Frequency.WEEKLY
                || frequency == Recur.Frequency.MONTHLY) && !rrule.dayList.isEmpty()
                || frequency == Recur.Frequency.HOURLY
                || frequency == Recur.Frequency.MINUTELY
                || rrule.until != null
                || rrule.interval > 1
                || rrule.count > 0
    }
    val context = LocalContext.current
    val rrule  = recurrence
        .takeIf { !it.isNullOrBlank() }
        ?.let {
            try {
                newRecur(it)
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }

    val selected = when  {
        rrule == null  -> 0
        rrule.frequency == Recur.Frequency.DAILY -> 1
        rrule.frequency == Recur.Frequency.WEEKLY -> 2
        rrule.frequency == Recur.Frequency.MONTHLY -> 3
        rrule.frequency == Recur.Frequency.YEARLY -> 4
        isCustomValue(rrule) -> 5
        else -> 0
    }
    val repeatOptions: MutableList<String> =
        Lists.newArrayList(*context.resources.getStringArray(R.array.repeat_options))
    if (isCustomValue(rrule)) {
        repeatOptions[5] = repeatRuleToString.toString(recurrence)!!
    }

    //val selection = remember { mutableIntStateOf(selected) }
    fun setSelection(i: Int) {
        if (i == 0) {
            setRecurrence(null)
        } else if (i == 5) {
            peekCustomRecurrence(rrule.toString())
        } else {
            val frequency: Recur.Frequency =
                when (i) {
                    1 -> Recur.Frequency.DAILY
                    2 -> Recur.Frequency.WEEKLY
                    3 -> Recur.Frequency.MONTHLY
                    4 -> Recur.Frequency.YEARLY
                    else -> throw IllegalArgumentException()
                }
            setRecurrence(
                newRecur().apply {
                    interval = 1
                    setFrequency(frequency.name)
                }.toString()
            )
        }
        dismiss()
    }

    BasicAlertDialog(
        onDismissRequest = dismiss,
    ) {
        Card {
            Column (modifier = Modifier.padding(16.dp)) {
                Row {
                    Text(
                        text = stringResource(id = R.string.repeats_from),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    var expanded by remember { mutableStateOf(false) }
                    Text(
                        text = stringResource(
                            id = if (repeatFromCompletion)
                                R.string.repeat_type_completion
                            else
                                R.string.repeat_type_due
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = TextDecoration.Underline,
                        ),
                        modifier = Modifier.clickable { expanded = true },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                onRepeatFromChanged(false)
                            },
                            text = {
                                Text(
                                    text = stringResource(id = R.string.repeat_type_due),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        )
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                onRepeatFromChanged(true)
                            },
                            text = {
                                Text(
                                    text = stringResource(id = R.string.repeat_type_completion),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        )
                    }
                }
                for (i in 0..5) {
                    SelectableText(
                        text = repeatOptions[i],
                        index = i,
                        selected = selected,
                        setSelection = { setSelection(i) }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableText (
    text: String,
    index: Int,
    selected: Int,
    setSelection: (Int) -> Unit
) {
    Row (verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = index == selected,
            onClick = { setSelection(index) }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text)
    }
}