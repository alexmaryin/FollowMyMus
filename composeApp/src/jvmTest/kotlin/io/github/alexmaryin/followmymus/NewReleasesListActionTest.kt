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
    fun `Dismiss appends ID to dismissHistory and calls repository`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))

        assertEquals(listOf("rg-1"), component.state.value.dismissHistory.dismissedIds)
        assertEquals(listOf("rg-1"), repository.dismissedCalls)
    }

    @Test
    fun `Multiple Dismiss calls accumulate in order`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))
        component(NewReleasesListAction.Dismiss("rg-3"))

        assertEquals(
            listOf("rg-1", "rg-2", "rg-3"),
            component.state.value.dismissHistory.dismissedIds,
        )
        assertEquals(
            listOf("rg-1", "rg-2", "rg-3"),
            repository.dismissedCalls,
        )
    }

    @Test
    fun `RestoreAllDismissed preserves history and sets restoreWasApplied`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))

        component(NewReleasesListAction.RestoreAllDismissed)

        assertEquals(listOf("rg-1", "rg-2"), component.state.value.dismissHistory.dismissedIds)
        assertTrue(component.state.value.dismissHistory.restoreWasApplied)
        assertEquals(1, repository.restoreAllCalls)
    }

    @Test
    fun `UndoLastDismissal after RestoreAllDismissed calls markDismissed on last ID`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))

        component(NewReleasesListAction.RestoreAllDismissed)
        component(NewReleasesListAction.UndoLastDismissal)

        assertEquals(listOf("rg-1"), component.state.value.dismissHistory.dismissedIds)
        assertTrue(component.state.value.dismissHistory.restoreWasApplied)
        assertEquals(listOf("rg-2"), repository.dismissedCalls.drop(2))
        assertTrue(repository.unseenCalls.isEmpty())
    }

    @Test
    fun `UndoLastDismissal pops last ID and calls markUnseen`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))
        component(NewReleasesListAction.Dismiss("rg-3"))

        component(NewReleasesListAction.UndoLastDismissal)

        assertEquals(
            listOf("rg-1", "rg-2"),
            component.state.value.dismissHistory.dismissedIds,
        )
        assertEquals(listOf("rg-3"), repository.unseenCalls)
    }

    @Test
    fun `UndoLastDismissal on empty history is a no-op`() = runTest {
        component(NewReleasesListAction.UndoLastDismissal)

        assertTrue(component.state.value.dismissHistory.dismissedIds.isEmpty())
        assertTrue(repository.unseenCalls.isEmpty())
    }

    @Test
    fun `multiple UndoLastDismissal after RestoreAllDismissed re-dismisses each item`() = runTest {
        component(NewReleasesListAction.Dismiss("rg-1"))
        component(NewReleasesListAction.Dismiss("rg-2"))

        component(NewReleasesListAction.RestoreAllDismissed)
        component(NewReleasesListAction.UndoLastDismissal)
        component(NewReleasesListAction.UndoLastDismissal)

        assertTrue(component.state.value.dismissHistory.dismissedIds.isEmpty())
        assertFalse(component.state.value.dismissHistory.restoreWasApplied)
        assertEquals(listOf("rg-2", "rg-1"), repository.dismissedCalls.drop(2))
    }

    @Test
    fun `SelectRelease calls openMedia`() = runTest {
        component(NewReleasesListAction.SelectRelease("rg-1", "Test Release"))

        assertEquals(listOf("rg-1" to "Test Release"), openMediaCalls)
    }
}
