package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import org.tasks.time.DateTimeUtils2.currentTimeMillis
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
    val state = remember(estimated, elapsed) {
        TimerChipState(estimated, elapsed)
    }

    val text = 
        if (started == 0L && state.estimated.intValue == 0 && state.elapsed.intValue == 0)
            null
        else
            state.elapsedText(started) + "/" + state.estimatedText()

    if (text != null) {
        Chip(
            title = text,
            leading = timerIcon,
            action = { state.showDialog.value = true }
        )
    } else {
        IconChip(
            icon = timerIcon,
            action = { state.showDialog.value = true }
        )
    }

    LaunchedEffect(started > 0, state) {
        while (true) {
            delay(1.seconds)
            state.now.longValue = currentTimeMillis()
        }
    }

    TimerDialog(
        state = state,
        started = started,
        setTimer = setTimer,
        onDone = {
            setValues(state.estimated.intValue, state.elapsed.intValue)
        }
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
