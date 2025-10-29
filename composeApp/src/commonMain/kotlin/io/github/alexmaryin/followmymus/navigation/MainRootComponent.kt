package io.github.alexmaryin.followmymus.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnStart
import io.github.alexmaryin.followmymus.navigation.RootComponent.Child
import io.github.alexmaryin.followmymus.navigation.mainScreenPages.MainPagerComponent
import io.github.alexmaryin.followmymus.screens.login.domain.CMPLoginComponent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.CMPSignUpComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class MainRootComponent(
    componentContext: ComponentContext
) : RootComponent, ComponentContext by componentContext, KoinComponent {

    private val sessionManager by inject<SessionManager>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

            Config.MainScreen -> Child.MainScreenPager(MainPagerComponent(context))

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

    private fun observeSessionStatus() = scope.launch {
        delay(3000L)
        sessionManager.sessionStatus().collectLatest { sessionStatus ->
            println(sessionStatus)
            when (sessionStatus) {
                is SessionStatus.Authenticated -> navigation.replaceAll(Config.MainScreen)

                is SessionStatus.NotAuthenticated,
                is SessionStatus.RefreshFailure -> navigation.replaceAll(Config.Login())

                else -> Unit
            }
        }
    }
}