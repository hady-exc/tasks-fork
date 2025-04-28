package org.tasks.compose.taskdrawer

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import kotlin.time.Duration.Companion.seconds

class TimerHelper {
    val estimated = mutableIntStateOf(0)
    val elapsed = mutableIntStateOf(0)
    val showDialog = mutableStateOf(false)
    val now = mutableLongStateOf(0L)

    fun elapsedText(started: Long): String {
        val newElapsed =
            elapsed.intValue + (if (started > 0) now.longValue - started / 1000 else 0)
        return if (newElapsed >= 0) DateUtils.formatElapsedTime(newElapsed.toLong()) else ""

    }

    fun estimatedText(): String =
        DateUtils.formatElapsedTime(estimated.intValue.toLong())

    @Composable
    fun Launch(started: Long) {
        LaunchedEffect(key1 = started) {
            while (started > 0) {
                delay(1.seconds)
                now.longValue = currentTimeMillis()
            }
        }
    }
}