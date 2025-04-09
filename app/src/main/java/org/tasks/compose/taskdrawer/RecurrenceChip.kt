package org.tasks.compose.taskdrawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.common.collect.Lists
import net.fortuna.ical4j.model.Recur
import org.tasks.R
import org.tasks.repeats.RecurrenceUtils.newRecur
import timber.log.Timber

private val repeatIcon = Icons.Outlined.Repeat

@Composable
fun RecurrenceChip (
    recurrence: String?,
    onClick: () -> Unit
) {
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
    val selected = when  {
        rrule == null  -> 0
        rrule.frequency == Recur.Frequency.DAILY -> 1
        rrule.frequency == Recur.Frequency.WEEKLY -> 2
        rrule.frequency == Recur.Frequency.MONTHLY -> 3
        rrule.frequency == Recur.Frequency.YEARLY -> 4
        isCustomValue(rrule) -> 5
        else -> 0
    }
    val context = LocalContext.current
    val repeatOptions: MutableList<String> =
        Lists.newArrayList(*context.resources.getStringArray(R.array.repeat_options))
    val title = repeatOptions[selected]

    if (rrule == null) {
        IconChip(icon = repeatIcon, action = onClick)
    } else {
        Chip(
            title = title,
            leading = repeatIcon,
            action = onClick
        )
    }
}