package org.tasks.compose

import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import timber.log.Timber

/** TODO - write comments on use and implementation details */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardDock(
    //rootView: CoordinatorLayout,
    visible: State<Boolean>,
    ignoreImeClose: State<Boolean>,
    onDismissRequest: (() -> Unit),
    content: @Composable () -> Unit = {}
) {
    if (visible.value) {

        val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels

        val keyboardHeight = rememberKeyboardHeight(screenHeight)
        var keyboardOpened by remember { mutableStateOf(false) }
        val density = LocalDensity.current

        if (!keyboardOpened && keyboardHeight.value > 60.dp) {
            keyboardOpened = true
        } else if (keyboardOpened && keyboardHeight.value < 30.dp) {
            if (!ignoreImeClose.value) onDismissRequest()
            keyboardOpened = false
        }

        Popup(
            popupPositionProvider = remember { WindowBottomPositionProvider(screenHeight, density, keyboardHeight) },
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
                    content()
                }
            }
        }
    }
}

@Composable
internal fun rememberKeyboardHeight(screenHeight: Int): State<Dp> {
    val keyboardHeight = remember { mutableStateOf(0.dp) }
    val view = LocalView.current
    val density = LocalDensity.current
    DisposableEffect(view) {
        val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            keyboardHeight.value = with (density) { (screenHeight - rect.bottom).toDp() }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
        }
    }
    return keyboardHeight
}

/*
* Aligns popup bottom with the bottom of the coordinator_layout
* which is aligned with the top of the IME by the system
*/
private class WindowBottomPositionProvider(
    val screenHeight: Int,
    val density: Density,
    val keyboardHeight: State<Dp>
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        Timber.d("***PROVIDER: keyboardHeight == ${keyboardHeight.value}, screenSize == $screenHeight  popup height == ${popupContentSize.height}")
        return with(density) {
            IntOffset(0, screenHeight - keyboardHeight.value.roundToPx() - popupContentSize.height)
                .let { Timber.d("offset == $it  anchorBounds == $anchorBounds"); it }
        }
        /*
        val rootViewXY = intArrayOf(0, 0)
        rootView.getLocationOnScreen(rootViewXY)
        val bottom = /*rootView.height*/ screenHeight + rootViewXY[1]   /* rootViewXY[1] == rootView.y */
        return IntOffset(0, bottom - popupContentSize.height)
         */
    }
}
