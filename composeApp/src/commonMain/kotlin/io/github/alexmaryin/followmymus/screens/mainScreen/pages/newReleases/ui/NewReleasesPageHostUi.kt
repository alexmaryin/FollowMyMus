@file:OptIn(ExperimentalDecomposeApi::class)

package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
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
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.pageHost.NewReleasesHostAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.panelsNavigation.NewReleasesHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui.list.NewReleasesList
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.mediaPanel.MediaPanelUi

@Composable
fun NewReleasesPageHostUi(component: NewReleasesHostComponent) {
    val panels by component.panels.subscribeAsState()
    val windowSize = currentWindowAdaptiveInfoV2().windowSizeClass

    Box(modifier = Modifier.fillMaxSize()) {
        ChildPanels(
            panels = panels,
            mainChild = { NewReleasesList(it.instance) },
            detailsChild = { MediaPanelUi(it.instance) },
            extraChild = { },
            layout = HorizontalChildPanelsLayout(dualWeights = 0.4f to 0.6f),
            secondPanelPlaceholder = { },
            animators = ChildPanelsAnimators(single = fade() + scale(), dual = fade() to fade()),
            predictiveBackParams = {
                PredictiveBackParams(
                    backHandler = component.backHandler,
                    onBack = { component(NewReleasesHostAction.OnBack) },
                    animatable = ::materialPredictiveBackAnimatable
                )
            }
        )

        val mode = when {
            windowSize.isWidthAtLeastBreakpoint(
                WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
            ) && panels.details != null -> ChildPanelsMode.DUAL
            else -> ChildPanelsMode.SINGLE
        }

        DisposableEffect(mode) {
            component(NewReleasesHostAction.SetMode(mode))
            onDispose { }
        }
    }
}
