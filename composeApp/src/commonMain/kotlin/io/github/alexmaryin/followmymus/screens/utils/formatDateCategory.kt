package io.github.alexmaryin.followmymus.screens.utils

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.sort_date_this_month
import followmymus.composeapp.generated.resources.sort_date_this_week
import followmymus.composeapp.generated.resources.sort_date_today
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock
import kotlin.time.Instant

sealed interface DateCategory {
    data class Recent(val type: RecentDate) : DateCategory
    data class ByYear(val year: Int) : DateCategory
}

enum class RecentDate(val titleRes: StringResource) {
    TODAY(Res.string.sort_date_today),
    THIS_WEEK(Res.string.sort_date_this_week),
    THIS_MONTH(Res.string.sort_date_this_month)
}

suspend fun DateCategory.toTitle() = when (this) {
    is DateCategory.Recent -> getString(this.type.titleRes)
    is DateCategory.ByYear -> this.year.toString()
}

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

suspend fun Map<DateCategory, List<FavoriteArtist>>.sortDateCategoryGroups(): Map<String, List<FavoriteArtist>> {

    val sorted = toList().sortedBy {
        when (val key = it.first) {
            is DateCategory.Recent -> key.type.ordinal
            is DateCategory.ByYear -> key.year
        }
    }.associate { (category, list) -> category.toTitle() to list }

    return sorted
}

