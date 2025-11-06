package io.github.alexmaryin.followmymus

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.start
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.core.ui.ObserveEvents
import io.github.alexmaryin.followmymus.screens.commonUi.isPasswordVisibleKey
import io.github.alexmaryin.followmymus.screens.signUp.domain.CMPSignUpComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpEvent
import io.github.alexmaryin.followmymus.screens.signUp.ui.SignUpScreen
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.mockk.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class SignUpScreenTest {

    private val mockkSession = mockk<SessionManager>()
    private val clickCatcher = mockk<() -> Unit> { every { this@mockk() } returns Unit }
    private val lifecycle = com.arkivanov.essenty.lifecycle.LifecycleRegistry()
    private val lifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: androidx.lifecycle.Lifecycle
            get() = registry
    }

    private lateinit var signUpComponent: SignUpComponent

    @BeforeTest
    fun setUp() {
        lifecycle.start()
        signUpComponent = CMPSignUpComponent(
            DefaultComponentContext(lifecycle),
            onLoginClick = clickCatcher
        )
        startKoin {
            modules(module { single<SessionManager> { mockkSession } })
        }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private suspend fun ComposeUiTest.setContentForSignUpScreen(
        eventsHandler: ((SignUpEvent) -> Unit)? = null
    ) {
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                SignUpScreen(signUpComponent)
                eventsHandler?.let {
                    ObserveEvents(signUpComponent.events, onEvent = it)
                }
            }
        }
        awaitIdle()
    }

    @Test
    fun `SignUp Screen should have clickable link to Login screen`() = runComposeUiTest {
        setContentForSignUpScreen()

        onNodeWithText(getString(Res.string.link_to_login))
            .assertHasClickAction()
            .performClick()
        verify(exactly = 1) { clickCatcher() }
    }

    @Test
    fun `Nickname text field should show icon to erase when typing and do it on click`() = runComposeUiTest {
        setContentForSignUpScreen()

        val nicknameField = onNodeWithText(getString(Res.string.nickname_signup_label))
        nicknameField.performTextInput("test")
        nicknameField.assert(hasText("test"))

        onNodeWithContentDescription("clear text field").assertExists()
            .assertHasClickAction()
            .performClick()

        nicknameField.assert(hasText(""))
    }

    @Test
    fun `Password text field should show icon to toggle visibility and do it on click`() = runComposeUiTest {
        setContentForSignUpScreen()

        val passwordField = onNodeWithText(getString(Res.string.password_signup_label))
        passwordField.performTextInput("password123")
        passwordField.assert(SemanticsMatcher.expectValue(isPasswordVisibleKey, false))

        onNodeWithContentDescription("toggle password visibility").assertExists()
            .assertHasClickAction()
            .performClick()

        passwordField.assert(SemanticsMatcher.expectValue(isPasswordVisibleKey, true))

        onNodeWithContentDescription("toggle password visibility").assertExists()
            .performClick()

        passwordField.assert(SemanticsMatcher.expectValue(isPasswordVisibleKey, false))
    }

    @Test
    fun `Invalid nickname should follows with error support text and reacts on new input`() = runComposeUiTest {
        setContentForSignUpScreen()
        val nicknameField = onNodeWithText(getString(Res.string.nickname_signup_label))
        nicknameField.performTextInput("invalid nickname,")

        onNodeWithContentDescription("sign up click")
            .assertHasClickAction()
            .performClick()

        waitUntil { !signUpComponent.state.value.isNicknameValid }

        onNodeWithContentDescription("clear text field").performClick()
        nicknameField.performTextInput("valid_nickname")

        onNodeWithContentDescription("sign up click").performClick()

        waitUntil { signUpComponent.state.value.isNicknameValid }
    }

    @Test
    fun `Invalid password should follows with error support text and reacts on new input`() = runComposeUiTest {
        coEvery { mockkSession.signUp(any()) } returns Result.Success(mockk())

        setContentForSignUpScreen()
        val nicknameField = onNodeWithText(getString(Res.string.nickname_signup_label))
        nicknameField.performTextInput("validNickname")
        val passwordField = onNodeWithText(getString(Res.string.password_signup_label))
        passwordField.performTextInput("123abc")

        onNodeWithContentDescription("sign up click")
            .assertHasClickAction()
            .performClick()

        waitUntil { !signUpComponent.state.value.isPasswordValid }

        onNodeWithText(getString(Res.string.password_signup_error))
            .assertIsDisplayed()

        passwordField.performTextReplacement("123AbCd!#@")

        onNodeWithContentDescription("sign up click").performClick()

        waitUntil { signUpComponent.state.value.isPasswordValid }
    }

    @Test
    fun `Valid credentials for sign up should follows with success state`() = runComposeUiTest {
        val validCredentials = Credentials("validNickname", "123AbCd!#@")
        coEvery { mockkSession.signUp(validCredentials) } coAnswers {
            delay(100)
            Result.Success(mockk())
        }
        setContentForSignUpScreen()

        val nicknameField = onNodeWithText(getString(Res.string.nickname_signup_label))
        nicknameField.performTextInput(validCredentials.nickname)
        val passwordField = onNodeWithText(getString(Res.string.password_signup_label))
        passwordField.performTextInput(validCredentials.password)

        onNodeWithContentDescription("sign up click").performClick()
        waitUntil { signUpComponent.state.value.isLoading }

        coVerify(exactly = 1) { mockkSession.signUp(validCredentials) }

        waitUntil { !signUpComponent.state.value.isLoading }
    }

    @Test
    fun `Existing nickname for sign up should follows with error about it`() = runComposeUiTest {
        val credentials = Credentials("nickname", "123AbCd!#@")
        coEvery { mockkSession.signUp(credentials) } returns
                Result.Error(SessionError.AuthError(AuthErrorCode.EmailExists))
        val errorString = getString(Res.string.email_exists)
        setContentForSignUpScreen { event ->
            assert(event == SignUpEvent.ShowError(errorString))
        }

        val nicknameField = onNodeWithText(getString(Res.string.nickname_signup_label))
        nicknameField.performTextInput(credentials.nickname)
        val passwordField = onNodeWithText(getString(Res.string.password_signup_label))
        passwordField.performTextInput(credentials.password)

        onNodeWithContentDescription("sign up click").performClick()

        coVerify(exactly = 1) { mockkSession.signUp(credentials) }

        waitUntil { !signUpComponent.state.value.isLoading && !signUpComponent.state.value.isNicknameValid }
    }
}