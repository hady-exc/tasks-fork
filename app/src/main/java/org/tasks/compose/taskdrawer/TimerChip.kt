package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

private val timerIcon = Icons.Outlined.Timer

@Composable
fun TimerChip(
    started: Long,
    estimated: Int,
    elapsed: Int,
    setTimer: (Boolean) -> Unit,
    setValues: (estimated: Int, elapsed: Int) -> Unit
) {
    val showDialog = remember { mutableStateOf(false) }
    val now = remember { mutableLongStateOf(started) }

    val text = 
        if (started == 0L && estimated == 0 && elapsed == 0)
            null
        else {
            val elapsed = elapsed +
                    if (started > 0L) ((now.longValue - started) / 1000).toInt() else 0
            DateUtils.formatElapsedTime(elapsed.toLong()) +
            "/" +
            DateUtils.formatElapsedTime(estimated.toLong())
        }

    if (text != null) {
        Chip(
            title = text,
            leading = timerIcon,
            action = { showDialog.value = true }
        )
    } else {
        IconChip(
            icon = timerIcon,
            action = { showDialog.value = true }
        )
    }

    LaunchedEffect(started) {
        while (started > 0L) {
            delay(1.seconds)
            now.longValue = currentTimeMillis()
        }
    }

    TimerDialog(
        show = showDialog.value,
        started = started,
        estimated = estimated,
        elapsed = elapsed,
        setResult = { started, estimated, elapsed ->
            setValues(estimated, elapsed)
            setTimer(started > 0L)
            now.longValue = started
        },
        close = { showDialog.value = false }
    )
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerChipEmpty() {
    TimerChip(
        started = 0L,
        elapsed = 0,
        estimated = 0,
        setTimer = {},
        setValues = { est, elp ->  }
    )
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerChipEstimated() {
    TimerChip(
        started = 0L,
        elapsed = 0,
        estimated = 615,
        setTimer = {},
        setValues = { est, elp ->  }
    )
}

@Composable
@Preview(showBackground = true, locale = "rus")
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerChipStarted() {
    TimerChip(
        started = currentTimeMillis()+1234567,
        elapsed = 123,
        estimated = 615,
        setTimer = {},
        setValues = { est, elp ->  }
    )
}
