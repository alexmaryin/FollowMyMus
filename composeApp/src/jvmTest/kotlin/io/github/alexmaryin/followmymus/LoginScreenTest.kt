package io.github.alexmaryin.followmymus

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.text
import androidx.compose.ui.test.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.start
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.credentials_invalid
import followmymus.composeapp.generated.resources.link_to_sign_up
import followmymus.composeapp.generated.resources.nickname_login_label
import followmymus.composeapp.generated.resources.password
import followmymus.composeapp.generated.resources.password_login_label
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.screens.commonUi.isPasswordVisibleKey
import io.github.alexmaryin.followmymus.screens.login.domain.CMPLoginComponent
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
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class LoginScreenTest {

    val mockkSession = mockk<SessionManager>()

    val lifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: androidx.lifecycle.Lifecycle
            get() = registry
    }

    val lifecycle = com.arkivanov.essenty.lifecycle.LifecycleRegistry()

    val clickCatcher = mockk<() -> Unit> { every { this@mockk() } returns Unit }

    val loginComponent = runOnUiThread {
        CMPLoginComponent(
            null, DefaultComponentContext(lifecycle),
            onSignUpClick = clickCatcher
        )
    }

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<SessionManager> { mockkSession }
                }
            )
        }
    }

    @Test
    fun `Login Screen should have clickable link to Sign up screen`() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                lifecycle.start()
                LoginScreen(loginComponent)
            }
        }
        awaitIdle()

        onNodeWithText(getString(Res.string.link_to_sign_up))
            .assert(hasClickAction())
            .performClick()
        verify(exactly = 1) { clickCatcher() }

        stopKoin()
    }

    @Test
    fun `Nickname text field should show icon to erase when typing`() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                lifecycle.start()
                LoginScreen(loginComponent)
            }
        }
        awaitIdle()

        onNodeWithText(getString(Res.string.nickname_login_label))
            .performTextInput("test")

        onNodeWithText(getString(Res.string.nickname_login_label))
            .assert(hasText("test"))

        onNodeWithContentDescription("clear text field").assertExists()
            .assertHasClickAction()
            .performClick()

        onNodeWithText(getString(Res.string.nickname_login_label))
            .assert(hasText(""))

        stopKoin()
    }

    @Test
    fun `Invalid credentials should follows with error state`() = runComposeUiTest {

        coEvery { mockkSession.signIn(any()) } returns
                Result.Error(SessionError.AuthError(AuthErrorCode.InvalidCredentials))

        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                lifecycle.start()
                LoginScreen(loginComponent)
            }
        }
        awaitIdle()

        onNodeWithText(getString(Res.string.nickname_login_label))
            .performTextInput("invalid_email")

        onNodeWithContentDescription("login click")
            .assertHasClickAction()
            .performClick()

        waitUntil { !loginComponent.state.value.isCredentialsValid }

        stopKoin()
    }

    @Test
    fun `Password text field should show icon to toggle visibility`() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                lifecycle.start()
                LoginScreen(loginComponent)
            }
        }
        awaitIdle()

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

        stopKoin()
    }
}