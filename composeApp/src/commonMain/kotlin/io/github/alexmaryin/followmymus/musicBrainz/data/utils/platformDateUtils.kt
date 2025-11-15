package io.github.alexmaryin.followmymus.musicBrainz.data.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

internal expect fun YearMonth.formatToMonthYear(): String
internal expect fun LocalDate.formatToDayMonthYear(): String
