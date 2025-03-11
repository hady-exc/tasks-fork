package org.tasks.compose.taskdrawer

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet (
    show: Boolean,
    hide: () -> Unit,
    onDismissRequest: () -> Unit,
    hideConfirmation: () -> Boolean,
    content: @Composable (close: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val state:  SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden) {
                hideConfirmation()
            } else { true }
        }
    )

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = state,
            containerColor = colorResource(id = R.color.input_popup_background),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            dragHandle = null
        ) {
            content {
                scope.launch { state.hide() }
                    .invokeOnCompletion { hide() }
            }
        }
    }

}