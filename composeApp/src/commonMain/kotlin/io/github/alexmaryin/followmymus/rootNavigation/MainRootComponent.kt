package io.github.alexmaryin.followmymus.rootNavigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.rootNavigation.RootComponent.Child
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PagerComponent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.getNickname
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class MainRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val sessionManager by inject<SessionManager>()
    private val scope = componentContext.coroutineScope() + SupervisorJob()

    init {
        lifecycle.doOnStart {
            observeSessionStatus()
        }
    }

    private val navigation = StackNavigation<Config>()

    override val childStack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Splash,
        handleBackButton = true
    ) { config, context ->
        when (config) {
            is Config.Splash -> Child.Splash

            is Config.Login -> Child.LoginChild(
                login(config.qrCode, context) { navigation.replaceCurrent(Config.SignUp) }
            )

            is Config.MainScreen -> Child.MainScreenPager(
                mainScreen(context, config.nickname)
            )

            Config.Settings -> TODO()

            Config.SignUp -> Child.SignUpChild(
                signUp(context) { navigation.replaceCurrent(Config.Login()) }
            )
        }
    }

    private fun login(
        qrCode: String? = null,
        componentContext: ComponentContext,
        onSignUpClick: () -> Unit
    ) = get<LoginComponent> { parametersOf(qrCode, componentContext, onSignUpClick) }

    private fun signUp(
        componentContext: ComponentContext,
        onLoginClick: () -> Unit
    ) = get<SignUpComponent> { parametersOf(componentContext, onLoginClick) }

    private fun mainScreen(
        componentContext: ComponentContext,
        nickName: String
    ) = get<PagerComponent> { parametersOf(componentContext, nickName) }

    private fun observeSessionStatus() = scope.launch {

//        val splashDelay = async { delay(2500L) }
        sessionManager.sessionStatus().collectLatest { sessionStatus ->
            println(sessionStatus)
//            splashDelay.await()
            when (sessionStatus) {
                is SessionStatus.Authenticated -> navigation.replaceAll(
                    Config.MainScreen(
                        nickname = sessionStatus.session.getNickname() ?: "Anonymous"
                    )
                )

                is SessionStatus.NotAuthenticated,
                is SessionStatus.RefreshFailure -> navigation.replaceAll(Config.Login())

                else -> Unit
            }
        }
    }
}