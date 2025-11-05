package io.github.alexmaryin.followmymus

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.lifecycle.start
import io.github.alexmaryin.followmymus.rootNavigation.MainRootComponent
import io.github.alexmaryin.followmymus.rootNavigation.RootComponent
import io.github.alexmaryin.followmymus.rootNavigation.ui.RootContent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginAction
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginEvent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.MainScreenState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.MainPages
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PagerComponent
import io.github.alexmaryin.followmymus.screens.splash.SplashScreen
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class RootTest {
    
    val mockkSession = mockk<SessionManager>()

    val mockSessionStatus = mockk<SessionStatus.Authenticated> {
        every { this@mockk.session.user } returns mockk<UserInfo> user@{
            every { this@user.email } returns "mockk" + Credentials.SUFFIX
        }
    }

    val mockkPager = mockk<PagerComponent> {
        every { this@mockk.state  } returns MutableValue(MainScreenState("mockk"))
        every { this@mockk.pages } returns MutableValue(mockk pages@{
            every { this@pages.selectedIndex } returns 0
            every { this@pages.items } returns emptyList()
        })
    }

    val loginComponent = object : LoginComponent {
        override val state = MutableValue(LoginState())
        override val events = emptyFlow<LoginEvent>()
        override fun invoke(action: LoginAction) {}
    }

    val lifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: androidx.lifecycle.Lifecycle
            get() = registry
    }

    private val lifecycle = com.arkivanov.essenty.lifecycle.LifecycleRegistry()
    private lateinit var root: RootComponent

    @OptIn(ExperimentalTestApi::class)
    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<SessionManager> { mockkSession }
                    factory<LoginComponent> { loginComponent }
                    factory<PagerComponent> { mockkPager }
                }
            )
        }

    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private suspend fun ComposeUiTest.setContentForRoot() {
        root = MainRootComponent(DefaultComponentContext(lifecycle))
        lifecycle.start()
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                RootContent(root)
            }
        }
        awaitIdle()
    }

    @Test
    fun `Check if app splash starts with Logo`() = runComposeUiTest {
        setContent { SplashScreen() }
        awaitIdle()
        "FollowMyMus".forEach { char ->
            onAllNodesWithText(char.toString()).onFirst().assertExists()
        }
    }

    @Test
    fun `Check if app navigates to Login if not authenticated`() = runComposeUiTest {

        every { mockkSession.sessionStatus() } returns flow { emit(SessionStatus.NotAuthenticated()) }
        setContentForRoot()

        waitUntil { root.childStack.value.active.instance is RootComponent.Child.LoginChild }
        onNodeWithContentDescription("login click").assertExists()
    }

    @Test
    fun `Check if app navigates to Main screen when authenticated`() = runComposeUiTest {
        every { mockkSession.sessionStatus() } returns flow { emit(mockSessionStatus) }
        setContentForRoot()

        waitUntil { root.childStack.value.active.instance is RootComponent.Child.MainScreenPager }
        MainPages.entries.forEach { navIcon ->
            onNodeWithText(getString(navIcon.titleRes)).assertExists()
        }
    }
}