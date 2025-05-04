package org.tasks.compose.taskdrawer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.tasks.R
import org.tasks.compose.Constants

@Composable
fun PromptDiscard(
    show: Boolean,
    discard: () -> Unit,
    cancel: () -> Unit
) {
    if (show) {
        AlertDialog(
            onDismissRequest = cancel,
            title = { Text(stringResource(R.string.discard_confirmation), style = MaterialTheme.typography.headlineSmall) },
            confirmButton = { Constants.TextButton(text = R.string.keep_editing, onClick = cancel ) },
            dismissButton = { Constants.TextButton(text = R.string.discard, onClick = { discard(); cancel() }) }
        )
    }
}