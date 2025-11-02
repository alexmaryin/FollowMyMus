package io.github.alexmaryin.followmymus.core.ui

import androidx.window.core.layout.WindowSizeClass

enum class DeviceConfiguration {
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    DESKTOP;

    companion object {
        fun fromWindowSize(windowsSize: WindowSizeClass): DeviceConfiguration {
            return when {
                isDesktop() -> DESKTOP

                windowsSize.isAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                    WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
                ) -> TABLET_PORTRAIT

                windowsSize.isHeightAtLeastBreakpoint(
                    WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
                ) -> MOBILE_PORTRAIT

                else -> MOBILE_LANDSCAPE
            }
        }
    }
}