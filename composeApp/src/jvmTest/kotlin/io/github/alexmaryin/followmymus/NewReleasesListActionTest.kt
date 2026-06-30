package io.github.alexmaryin.followmymus

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.start
import io.github.alexmaryin.followmymus.musicBrainz.data.repository.FakeNewReleasesRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesListAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewReleasesListActionTest {

    private val lifecycle = LifecycleRegistry()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeNewReleasesRepository
    private lateinit var component: NewReleasesList
    private val openMediaCalls = mutableListOf<Pair<String, String>>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        lifecycle.start()
        repository = FakeNewReleasesRepository()
        component = NewReleasesList(
            repository = repository,
            context = DefaultComponentContext(lifecycle),
            openMedia = { id, name -> openMediaCalls += id to name },
        )
    }

    @Test
    fun `Dismiss appends ID to dismissedIds and calls repository`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))

        assertEquals(listOf("rg-1"), component.state.value.dismissedIds)
        assertEquals(listOf("rg-1"), repository.dismissedCalls)
        assertTrue(component.state.value.hasDismissals)
    }

    @Test
    fun `Multiple Dismiss calls accumulate in insertion order`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))
        component(NewReleasesListAction.Dismiss("rg-3"))

        assertEquals(
            listOf("rg-1", "rg-2", "rg-3"),
            component.state.value.dismissedIds,
        )
        assertEquals(
            listOf("rg-1", "rg-2", "rg-3"),
            repository.dismissedCalls,
        )
    }

    @Test
    fun `RestoreAllDismissed clears dismissedIds and calls repository`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))

        component(NewReleasesListAction.RestoreAllDismissed)

        assertTrue(component.state.value.dismissedIds.isEmpty())
        assertFalse(component.state.value.hasDismissals)
        assertEquals(1, repository.restoreAllCalls)
    }

    @Test
    fun `UndoLastDismissal pops last ID and calls markUnseen`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))
        component(NewReleasesListAction.Dismiss("rg-3"))

        component(NewReleasesListAction.UndoLastDismissal)

        assertEquals(
            listOf("rg-1", "rg-2"),
            component.state.value.dismissedIds,
        )
        assertTrue(component.state.value.hasDismissals)
        assertEquals(listOf("rg-3"), repository.unseenCalls)
    }

    @Test
    fun `UndoLastDismissal on empty history is a no-op`() = runTest {
        component(NewReleasesListAction.UndoLastDismissal)

        assertTrue(component.state.value.dismissedIds.isEmpty())
        assertFalse(component.state.value.hasDismissals)
        assertTrue(repository.unseenCalls.isEmpty())
    }

    @Test
    fun `Multiple UndoLastDismissal pops in reverse order and clears hasDismissals`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))
        component(NewReleasesListAction.Dismiss("rg-3"))

        component(NewReleasesListAction.UndoLastDismissal)
        component(NewReleasesListAction.UndoLastDismissal)
        component(NewReleasesListAction.UndoLastDismissal)

        assertTrue(component.state.value.dismissedIds.isEmpty())
        assertFalse(component.state.value.hasDismissals)
        assertEquals(listOf("rg-3", "rg-2", "rg-1"), repository.unseenCalls)
    }

    @Test
    fun `UndoLastDismissal after RestoreAllDismissed is a no-op`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))

        component(NewReleasesListAction.RestoreAllDismissed)
        component(NewReleasesListAction.UndoLastDismissal)

        assertTrue(component.state.value.dismissedIds.isEmpty())
        assertFalse(component.state.value.hasDismissals)
        assertTrue(repository.unseenCalls.isEmpty())
        assertEquals(1, repository.restoreAllCalls)
    }

    @Test
    fun `SelectRelease calls openMedia with id and name`() = runTest {
        component(NewReleasesListAction.SelectRelease("rg-1", "Test Release"))

        assertEquals(listOf("rg-1" to "Test Release"), openMediaCalls)
    }

    @Test
    fun `OnMediaOpened calls repository markSeen`() = runTest {
        component(NewReleasesListAction.OnMediaOpened("rg-1"))

        assertEquals(listOf("rg-1"), repository.seenCalls)
    }
}
