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
import io.github.alexmaryin.followmymus.screens.splash.SplashScreen
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.jan.supabase.auth.status.SessionStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RootNavigationTests {

    val mockkSession = mockk<SessionManager>()
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

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<SessionManager> { mockkSession }
                    factory<LoginComponent> { loginComponent }
                }
            )
        }
    }

    @Test
    fun `Check if app splash starts with Logo`() = runComposeUiTest {
        setContent { SplashScreen() }
        awaitIdle()
        "FollowMyMus".forEach { char ->
            onAllNodesWithText(char.toString()).onFirst().assertExists()
        }
        stopKoin()
    }

    @Test
    fun `Check if app navigates to Login if not authenticated`() = runComposeUiTest {

        every { mockkSession.sessionStatus() } returns flow { emit(SessionStatus.NotAuthenticated()) }

        val lifecycle = com.arkivanov.essenty.lifecycle.LifecycleRegistry()
        val root = runOnUiThread {
            MainRootComponent(DefaultComponentContext(lifecycle))
        }
        setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                lifecycle.start()
                RootContent(root)
            }
        }

        awaitIdle()

        waitUntil { root.childStack.value.active.instance is RootComponent.Child.LoginChild }
        onNodeWithContentDescription("login click").assertExists()

        stopKoin()
    }
}