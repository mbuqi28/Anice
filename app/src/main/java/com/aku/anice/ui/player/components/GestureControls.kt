package com.aku.anice.ui.player.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun GestureControls(
    onSingleTap: () -> Unit,
    onDoubleTap: (isForward: Boolean) -> Unit,
    onVerticalSwipe: (isLeft: Boolean, delta: Float) -> Unit,
    onHorizontalSwipe: (delta: Float) -> Unit,
    onDragEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Resolusi Konflik 1: Tap & Double Tap
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = { offset ->
                        val isForward = offset.x > size.width / 2
                        onDoubleTap(isForward)
                    }
                )
            }
            .pointerInput(Unit) {
                // Resolusi Konflik 2: Swiping
                detectVerticalDragGestures(
                    onDragEnd = { onDragEnd() },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val isLeft = change.position.x < size.width / 2
                        onVerticalSwipe(isLeft, -dragAmount)
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { onDragEnd() },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        onHorizontalSwipe(dragAmount)
                    }
                )
            }
    ) {
        content()
    }
}
