package io.github.alexmaryin.followmymus.screens.signUp.domain

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.rootNavigation.ui.saveableMutableValue
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionError
import io.github.jan.supabase.auth.exception.AuthErrorCode
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Factory(binds = [SignUpComponent::class])
class CMPSignUpComponent(
    private val componentContext: ComponentContext,
    private val onLoginClick: () -> Unit
) : SignUpComponent, ComponentContext by componentContext, KoinComponent {

    private val _state by saveableMutableValue(SignUpState.serializer(), init = ::SignUpState)
    override val state: Value<SignUpState> get() = _state

    private val eventChannel = Channel<SignUpEvent>()
    override val events = eventChannel.receiveAsFlow()

    private val scope = componentContext.coroutineScope() + SupervisorJob()

    private val sessionManager by inject<SessionManager>()

    override operator fun invoke(action: SignUpAction) {
        when (action) {
            SignUpAction.OnSignUp -> scope.launch {
                signUp(
                    nickname = _state.value.nickname,
                    password = _state.value.password
                )
            }

            SignUpAction.OnOpenLogin -> onLoginClick()
            is SignUpAction.NicknameChange -> _state.update {
                it.copy(nickname = action.new, isNicknameValid = true)
            }

            is SignUpAction.PasswordChange -> _state.update {
                it.copy(password = action.new, isPasswordValid = true)
            }
        }
    }

    private suspend fun signUp(nickname: String, password: String) {
        if (!_state.value.nicknameValidation()) {
            _state.update { it.copy(isNicknameValid = false) }
            return
        }

        if (!_state.value.passwordValidation()) {
            _state.update { it.copy(isPasswordValid = false) }
            return
        }

        _state.update { it.copy(isLoading = true, isNicknameValid = true, isPasswordValid = true) }
        val result = sessionManager.signUp(Credentials(nickname, password))
        _state.update { it.copy(isLoading = false) }

        result.forError { error, message ->
            val msg = when (error) {
                is SessionError.AuthError -> {
                    when (error.code) {
                        AuthErrorCode.EmailExists -> {
                            _state.update { it.copy(isNicknameValid = false) }
                            getString(Res.string.email_exists)
                        }

                        AuthErrorCode.SignupDisabled -> getString(Res.string.signup_disabled)

                        AuthErrorCode.UserAlreadyExists -> {
                            _state.update { it.copy(isNicknameValid = false) }
                            getString(Res.string.email_exists)
                        }

                        AuthErrorCode.WeakPassword -> {
                            _state.update { it.copy(isPasswordValid = false) }
                            getString(Res.string.weak_password)
                        }

                        else -> getString(Res.string.auth_error) + error.code.value +
                                "\n" + message
                    }
                }

                is SessionError.RestError -> getString(Res.string.rest_error) + error.statusCode +
                        "\n" + message

                else -> getString(Res.string.network_error) + "\n" + message
            }
            eventChannel.send(SignUpEvent.ShowError(msg))
        }
    }
}