package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.releasesPanel

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.musicBrainz.domain.ReleasesRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.domain.models.ArtistReleases
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

class ReleasesList(
    private val repository: ReleasesRepository,
    private val artistId: String,
    private val context: ComponentContext
) : ComponentContext by context {

    private val _releases = MutableStateFlow(ArtistReleases("", ""))
    val releases = _releases.asStateFlow()

    private val scope = context.coroutineScope() + SupervisorJob()

    init {
        lifecycle.doOnStart {
            scope.launch {
                repository.searchArtistReleases(artistId).collect {
                    _releases.value = it
                }
            }
        }
    }
}