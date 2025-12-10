package io.github.alexmaryin.followmymus.musicBrainz.domain.models

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.sort_date_this_month
import followmymus.composeapp.generated.resources.sort_date_this_week
import followmymus.composeapp.generated.resources.sort_date_today
import org.jetbrains.compose.resources.StringResource

sealed interface DateCategory {
    data class Recent(val type: RecentDate) : DateCategory
    data class ByYear(val year: Int) : DateCategory
}

enum class RecentDate(val titleRes: StringResource) {
    TODAY(Res.string.sort_date_today),
    THIS_WEEK(Res.string.sort_date_this_week),
    THIS_MONTH(Res.string.sort_date_this_month)
}