package org.tasks.compose.taskdrawer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var focused by remember { mutableStateOf(false) }
    if (show || focused) {
        OutlinedTextField(
            value = current,
            onValueChange = onValueChange,
            modifier = Modifier.onFocusChanged { focused = it.isFocused; onFocusChange(focused) },
            hintText = stringResource(id = R.string.TEA_note_label),
            singleLine = false,
            maxLines = if (focused) Int.MAX_VALUE else 2,
        )
    }
}