package org.tasks.compose.taskdrawer

import android.text.format.DateUtils
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf

class TimerChipState (
    estimated: Int = 0,
    elapsed: Int = 0
) {
    val estimated = mutableIntStateOf(estimated)
    val elapsed = mutableIntStateOf(elapsed)
    val showDialog = mutableStateOf(false)
    val now = mutableLongStateOf(0L)

    private fun currentElapsed(started: Long): Int {
        var newValue = elapsed.intValue
        if (started > 0) {
            newValue += ((now.longValue - started) / 1000L).toInt()
        }
        return newValue
    }

    fun elapsedText(started: Long): String = DateUtils.formatElapsedTime(currentElapsed(started).toLong())

    fun estimatedText(): String =
        DateUtils.formatElapsedTime(estimated.intValue.toLong())

}