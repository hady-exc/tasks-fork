package org.tasks.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.coordinatorlayout.widget.CoordinatorLayout
import org.tasks.compose.edit.rememberKeyboardHeight

@Composable
fun KeyboardDock(
    rootView: CoordinatorLayout,
    visible: State<Boolean>,
    onDismissRequest: (() -> Unit),
    content: @Composable () -> Unit = {}
) {

    if (visible.value) {
        Popup(
            popupPositionProvider = remember { WindowBottomPositionProvider(rootView) },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true,
                clippingEnabled = false
            )
        ) {
            AnimatedVisibility(
                visible = visible.value,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column {
                        //PopupContent(state, save, { switchOff(); edit() }, switchOff, getList)
                        content()
                        Box(modifier = Modifier.fillMaxWidth().height(height = rememberKeyboardHeight().value ))
                    }
                }
            }

            val keyboardHeight = rememberKeyboardHeight()
            val keyboardOpened = remember { mutableStateOf(false) }
            if (keyboardOpened.value && keyboardHeight.value < 30.dp) {
                keyboardOpened.value = false
                onDismissRequest()
            } else if (!keyboardOpened.value && keyboardHeight.value > 60.dp) {
                keyboardOpened.value = true
            }

        }
    }
}

/*
* Aligns the popup bottom with the bottom of the coordinator_layout
* which is aligned with the top of the IME by the system
*/
private class WindowBottomPositionProvider(
    val rootView: CoordinatorLayout
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val rootViewXY = intArrayOf(0, 0)
        rootView.getLocationOnScreen(rootViewXY)
        val bottom = rootView.height + rootViewXY[1]   /* rootViewXY[1] == rootView.y */
        return IntOffset(0, bottom - popupContentSize.height)
    }
}
