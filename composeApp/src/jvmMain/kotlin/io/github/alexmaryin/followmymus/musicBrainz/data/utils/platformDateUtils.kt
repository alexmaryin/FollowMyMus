package io.github.alexmaryin.followmymus.musicBrainz.data.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaYearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

internal actual fun YearMonth.formatToMonthYear(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    return toJavaYearMonth().format(formatter)
}

internal actual fun LocalDate.formatToDayMonthYear(): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())
    return toJavaLocalDate().format(formatter)
}
