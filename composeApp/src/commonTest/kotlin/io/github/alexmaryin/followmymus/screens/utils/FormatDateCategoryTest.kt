package io.github.alexmaryin.followmymus.screens.utils

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.models.FavoriteArtist
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class FormatDateCategoryTest {

    private val timeZone = TimeZone.UTC

    @Test
    fun `toDateCategory returns TODAY for current date`() {
        val now = Clock.System.now()
        val category = now.toDateCategory(timeZone)
        assertEquals(DateCategory.Recent(RecentDate.TODAY), category)
    }

    @Test
    fun `toDateCategory returns THIS_WEEK for a date 3 days ago`() {
        val now = Clock.System.now()
        val threeDaysAgo = now.minus(3, DateTimeUnit.DAY, timeZone)
        val category = threeDaysAgo.toDateCategory(timeZone)
        assertEquals(DateCategory.Recent(RecentDate.THIS_WEEK), category)
    }

    @Test
    fun `toDateCategory returns THIS_MONTH for a date 15 days ago`() {
        val now = Clock.System.now()
        val fifteenDaysAgo = now.minus(15, DateTimeUnit.DAY, timeZone)
        val category = fifteenDaysAgo.toDateCategory(timeZone)
        assertEquals(DateCategory.Recent(RecentDate.THIS_MONTH), category)
    }

    @Test
    fun `toDateCategory returns ByYear for a date in a previous year`() {
        val now = Clock.System.now()
        val lastYear = now.minus(1, DateTimeUnit.YEAR, timeZone)
        val category = lastYear.toDateCategory(timeZone)
        val expectedYear = lastYear.toLocalDateTime(timeZone).year
        assertEquals(DateCategory.ByYear(expectedYear), category)
    }

    @Test
    fun `sortDateCategoryGroups sorts groups correctly`() = runTest {
        val today = DateCategory.Recent(RecentDate.TODAY)
        val thisWeek = DateCategory.Recent(RecentDate.THIS_WEEK)
        val thisMonth = DateCategory.Recent(RecentDate.THIS_MONTH)
        val year2023 = DateCategory.ByYear(2023)
        val year2022 = DateCategory.ByYear(2022)

        val map = mapOf(
            year2022 to emptyList<FavoriteArtist>(),
            thisMonth to emptyList(),
            year2023 to emptyList(),
            today to emptyList(),
            thisWeek to emptyList()
        )

        val sortedMap = map.sortDateCategoryGroups()

        val expectedOrder = listOf(
            today.toTitle(),
            thisWeek.toTitle(),
            thisMonth.toTitle(),
            year2023.toTitle(),
            year2022.toTitle()
        )

        assertEquals(expectedOrder, sortedMap.keys.toList())
    }
}
