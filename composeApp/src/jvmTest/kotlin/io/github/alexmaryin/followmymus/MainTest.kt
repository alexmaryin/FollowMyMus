package io.github.alexmaryin.followmymus

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runComposeUiTest
import io.github.alexmaryin.followmymus.screens.splash.SplashScreen
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class MainTest {

    @Test
    fun `Check if app splash starts with Logo`() = runComposeUiTest {
        setContent {
            SplashScreen()
        }
        awaitIdle()
        "FollowMyMus".forEach { char ->
            onAllNodesWithText(char.toString()).onFirst().assertExists()
        }
    }

    @Test
    fun `Check if app navigates to Login if no valid session`() {

    }
}