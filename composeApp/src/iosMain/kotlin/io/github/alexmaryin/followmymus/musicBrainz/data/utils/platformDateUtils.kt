package io.github.alexmaryin.followmymus.musicBrainz.data.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toNSDateComponents
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

internal actual fun YearMonth.formatToMonthYear(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "MMMM yyyy"
    formatter.locale = NSLocale.currentLocale
    val date = NSCalendar.currentCalendar.dateFromComponents(toNSDateComponents())
    return date?.let { formatter.stringFromDate(it) } ?: ""
}

internal actual fun LocalDate.formatToDayMonthYear(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "dd MMMM yyyy"
    formatter.locale = NSLocale.currentLocale
    val date = NSCalendar.currentCalendar.dateFromComponents(toNSDateComponents())
    return date?.let { formatter.stringFromDate(it) } ?: ""
}
