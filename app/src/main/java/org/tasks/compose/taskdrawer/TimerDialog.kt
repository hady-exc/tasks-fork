package org.tasks.compose.taskdrawer

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.tasks.R
import org.tasks.compose.DisabledText

@Composable
fun TimerDialog(
    started: Long,
    estimated: Int,
    elapsed: Int,

) {
    val started = remember { mutableStateOf(started) }
    val estimated = remember { mutableStateOf(estimated) }
    val elapsed = remember { mutableStateOf(elapsed) }
    AlertDialog(
        onDismissRequest = {},
        confirmButton = { Text("OK") },
        dismissButton = { Text("CANCEL") },
        text = { TimerDialogContent(started.value, estimated.value, elapsed.value) }
    )
}

@Composable
private fun TimerDialogContent(
    started: Long,
    estimated: Int,
    elapsed: Int,
) {
    val started = remember { mutableSetOf(started) }
    val estimated = remember { mutableStateOf(estimated) }
    val elapsed = remember { mutableStateOf(elapsed) }
    Column {
        Text(stringResource(R.string.TEA_estimatedDuration_label))
        Box (
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DurationPicker(
                current = estimated.value,
                setValue = { estimated.value = it }
            )
        }
        Text(stringResource(R.string.TEA_elapsedDuration_label))
        Box (
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DurationPicker(
                current = elapsed.value,
                setValue = { elapsed.value = it }
            )
        }
        Row (modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)){
            DisabledText(
                text = stringResource(R.string.TEA_timer_controls),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.PlayArrow, null)
        }
    }
}

@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TimerDialogPreview()
{
    TimerDialog(0, 65*60, 315)
}