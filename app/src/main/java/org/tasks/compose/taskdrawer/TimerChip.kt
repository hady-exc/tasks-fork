package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import org.tasks.time.DateTimeUtils2.currentTimeMillis

private val timerIcon = Icons.Outlined.Timer

@Composable
fun TimerChip(
    started: Long,
    estimated: Int,
    elapsed: Int,
    setTimer: (Boolean) -> Unit,
    setValues: (estimated: Int, elapsed: Int) -> Unit
) {
    val helper = remember { TimerHelper() }
    helper.estimated.intValue = estimated
    helper.elapsed.intValue = elapsed

    val text = when {
        started > 0 && helper.estimated.intValue > 0 -> helper.elapsedText(started) + " / " + helper.estimatedText()
        helper.elapsed.intValue > 0 -> helper.elapsedText(started)
        helper.estimated.intValue > 0 -> "00:00 / " + helper.estimatedText()
        else -> null
    }

    if (text != null) {
        Chip(
            title = text,
            leading = timerIcon,
            action = { helper.showDialog.value = true }
        )
    } else {
        IconChip(
            icon = timerIcon,
            action = { helper.showDialog.value = true }
        )
    }

    helper.Launch(started)

    TimerDialog(
        helper = helper,
        started = started,
        setTimer = setTimer,
        onDone = { setValues(helper.estimated.intValue, helper.elapsed.intValue) }
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
