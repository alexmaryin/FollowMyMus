package io.github.alexmaryin.followmymus.core.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.shouldNotBeSwiped(): Modifier = pointerInput(Unit) {
    detectHorizontalDragGestures { change, _ ->
        change.consume()
    }
}
