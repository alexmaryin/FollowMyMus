package io.github.alexmaryin.followmymus.preferences

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * App-level settings stored in DataStore preferences (NOT in Room — these
 * survive the `musicbrainz.db` destructive migration because they live in
 * a separate `.preferences_pb` file).
 *
 * Both fields are nullable:
 * - `newReleasesLastOpenedDay == null` is the "first sync ever" signal
 *   (the floor becomes `today - 30 days`).
 * - `newReleasesLastSyncCompletedAt == null` is rendered by the UI as
 *   "never updated" until the first successful sync.
 */
data class AppSettings(
    val newReleasesLastOpenedDay: LocalDate?,
    val newReleasesLastSyncCompletedAt: Instant?,
)

private val KEY_NEW_RELEASES_LAST_OPENED_DAY = stringPreferencesKey("new_releases_last_opened_day")
private val KEY_NEW_RELEASES_LAST_SYNC_COMPLETED_AT = stringPreferencesKey("new_releases_last_sync_completed_at")

fun PreferenceSource.getAppSettings(): Flow<AppSettings> =
    preferences.data
        .map { p ->
            AppSettings(
                newReleasesLastOpenedDay = p[KEY_NEW_RELEASES_LAST_OPENED_DAY]?.let(LocalDate::parse),
                newReleasesLastSyncCompletedAt = p[KEY_NEW_RELEASES_LAST_SYNC_COMPLETED_AT]?.let(Instant::parse),
            )
        }
        .flowOn(Dispatchers.IO)

/**
 * Atomically updates both keys in a single `prefs.edit { }` block. Mirrors
 * the `changeThemeMode` / `changeLanguage` error-handling pattern: catch
 * and print, never propagate (the sync still completes; the next sync just
 * re-fetches the same window).
 */
suspend fun PreferenceSource.setNewReleasesFloor(day: LocalDate, now: Instant) {
    withContext(Dispatchers.IO) {
        try {
            preferences.edit { p ->
                p[KEY_NEW_RELEASES_LAST_OPENED_DAY] = day.toString()
                p[KEY_NEW_RELEASES_LAST_SYNC_COMPLETED_AT] = now.toString()
            }
        } catch (e: Exception) {
            println("FAILED TO SAVE APP SETTINGS ON THE DISK!")
            e.printStackTrace()
        }
    }
}

suspend fun PreferenceSource.clearNewReleasesFloor() {
    try {
        preferences.edit { p ->
            p.remove(KEY_NEW_RELEASES_LAST_OPENED_DAY)
            p.remove(KEY_NEW_RELEASES_LAST_SYNC_COMPLETED_AT)
        }
    } catch (e: Exception) {
        println("FAILED TO SAVE APP SETTINGS ON THE DISK!")
        e.printStackTrace()
    }
}
