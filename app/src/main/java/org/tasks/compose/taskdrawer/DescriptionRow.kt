package org.tasks.compose.taskdrawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import org.tasks.R

@Composable
fun Description(
    show: Boolean,
    current: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit
) {
    if (current != "" || show) {
        OutlinedTextField(
            value = current,
            onValueChange = onValueChange,
            modifier = Modifier.onFocusChanged { onFocusChange(it.isFocused) },
            hintText = stringResource(id = R.string.TEA_note_label),
            singleLine = false,
            maxLines = if (show) Int.MAX_VALUE else 2,
        )
    }
}