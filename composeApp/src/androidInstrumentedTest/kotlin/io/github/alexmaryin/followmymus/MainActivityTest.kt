package io.github.alexmaryin.followmymus

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.alexmaryin.followmymus.screens.splash.SplashScreen
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class MainComposeTest {
    @get:Rule val composeTestRule = createComposeRule()
    @Test
    fun splashScreenTest() {
        runBlocking {
            composeTestRule.setContent {
                SplashScreen()
            }
            composeTestRule.onNodeWithText("FollowMyMus").assertExists()
        }
    }
}