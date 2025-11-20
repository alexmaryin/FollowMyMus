package io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.ui.components.nicknameAvatar

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

fun avatarInitials(nickname: String): String {
    val parts = nickname.split('.', '_').filter { it.isNotEmpty() }

    if (parts.size >= 2) {
        return (parts[0][0].toString() + parts[1][0]).uppercase()
    }

    val word = parts[0]

    val digits = word.filter { it.isDigit() }
    if (digits.isNotEmpty()) {
        return (word[0].toString() + digits.takeLast(2)).uppercase()
    }

    val letters = word.filter { it.isLetter() }
    val second = letters.drop(1).first { it.lowercaseChar() != letters[0].lowercaseChar() }

    return (letters[0].toString() + second).uppercase()
}

fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1 - abs((h / 60f) % 2 - 1))
    val m = v - c

    val (r, g, b) = when {
        h < 60  -> Triple(c, x, 0f)
        h < 120 -> Triple(x, c, 0f)
        h < 180 -> Triple(0f, c, x)
        h < 240 -> Triple(0f, x, c)
        h < 300 -> Triple(x, 0f, c)
        else    -> Triple(c, 0f, x)
    }

    return Color(r + m, g + m, b + m)
}

fun avatarColor(nickname: String): Color {
    val hash = abs(nickname.hashCode())
    val hue = hash % 360       // only hue varies
    val saturation = 0.55f     // fixed pleasant values
    val value = 0.85f
    return hsvToColor(hue.toFloat(), saturation, value)
}

fun avatarTextColor(background: Color): Color {
    // Based on W3C perceived luminance formula
    val r = background.red
    val g = background.green
    val b = background.blue

    val luminance = 0.299 * r + 0.587 * g + 0.114 * b
    return if (luminance > 0.6) Color.Black else Color.White
}
