package org.tasks.compose.taskdrawer

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/* This fun is copied from the SheetDefaults.kt to be able to set
*  initialValue and skipHiddenState parameters to reduce blinking
*  during start animation
*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSheetState(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
    initialValue: SheetValue = SheetValue.Hidden,
    skipHiddenState: Boolean = false,
): SheetState {
    val density = LocalDensity.current
    return rememberSaveable(
        skipPartiallyExpanded,
        confirmValueChange,
        skipHiddenState,
        saver =
            SheetState.Saver(
                skipPartiallyExpanded = skipPartiallyExpanded,
                confirmValueChange = confirmValueChange,
                density = density,
                skipHiddenState = skipHiddenState,
            )
    ) {
        SheetState(
            skipPartiallyExpanded,
            density,
            initialValue,
            confirmValueChange,
            skipHiddenState,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet (
    dismiss: () -> Unit,
    onDismissRequest: () -> Unit,
    hideConfirmation: () -> Boolean,
    content: @Composable (close: () -> Unit) -> Unit
) {
    //val scope = rememberCoroutineScope()
    val state:  SheetState = rememberSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden) {
                hideConfirmation()
            } else { true }
        },
        initialValue = SheetValue.Expanded,
        skipHiddenState = false
    )

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = state,
        //containerColor = colorResource(id = R.color.input_popup_background), TODO: correct
        scrimColor = BottomSheetDefaults.ScrimColor.copy(alpha = 0.05f),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = null
    ) {
        content {
            dismiss()
            //scope.launch { state.hide() }
            //    .invokeOnCompletion { dismiss() }
        }
    }
}