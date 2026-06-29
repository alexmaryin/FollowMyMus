package io.github.alexmaryin.followmymus

import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.DismissHistory
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DismissHistoryTest {

    @Test
    fun `empty history has no dismissals`() {
        val subject = DismissHistory()
        assertTrue(subject.dismissedIds.isEmpty())
        assertFalse(subject.hasDismissals)
    }

    @Test
    fun `append one ID sets hasDismissals`() {
        val subject = DismissHistory().copy(
            dismissedIds = DismissHistory().dismissedIds + "rg-1"
        )
        assertEquals(listOf("rg-1"), subject.dismissedIds)
        assertTrue(subject.hasDismissals)
    }

    @Test
    fun `append multiple IDs preserves insertion order`() {
        val subject = DismissHistory().copy(
            dismissedIds = DismissHistory().dismissedIds + listOf("rg-1", "rg-2", "rg-3")
        )
        assertEquals(listOf("rg-1", "rg-2", "rg-3"), subject.dismissedIds)
    }

    @Test
    fun `pop last ID returns remaining history`() {
        val ids = listOf("rg-1", "rg-2", "rg-3")
        val subject = DismissHistory(dismissedIds = ids.dropLast(1))
        assertEquals(listOf("rg-1", "rg-2"), subject.dismissedIds)
        assertTrue(subject.hasDismissals)
    }

    @Test
    fun `pop until empty clears hasDismissals`() {
        val ids = listOf("rg-1")
        val subject = DismissHistory(dismissedIds = ids.dropLast(1))
        assertTrue(subject.dismissedIds.isEmpty())
        assertFalse(subject.hasDismissals)
    }

    @Test
    fun `serialization round-trip preserves dismissedIds`() {
        val original = DismissHistory(dismissedIds = listOf("rg-1", "rg-2"))
        val json = Json.encodeToString(DismissHistory.serializer(), original)
        val decoded = Json.decodeFromString(DismissHistory.serializer(), json)
        assertEquals(original, decoded)
    }

    @Test
    fun `serialization of empty history`() {
        val original = DismissHistory()
        val json = Json.encodeToString(DismissHistory.serializer(), original)
        val decoded = Json.decodeFromString(DismissHistory.serializer(), json)
        assertEquals(original, decoded)
    }
}
