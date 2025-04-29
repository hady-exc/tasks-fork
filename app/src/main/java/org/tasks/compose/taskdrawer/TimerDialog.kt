package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import kotlin.time.Duration.Companion.seconds

@Composable
fun TimerDialog(
    show: Boolean,
    started: Long,
    estimated: Int,
    elapsed: Int,
    setResult: (started: Long, estimated: Int, elapsed: Int) -> Unit,
    close: () -> Unit
) {
    if (show) {
        val now = remember { mutableLongStateOf(started+elapsed)}
        val started = remember { mutableLongStateOf(started)}
        val estimated = remember { mutableIntStateOf(estimated)}
        val elapsed = remember { mutableIntStateOf(elapsed)}

        AlertDialog(
            onDismissRequest = close,
            confirmButton = {
                Text(
                    text = stringResource(R.string.ok),
                    modifier = Modifier.clickable{
                        setResult(
                            started.longValue,
                            estimated.intValue,
                            elapsed.intValue
                        )
                        now.longValue = started.longValue
                        close()
                    }
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.clickable(onClick = close)
                )
            },
            text = { TimerDialogContent(started, estimated, elapsed, now) }
        )
    }
}

@Composable
private fun TimerDialogContent(
    started: MutableLongState,
    estimated: MutableIntState,
    elapsed: MutableIntState,
    now: MutableLongState,
) {
    Column {
        Text(stringResource(R.string.TEA_estimatedDuration_label))
        Box (
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DurationPicker(
                current = estimated.intValue,
                setValue = { estimated.intValue = it }
            )
        }
        Text(stringResource(R.string.TEA_elapsedDuration_label))
        Box (
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DurationPicker(
                current = elapsed.intValue,
                setValue = { elapsed.intValue = it }
            )
        }
        Row (
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val text = if ( started.longValue == 0L) {
                stringResource(R.string.TEA_timer_controls)
            } else {
                val current = elapsed.intValue + ((now.longValue - started.longValue) / 1000).toInt()
                DateUtils.formatElapsedTime(current.toLong())
            }

            DisabledText( text = text, modifier = Modifier.weight(1f) )
            IconButton(
                onClick = {
                    if  (started.longValue == 0L) {
                        now.longValue = currentTimeMillis()
                        started.longValue = now.longValue
                    } else {
                        elapsed.intValue =
                            elapsed.intValue + ((currentTimeMillis() - started.longValue) / 1000L).toInt()
                        started.longValue = 0L
                        now.longValue = 0L
                    }
                }
            ) {
                if (started.longValue == 0L) {
                    Icon(Icons.Outlined.PlayArrow, null)
                } else {
                    Icon(Icons.Outlined.Pause, null)
                }
            }
        }
    }

    LaunchedEffect(started.longValue) {
        while (started.longValue > 0L) {
            delay(1.seconds)
            now.longValue = currentTimeMillis()
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerDialogPreviewTimerStarted()
{
    TimerDialog(
        show = true,
        started = currentTimeMillis()+12345678,
        estimated = 980,
        elapsed = 315,
        setResult = { a,b,c -> },
        close = {}
    )
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerDialogPreview()
{
    TimerDialog(
        show = true,
        started = 0L,
        estimated = 980,
        elapsed = 315,
        setResult = { a,b,c -> },
        close = {}
    )
}

