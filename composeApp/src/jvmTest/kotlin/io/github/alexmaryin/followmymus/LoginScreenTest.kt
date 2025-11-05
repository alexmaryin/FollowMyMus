package io.github.alexmaryin.followmymus

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.start
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.link_to_sign_up
import followmymus.composeapp.generated.resources.nickname_login_label
import followmymus.composeapp.generated.resources.password_login_label
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.screens.commonUi.isPasswordVisibleKey
import io.github.alexmaryin.followmymus.screens.login.domain.CMPLoginComponent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent
import io.github.alexmaryin.followmymus.screens.login.ui.LoginScreen
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class LoginScreenTest {

    private val mockkSession = mockk<SessionManager>()
    private val clickCatcher = mockk<() -> Unit> { every { this@mockk() } returns Unit }
    private val lifecycle = com.arkivanov.essenty.lifecycle.LifecycleRegistry()
    private val lifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: androidx.lifecycle.Lifecycle
            get() = registry
    }
    private lateinit var loginComponent: LoginComponent

    @BeforeTest
    fun setUp() {
        lifecycle.start()
        loginComponent = CMPLoginComponent(
            null, DefaultComponentContext(lifecycle),
            onSignUpClick = clickCatcher
        )
        startKoin {
            modules(module { single<SessionManager> { mockkSession } })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private suspend fun ComposeUiTest.setContentForLoginScreen() {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                LoginScreen(loginComponent)
            }
        }
        awaitIdle()
    }

    @Test
    fun `Login Screen should have clickable link to Sign up screen`() = runComposeUiTest {
        setContentForLoginScreen()

        onNodeWithText(getString(Res.string.link_to_sign_up))
            .assert(hasClickAction())
            .performClick()
        verify(exactly = 1) { clickCatcher() }
    }

    @Test
    fun `Nickname text field should show icon to erase when typing`() = runComposeUiTest {
        setContentForLoginScreen()

        onNodeWithText(getString(Res.string.nickname_login_label))
            .performTextInput("test")

        onNodeWithText(getString(Res.string.nickname_login_label))
            .assert(hasText("test"))

        onNodeWithContentDescription("clear text field").assertExists()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(getString(Res.string.nickname_login_label))
            .assert(hasText(""))
    }

    @Test
    fun `Invalid credentials should follows with error state`() = runComposeUiTest {

        coEvery { mockkSession.signIn(any()) } returns
                Result.Error(SessionError.AuthError(AuthErrorCode.InvalidCredentials))

        setContentForLoginScreen()

        onNodeWithText(getString(Res.string.nickname_login_label))
            .performTextInput("invalid_email")

        onNodeWithContentDescription("login click")
            .assertHasClickAction()
            .performClick()

        waitUntil { !loginComponent.state.value.isCredentialsValid }
    }

    @Test
    fun `Password text field should show icon to toggle visibility`() = runComposeUiTest {
        setContentForLoginScreen()

        onNodeWithText(getString(Res.string.password_login_label))
            .performTextInput("password123")

        onNodeWithText(getString(Res.string.password_login_label))
            .assert(SemanticsMatcher.expectValue(isPasswordVisibleKey, false))

        onNodeWithContentDescription("toggle password visibility").assertExists()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(getString(Res.string.password_login_label))
            .assert(SemanticsMatcher.expectValue(isPasswordVisibleKey, true))

        onNodeWithContentDescription("toggle password visibility").assertExists()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(getString(Res.string.password_login_label))
            .assert(SemanticsMatcher.expectValue(isPasswordVisibleKey, false))
    }
}