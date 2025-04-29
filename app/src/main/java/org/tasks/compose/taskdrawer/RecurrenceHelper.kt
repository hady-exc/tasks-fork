package org.tasks.compose.taskdrawer

import android.content.Context
import com.google.common.collect.Lists
import net.fortuna.ical4j.model.Recur
import org.tasks.R
import org.tasks.repeats.RecurrenceUtils.newRecur
import org.tasks.repeats.RepeatRuleToString
import timber.log.Timber

class RecurrenceHelper (
    context: Context,
    private val repeatRuleToString: RepeatRuleToString,
    val recurrence: String?
) {
    val rrule = recurrence
        .takeIf { !it.isNullOrBlank() }
        ?.let {
            try {
                newRecur(it)
            } catch (e: Exception) {
                Timber.e(e)
                null
            }
        }

    private val titles: MutableList<String> =
        Lists.newArrayList(*context.resources.getStringArray(R.array.repeat_options))
    private val ruleTitle = if (isCustomValue()) repeatRuleToString.toString(recurrence)!! else titles[5]

    fun title(index: Int, short: Boolean = false): String =
        if (short || index < 5) titles[index]
        else ruleTitle

    fun isCustomValue(): Boolean {
        if (rrule == null) {
            return false
        }
        val frequency = rrule.frequency
        return (frequency == Recur.Frequency.WEEKLY || frequency == Recur.Frequency.MONTHLY) && !rrule.dayList.isEmpty()
                || frequency == Recur.Frequency.HOURLY
                || frequency == Recur.Frequency.MINUTELY
                || rrule.until != null
                || rrule.interval > 1
                || rrule.count > 0
    }

    fun selectionIndex(): Int =
        when {
            rrule == null -> 0
            isCustomValue() -> 5
            rrule.frequency == Recur.Frequency.DAILY -> 1
            rrule.frequency == Recur.Frequency.WEEKLY -> 2
            rrule.frequency == Recur.Frequency.MONTHLY -> 3
            rrule.frequency == Recur.Frequency.YEARLY -> 4
            else -> 0
        }

    fun selectedFrequency(index: Int): Recur.Frequency =
        when (index) {
            1 -> Recur.Frequency.DAILY
            2 -> Recur.Frequency.WEEKLY
            3 -> Recur.Frequency.MONTHLY
            4 -> Recur.Frequency.YEARLY
            else -> throw IllegalArgumentException()
        }
}