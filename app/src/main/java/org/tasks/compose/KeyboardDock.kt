package org.tasks.compose

import android.graphics.Rect
import android.view.View
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** TODO - write comments on use and implementation details */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardDock(
    visible: State<Boolean>,
    onDismissRequest: (() -> Unit),
    keyboardDetector: KeyboardDetector = KeyboardDetector.rememberDetector(onDismissRequest),
    content: @Composable (KeyboardDetector) -> Unit = { }
) {
    if (visible.value) {

        val rootView = LocalView.current

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
                    content(keyboardDetector)
                }
            }
        }
    }
}

/*
* Aligns popup bottom with the bottom of the coordinator_layout
* which is aligned with the top of the IME by the system
*/
private class WindowBottomPositionProvider(
    val rootView: View
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val rootViewXY = intArrayOf(0, 0)
        rootView.getLocationOnScreen(rootViewXY)
        //Timber.d("*** rootY == ${rootViewXY[1]}  rootHeight = ${rootView.height}  popupHeight = ${popupContentSize.height}")
        val bottom = rootView.height + rootViewXY[1]   /* rootViewXY[1] == rootView.y */
        return IntOffset(0, bottom - popupContentSize.height)
    }
}

data class ImeState (
    val externalActivity: Boolean = false,
    val imeOpen: Boolean = false
)

class KeyboardDetector (externalActivity: Boolean = false, imeOpen: Boolean = false) {
    private val stateFlow = MutableStateFlow(ImeState(externalActivity, imeOpen))
    lateinit var state: State<ImeState>

    fun blockDismiss(on: Boolean) = stateFlow.update {
        if (on) {
            /* dirty trick to compensate possible loosing onGlobal notification due to an inactivity period */
            stateFlow.value.copy(externalActivity = on, imeOpen = false)
        } else {
            stateFlow.value.copy(externalActivity = on)
        }
    }

    fun setImeVisible(on: Boolean) = stateFlow.update {
        stateFlow.value.copy(imeOpen = on)
    }

    companion object {
        @Composable
        fun rememberDetector(onDismissRequest: () -> Unit): KeyboardDetector {
            val saver = Saver<KeyboardDetector,Pair<Boolean, Boolean>> (
                save = {
                    Pair<Boolean, Boolean>(it.stateFlow.value.externalActivity, it.stateFlow.value.imeOpen)
                },
                restore = {
                    KeyboardDetector(it.first, it.second)
                }
            )
            val detector = rememberSaveable(saver = saver) { KeyboardDetector() }
            detector.state = detector.stateFlow.collectAsStateWithLifecycle()

            val view = LocalView.current
            val density = LocalDensity.current
            val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels
            DisposableEffect(view) {
                val onGlobalListener = ViewTreeObserver.OnGlobalLayoutListener {
                    val rect = Rect()
                    view.getWindowVisibleDisplayFrame(rect)
                    val keyboardHeight = with (density) { (screenHeight - rect.bottom).toDp() }
                    val state = detector.state.value
                    if (state.imeOpen && keyboardHeight < 30.dp) {
                        if (!state.externalActivity) {
                            onDismissRequest()
                        }
                        detector.setImeVisible(false)
                    } else if (!state.imeOpen && keyboardHeight > 60.dp) {
                        detector.setImeVisible(true)
                    }
                }
                view.viewTreeObserver.addOnGlobalLayoutListener(onGlobalListener)
                onDispose {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalListener)
                }
            }
            return detector
        }
    }
}
