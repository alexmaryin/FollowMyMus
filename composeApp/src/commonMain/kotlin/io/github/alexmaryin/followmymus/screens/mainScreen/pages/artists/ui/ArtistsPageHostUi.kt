package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.window.core.layout.WindowSizeClass
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.experimental.panels.ChildPanels
import com.arkivanov.decompose.extensions.compose.experimental.panels.ChildPanelsAnimators
import com.arkivanov.decompose.extensions.compose.experimental.panels.HorizontalChildPanelsLayout
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.PredictiveBackParams
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.materialPredictiveBackAnimatable
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.panels.ChildPanelsMode
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost.ArtistsHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.ArtistsPanelUi
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.mediaPanel.MediaPanelUi
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.ReleasesPanelUi
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel.components.ReleasesPlaceholder

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun ArtistsPageHostUi(
    component: ArtistsHostComponent
) {
    val panels by component.panels.subscribeAsState()
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass

    Box(modifier = Modifier.fillMaxSize()) {
        ChildPanels(
            panels = panels,
            mainChild = { ArtistsPanelUi(it.instance) },
            detailsChild = { ReleasesPanelUi(it.instance) },
            extraChild = { MediaPanelUi(it.instance) },
            layout = HorizontalChildPanelsLayout(dualWeights = 0.4f to 0.6f),
            secondPanelPlaceholder = { ReleasesPlaceholder() },
            animators = ChildPanelsAnimators(single = fade() + scale(), dual = fade() to fade()),
            predictiveBackParams = {
                PredictiveBackParams(
                    backHandler = component.backHandler,
                    onBack = { component(ArtistsHostAction.OnBack) },
                    animatable = ::materialPredictiveBackAnimatable
                )
            }
        )

        val mode = when {
            windowSize.isWidthAtLeastBreakpoint(
                WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
            ) -> ChildPanelsMode.DUAL

            else -> ChildPanelsMode.SINGLE
        }

        DisposableEffect(mode) {
            component(ArtistsHostAction.SetMode(mode))
            onDispose { }
        }
    }
}