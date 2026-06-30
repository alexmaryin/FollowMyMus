package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.pageHost

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.panels.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.core.data.asFlow
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.musicBrainz.domain.NewReleasesRepository
import io.github.alexmaryin.followmymus.preferences.PreferenceSource
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesListAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.panelsNavigation.NewReleasesHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.panelsNavigation.NewReleasesPanelConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui.NewReleasesHostSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetails
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.MediaDetailsConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.mediaDetailsPanel.getMediaDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@OptIn(ExperimentalDecomposeApi::class, ExperimentalCoroutinesApi::class)
@Factory(binds = [NewReleasesHostComponent::class])
class NewReleasesHost(
    @InjectedParam private val componentContext: ComponentContext,
) : NewReleasesHostComponent, ComponentContext by componentContext, KoinComponent {

    private val _state by saveableMutableValue(NewReleasesHostState.serializer(), init = ::NewReleasesHostState)
    override val state: Value<NewReleasesHostState> = _state

    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = NewReleasesHostSlots(this)

    private val scope = componentContext.coroutineScope()

    private val navigation = PanelsNavigation<Unit, MediaDetailsConfig, Unit>()

    private val _panels = childPanels(
        source = navigation,
        serializers = NewReleasesPanelConfig.SERIALIZERS,
        initialPanels = { Panels(main = Unit) },
        key = "NewReleasesPanels",
        onStateChanged = { new, _ ->
            val backIsVisible = new.details != null
            _state.update {
                it.copy(
                    releaseIdSelected = new.details?.releaseId,
                    backVisible = backIsVisible
                )
            }
        },
        handleBackButton = true,
        mainFactory = { _, context -> getNewReleasesList(context) },
        detailsFactory = ::getMediaDetails,
        extraFactory = { _, _ -> }
    )

    override val panels: Value<ChildPanels<*, NewReleasesList, *, MediaDetails, *, Unit>> = _panels

    init {
        panels.asFlow().flatMapLatest { update ->
            merge(
                update.main.instance.scaffoldSlots.snackbarMessages,
                update.details?.instance?.scaffoldSlots?.snackbarMessages ?: emptyFlow()
            )
        }
            .distinctUntilChanged()
            .onEach { message -> events.send(message) }
            .launchIn(scope)

        lifecycle.doOnStart {
            scope.launch {
                _panels.value.main.instance(NewReleasesListAction.LoadFromRemote)
            }
        }
    }

    private fun onBack() {
        navigation.pop()
    }

    override fun invoke(action: NewReleasesHostAction) {
        when (action) {
            is NewReleasesHostAction.ShowMediaDetails -> {
                navigation.navigate { state ->
                    state.copy(details = MediaDetailsConfig(action.releaseId, action.releaseName))
                }
                _panels.value.main.instance(NewReleasesListAction.OnMediaOpened(action.releaseId))
            }
            NewReleasesHostAction.CloseMediaDetails -> navigation.dismissDetails()
            NewReleasesHostAction.Refresh -> _panels.value.main.instance(NewReleasesListAction.LoadFromRemote)
            is NewReleasesHostAction.SetMode -> navigation.navigate { state -> state.copy(mode = action.mode) }
            NewReleasesHostAction.OnBack -> onBack()
        }
    }

    private fun getNewReleasesList(context: ComponentContext) =
        NewReleasesList(
            repository = get<NewReleasesRepository>(),
            preferenceSource = get<PreferenceSource>(),
            context = context,
            openMedia = { releaseId, releaseName ->
                invoke(NewReleasesHostAction.ShowMediaDetails(releaseId, releaseName))
            }
        )
}
