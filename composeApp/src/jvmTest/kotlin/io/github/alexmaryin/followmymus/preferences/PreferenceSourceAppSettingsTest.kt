package io.github.alexmaryin.followmymus.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import okio.Path.Companion.toPath
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class PreferenceSourceAppSettingsTest {

    private lateinit var tempFile: File
    private lateinit var source: PreferenceSource

    @BeforeTest
    fun setUp() {
        tempFile = File.createTempFile("app_settings_test", ".preferences_pb").apply { deleteOnExit() }
        val prefs = PreferenceDataStoreFactory.createWithPath(
            produceFile = { tempFile.absolutePath.toPath() }
        )
        source = PreferenceSource(prefs)
    }

    @AfterTest
    fun tearDown() {
        tempFile.delete()
    }

    @Test
    fun `empty DataStore yields all-null AppSettings`() = runTest {
        val settings = source.getAppSettings().first()
        assertNull(settings.newReleasesLastOpenedDay)
        assertNull(settings.newReleasesLastSyncCompletedAt)
    }

    @Test
    fun `setNewReleasesFloor writes both keys atomically and is visible on next read`() = runTest {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val now = Clock.System.now()
        source.setNewReleasesFloor(today, now)

        val settings = source.getAppSettings().first()
        assertEquals(today, settings.newReleasesLastOpenedDay)
        assertEquals(now, settings.newReleasesLastSyncCompletedAt)
    }

    @Test
    fun `setNewReleasesFloor overwrites a previous floor and timestamp`() = runTest {
        val first = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(7, DateTimeUnit.DAY)
        val firstNow = Clock.System.now()
        source.setNewReleasesFloor(first, firstNow)

        val later = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val laterNow = Clock.System.now()
        source.setNewReleasesFloor(later, laterNow)

        val settings = source.getAppSettings().first()
        assertEquals(later, settings.newReleasesLastOpenedDay)
        assertEquals(laterNow, settings.newReleasesLastSyncCompletedAt)
    }

    @Test
    fun `floor and timestamp are exposed as the right types after a round trip`() = runTest {
        val today = LocalDate(2026, 6, 28)
        val now = Instant.parse("2026-06-28T14:23:11.482Z")
        source.setNewReleasesFloor(today, now)

        val settings = source.getAppSettings().first()
        assertEquals(today, settings.newReleasesLastOpenedDay)
        assertEquals(now, settings.newReleasesLastSyncCompletedAt)
    }
}
