package io.github.alexmaryin.followmymus.screens.signUp.domain

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Factory(binds = [SignUpComponent::class])
class CMPSignUpComponent(
    private val componentContext: ComponentContext,
    private val onLoginClick: () -> Unit
) : SignUpComponent, ComponentContext by componentContext, KoinComponent {

    private val _state = MutableValue(SignUpState())
    override val state: Value<SignUpState> get() = _state

    private val scope = componentContext.coroutineScope() + SupervisorJob()

    private val sessionManager by inject<SessionManager>()

    override operator fun invoke(action: SignUpAction) {
        when (action) {
            SignUpAction.OnSignUp -> scope.launch {
                signUp(
                    nickname = _state.value.nickname.text.toString(),
                    password = _state.value.password.text.toString()
                )
            }
            SignUpAction.OnOpenLogin -> onLoginClick()
            SignUpAction.TogglePasswordVisibility -> _state.update {
                it.copy(isPasswordVisible = !it.isPasswordVisible)
            }
        }
    }

    private suspend fun signUp(nickname: String, password: String) {
        if (!_state.value.nicknameValidation) {
            _state.update { it.copy(isNicknameValid = false) }
            return
        }

        if (!_state.value.passwordValidation) {
            _state.update { it.copy(isPasswordValid = false) }
            return
        }

        _state.update { it.copy(isLoading = true, isNicknameValid = true, isPasswordValid = true) }
        val result = sessionManager.signUp(Credentials(nickname, password))
        _state.update { it.copy(isLoading = false) }

        result.forError {

        }
        result.forSuccess {

        }
    }

}