package org.tasks.compose

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

object SwipeOut {

    enum class Anchors { Left, Center, Right }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun SwipeOut(
        modifier: Modifier = Modifier,
        index: Int,
        onSwipe: (Int) -> Unit,
        decoration: @Composable BoxScope.() -> Unit = {},
        content: @Composable BoxScope.() -> Unit
    ) {
        val screenWidthPx =
            with(LocalDensity.current) {
                LocalConfiguration.current.screenWidthDp.dp.roundToPx()
            }
        val half = (screenWidthPx / 2).toFloat()

        val dragState = remember {
            AnchoredDraggableState(
                initialValue = Anchors.Center,
                anchors = DraggableAnchors<Anchors> {
                    Anchors.Left at -(half)
                    Anchors.Center at 0f
                    Anchors.Right at half
                },
                positionalThreshold = { distance: Float -> distance * 10f },
                velocityThreshold = { 100f },
                animationSpec = tween()
            )
        }

        if (dragState.currentValue == Anchors.Left || dragState.currentValue == Anchors.Right) {
            //Log.d("SWIPEOUT", "calling OnSwipe")
            onSwipe(index)
        }

        Box(    /* container for swipeable and it's background decoration */
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart
        ) {

            decoration()

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            x = dragState.requireOffset().roundToInt(),
                            y = 0
                        )
                    }
                    .background(Color.White)
                    .anchoredDraggable(dragState, Orientation.Horizontal, reverseDirection = false)
            ) {
                content()
            }
        }
    }
}