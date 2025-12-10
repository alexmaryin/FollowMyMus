package io.github.alexmaryin.followmymus.musicBrainz.data.utils

import io.github.alexmaryin.followmymus.musicBrainz.domain.models.DateCategory
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.RecentDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

fun Instant.toDateCategory(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): DateCategory {
    val date = toLocalDateTime(timeZone).date
    val today = Clock.System.now().toLocalDateTime(timeZone).date

    return when {
        date == today -> DateCategory.Recent(RecentDate.TODAY)
        date.daysUntil(today) < 7 -> DateCategory.Recent(RecentDate.THIS_WEEK)
        date.month == today.month && date.year == today.year -> DateCategory.Recent(RecentDate.THIS_MONTH)
        else -> DateCategory.ByYear(date.year)
    }
}

