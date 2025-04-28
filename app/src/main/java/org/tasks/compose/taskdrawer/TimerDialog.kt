package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
    helper: TimerHelper,
    started: Long,
    setTimer: (Boolean) -> Unit,
    onDone: () -> Unit
) {
    if (helper.showDialog.value) {
        AlertDialog(
            onDismissRequest = { helper.showDialog.value = false },
            confirmButton = {
                Text("OK", modifier = Modifier.clickable{ onDone(); helper.showDialog.value = false })
                            },
            dismissButton = {
                Text("CANCEL", modifier = Modifier.clickable { helper.showDialog.value = false })
                            },
            text = { TimerDialogContent(helper, started, setTimer) }
        )
    }
}

@Composable
private fun TimerDialogContent(
    helper: TimerHelper,
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
                current = helper.estimated.intValue,
                setValue = { helper.estimated.intValue = it }
            )
        }
        Text(stringResource(R.string.TEA_elapsedDuration_label))
        Box (
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DurationPicker(
                current = helper.elapsed.intValue,
                setValue = { helper.elapsed.intValue = it }
            )
        }
        Row (modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)){
            DisabledText(
                text = if (started > 0L) helper.elapsedText(started) else stringResource(R.string.TEA_timer_controls),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { setTimer(started == 0L) }
            ) { }
            Icon(Icons.Outlined.PlayArrow, null)
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerDialogPreviewTimerStarted()
{
    TimerDialog(
        helper = remember {
            TimerHelper().apply {
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
        helper = remember {
            TimerHelper().apply {
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

