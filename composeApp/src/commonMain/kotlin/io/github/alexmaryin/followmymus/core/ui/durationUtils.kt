package io.github.alexmaryin.followmymus.core.ui

fun Long.toCompactDuration(): String {
    if (this <= 0) return "00:00"

    val totalSeconds = this / 1_000

    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    fun pad2(value: Long): String =
        value.toString().padStart(2, '0')

    return if (hours > 0) {
        "${pad2(hours)}:${pad2(minutes)}:${pad2(seconds)}"
    } else {
        "${pad2(minutes)}:${pad2(seconds)}"
    }
}

