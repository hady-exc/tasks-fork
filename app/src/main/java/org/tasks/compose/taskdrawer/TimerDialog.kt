package org.tasks.compose.taskdrawer

import android.content.res.Configuration
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.DisabledText
import org.tasks.time.DateTimeUtils2.currentTimeMillis

@Composable
fun TimerDialog(
    state: TimerChipState,
    started: Long,
    setTimer: (Boolean) -> Unit,
    onDone: () -> Unit
) {
    if (state.showDialog.value) {
        AlertDialog(
            onDismissRequest = { state.showDialog.value = false },
            confirmButton = {
                Text(
                    text = stringResource(R.string.ok),
                    modifier = Modifier.clickable{ onDone(); state.showDialog.value = false }
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(R.string.cancel),
                    modifier = Modifier.clickable { state.showDialog.value = false }
                )
            },
            text = { TimerDialogContent(state, started, setTimer) }
        )
    }
}

@Composable
private fun TimerDialogContent(
    state: TimerChipState,
    started: Long,
    setTimer: (Boolean) -> Unit
) {
    Column {
        Text(stringResource(R.string.TEA_estimatedDuration_label))
        Box (
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DurationPicker(
                current = state.estimated.intValue,
                setValue = {
                    state.estimated.intValue = it
                }
            )
        }
        Text(stringResource(R.string.TEA_elapsedDuration_label))
        Box (
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DurationPicker(
                current = state.elapsed.intValue,
                setValue = {
                    state.elapsed.intValue = it
                }
            )
        }
        Row (
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val text = if ( started == 0L) {
                stringResource(R.string.TEA_timer_controls)
            } else {
                state.elapsedText(started)
            }

            DisabledText(
                text = text,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if  (started == 0L) {
                        state.now.longValue = currentTimeMillis()
                        setTimer(true)
                    } else {
                        state.elapsed.intValue =
                            state.elapsed.intValue + ((currentTimeMillis() - started) / 1000L).toInt()
                        setTimer(false)
                    }
                }
            ) {
                if (started == 0L) {
                    Icon(Icons.Outlined.PlayArrow, null)
                } else {
                    Icon(Icons.Outlined.Pause, null)
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerDialogPreviewTimerStarted()
{
    TimerDialog(
        state = remember {
            TimerChipState().apply {
                showDialog.value = true
                elapsed.intValue = 315
                estimated.intValue = 980
                now.longValue = currentTimeMillis()
            }
        },
        started = currentTimeMillis()+12345678,
        setTimer = {},
        onDone = {}
    )
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerDialogPreview()
{
    TimerDialog(
        state = remember {
            TimerChipState().apply {
                showDialog.value = true
                elapsed.intValue = 315
                estimated.intValue = 980
            }
        },
        started = 0L,
        setTimer = {},
        onDone = {}
    )
}

