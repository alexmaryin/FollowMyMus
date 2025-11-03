package io.github.alexmaryin.followmymus.screens.login.domain

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.navigation.ui.saveableMutableValue
import io.github.alexmaryin.followmymus.sessionManager.data.transferSession
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Factory(binds = [LoginComponent::class])
class CMPLoginComponent(
    qrCode: String?,
    private val componentContext: ComponentContext,
    private val onSignUpClick: () -> Unit
) : LoginComponent, ComponentContext by componentContext, KoinComponent {

    private val _state by saveableMutableValue(LoginState.serializer(), init = ::LoginState)
    override val state get() = _state

    private val eventChannel = Channel<LoginEvent>()
    override val events = eventChannel.receiveAsFlow()

    private val scope = componentContext.coroutineScope() + SupervisorJob()

    private val sessionManager by inject<SessionManager>()

    init {
        qrCode?.let { sessionCode ->
            scope.launch { loginBySessionCode(sessionCode) }
        }

    }

    override operator fun invoke(action: LoginAction) {
        when (action) {
            LoginAction.OnLogin -> scope.launch {
                login(
                    nickname = _state.value.nickname,
                    password = _state.value.password
                )
            }

            is LoginAction.OnQrRecognized -> scope.launch {
                loginBySessionCode(action.qrCode)
            }

            is LoginAction.OnNickNameSet -> _state.update { it.copy(nickname = action.new, isCredentialsValid = true) }

            is LoginAction.OnPasswordSet -> _state.update { it.copy(password = action.new, isCredentialsValid = true) }

            LoginAction.OnOpenSignUp -> onSignUpClick()

            LoginAction.OnOpenQrScan -> _state.update { it.copy(isQrScanOpen = true) }

            LoginAction.OnCloseQrScan -> _state.update { it.copy(isQrScanOpen = false) }
        }
    }

    private suspend fun login(nickname: String, password: String) {
        _state.update { it.copy(isLoading = true, isCredentialsValid = true) }
        val result = sessionManager.signIn(Credentials(nickname, password))
        result.forError { error, message ->
            val msg = when (error) {
                is SessionError.AuthError -> {
                    when (error.code) {
                        AuthErrorCode.InvalidCredentials -> {
                            _state.update { it.copy(isCredentialsValid = false) }
                            getString(Res.string.credentials_invalid)
                        }

                        else -> getString(Res.string.auth_error) + error.code.value +
                                "\n" + message
                    }
                }

                is SessionError.RestError -> getString(Res.string.rest_error) + error.statusCode +
                        "\n" + message

                else -> getString(Res.string.network_error) + "\n" + message
            }
            eventChannel.send(LoginEvent.ShowError(msg))
            _state.update { it.copy(isLoading = false) }
        }
        result.forSuccess {
            _state.update { it.copy(isLoading = false, isCredentialsValid = true) }
        }
    }

    private suspend fun loginBySessionCode(sessionCode: String) {
        _state.update { it.copy(isCredentialsValid = true, isLoading = true) }
        val channel by inject<RealtimeChannel> { parametersOf(sessionCode) }
        channel.transferSession(sessionManager)
        _state.update { it.copy(isLoading = false) }
    }
}