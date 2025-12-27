package io.github.alexmaryin.followmymus

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.essenty.lifecycle.start
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.empty_favorites_placeholder
import io.github.alexmaryin.followmymus.musicBrainz.domain.ArtistsRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SyncRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.models.RemoteSyncStatus
import io.github.alexmaryin.followmymus.rootNavigation.MainRootComponent
import io.github.alexmaryin.followmymus.rootNavigation.RootComponent
import io.github.alexmaryin.followmymus.rootNavigation.ui.RootContent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginAction
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginEvent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginState
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.MainPagerComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PagerComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHost
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesHostComponent
import io.github.alexmaryin.followmymus.screens.splash.SplashScreen
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.getString
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class RootTest {

    val lifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle
            get() = registry
    }

    private val lifecycle = com.arkivanov.essenty.lifecycle.LifecycleRegistry()
    private val context = DefaultComponentContext(lifecycle)
    private lateinit var root: RootComponent

    val mockkSession = mockk<SessionManager>()
    val mockRepository = mockk<SyncRepository> {
        every { hasPendingActions } returns MutableStateFlow(false)
        every { syncStatus } returns MutableStateFlow(RemoteSyncStatus.Idle)
        coEvery { syncRemote() } coAnswers {}
        coEvery { checkPendingActions() } coAnswers {}
    }

    val mockArtists = mockk<ArtistsRepository> {
        every { searchCount } returns MutableStateFlow(0)
        every { getFavoriteArtists(any()) } returns flowOf(emptyMap())
        every { getFavoriteArtistsIds() } returns flowOf(emptyList())
    }

    val mockSessionStatus = mockk<SessionStatus.Authenticated> {
        every { session.user } returns mockk<UserInfo> {
            every { email } returns "mockk" + Credentials.SUFFIX
        }
    }

    val loginComponent = object : LoginComponent {
        override val state = MutableValue(LoginState())
        override val events = emptyFlow<LoginEvent>()
        override fun invoke(action: LoginAction) {}
    }

    @OptIn(ExperimentalTestApi::class)
    @BeforeTest
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<SessionManager> { mockkSession }
                    factory<LoginComponent> { loginComponent }
                    factory<PagerComponent> { MainPagerComponent(context, "mockk") }
                    factory<FavoritesHostComponent> {
                        FavoritesHost(
                            mockRepository,
                            context,
                            "mockk"
                        )
                    }
                    factory<ArtistsRepository> { mockArtists }
                    factory<AccountHostComponent> { mockk() }
                }
            )
        }

    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    private suspend fun ComposeUiTest.setContentForRoot() {
        root = MainRootComponent(context)
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
    fun `Check if app navigates to Main if authenticated`() = runComposeUiTest {
        every { mockkSession.sessionStatus() } returns flow { emit(mockSessionStatus) }

        setContentForRoot()

        waitUntil { root.childStack.value.active.instance is RootComponent.Child.MainScreenPager }
        val text = getString(Res.string.empty_favorites_placeholder)
        onNodeWithText(text).assertExists()
    }
}