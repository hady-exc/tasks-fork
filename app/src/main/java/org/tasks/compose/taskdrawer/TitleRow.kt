package org.tasks.compose.taskdrawer

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tasks.R

private val clearIcon = Icons.Outlined.Close
private val saveIcon = Icons.Outlined.Save

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleRow(
    current: String,
    onValueChange: (String) -> Unit,
    changed: Boolean,
    save: () -> Unit,
    close: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = current,
        onValueChange = onValueChange,
        trailingIcon = {
            if (changed) {
                IconButton(onClick = save) { Icon(saveIcon, "Save") }
            } else {
                IconButton(onClick = close) { Icon(clearIcon, "Close") }
            }
        },
        modifier = Modifier
            .onFocusChanged {
                if (it.isFocused) {
                    scope.launch {
                        delay(30)
                        keyboardController?.show()
                    }
                }
            },
        hintText = stringResource(id = R.string.TEA_title_hint),
        onDone = { if (changed) save() },
        singleLine = true
    )

    LaunchedEffect(WindowInsets.isImeVisible == false) { keyboardController!!.show() }

}