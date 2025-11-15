package io.github.alexmaryin.followmymus.musicBrainz.data.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

internal fun String.parseLifeSpanDate() = runCatching {
    val parts = trim().split("-").map { it.trim() }
    when (parts.size) {
        1 -> {
            require(parts.first().length == 4) { "Year must be 4 digits" }
            parts.first()
        }
        2 -> {
            YearMonth.parse(parts.joinToString("-")).formatToMonthYear()
        }
        3 -> {
            LocalDate.parse(parts.joinToString("-")).formatToDayMonthYear()
        }
        else -> null
    }
}.getOrNull()
