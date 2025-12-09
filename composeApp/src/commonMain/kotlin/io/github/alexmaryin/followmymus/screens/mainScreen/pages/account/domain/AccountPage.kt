package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import io.github.alexmaryin.followmymus.core.data.saveableMutableValue
import io.github.alexmaryin.followmymus.core.system.FileHandler
import io.github.alexmaryin.followmymus.musicBrainz.data.repository.RoomRepository
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountPageConfig
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.AccountPageSlots
import io.github.alexmaryin.followmymus.sessionManager.data.qrcode.DEEP_LINK_URL_PREFIX
import io.github.alexmaryin.followmymus.sessionManager.data.qrcode.startTransferSession
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Factory(binds = [AccountHostComponent::class])
class AccountPage(
    private val sessionManager: SessionManager,
    private val repository: RoomRepository,
    private val componentContext: ComponentContext,
    private val nickname: String,
) : AccountHostComponent, ComponentContext by componentContext, KoinComponent {
    private val _state by saveableMutableValue(
        AccountPageState.serializer(),
        init = { AccountPageState(nickname = nickname) })
    override val state get() = _state

    override val events = Channel<SnackbarMsg>()
    override val scaffoldSlots = AccountPageSlots(this)

    private val navigation = StackNavigation<AccountPageConfig>()

    private var sessionTransferJob: Job? = null
    private var transferChannel: RealtimeChannel? = null

    private val scope = componentContext.coroutineScope() + SupervisorJob()

    override val childStack: Value<ChildStack<*, AccountHostComponent.Child>> = childStack(
        source = navigation,
        serializer = AccountPageConfig.serializer(),
        initialConfiguration = AccountPageConfig.Account(nickname),
        handleBackButton = true
    ) { config, _ ->
        when (config) {
            is AccountPageConfig.Account -> AccountHostComponent.Child.Account(config.nickname)
            AccountPageConfig.About -> AccountHostComponent.Child.About
            AccountPageConfig.PrivacyPolicy -> AccountHostComponent.Child.PrivacyPolicy
        }
    }


    override fun invoke(action: AccountAction) {
        when (action) {
            AccountAction.ShowAbout -> {
                _state.update { it.copy(backVisible = true) }
                navigation.bringToFront(AccountPageConfig.About)
            }

            AccountAction.ShowPrivacyPolicy -> {
                _state.update { it.copy(backVisible = true) }
                navigation.bringToFront(AccountPageConfig.PrivacyPolicy)
            }

            AccountAction.Logout -> scope.launch {
                _state.update { it.copy(sessionLogout = true) }
                repository.clearLocalData()
                sessionManager.signOut()
            }

            AccountAction.ToggleQrView -> toggleTransferringSession()

            AccountAction.LanguageClick ->
                _state.update { it.copy(isLanguageModalOpened = !state.value.isLanguageModalOpened) }

            AccountAction.ThemeClick ->
                _state.update { it.copy(isThemeModalOpened = !state.value.isThemeModalOpened) }

            AccountAction.DynamicClick ->
                _state.update { it.copy(isDynamicModalOpened = !state.value.isDynamicModalOpened) }

            is AccountAction.LanguageChange -> _state.update { it.copy(language = action.language) }

            is AccountAction.ThemeChange -> _state.update { it.copy(theme = action.theme) }

            is AccountAction.DynamicChange -> _state.update { it.copy(dynamicMode = action.dynamicMode) }

            is AccountAction.DownloadQR -> scope.launch { FileHandler().saveQR(action.image) }

            AccountAction.OnBack -> {
                _state.update { it.copy(backVisible = false) }
                navigation.bringToFront(AccountPageConfig.Account(nickname))
            }
        }
    }

    private fun toggleTransferringSession() {
        if (sessionTransferJob != null) closeQRandStopTransfer()
        else sessionTransferJob = scope.launch { showQRandStartTransfer() }
    }

    private suspend fun showQRandStartTransfer() {
        val sessionId = Uuid.random().toString()
        val deeplink = DEEP_LINK_URL_PREFIX + sessionId
        _state.update { it.copy(deepLink = deeplink) }
        transferChannel = get<RealtimeChannel> { parametersOf(sessionId) }
        transferChannel?.startTransferSession(sessionManager)
        closeQRandStopTransfer()
    }

    private fun closeQRandStopTransfer() {
        _state.update { it.copy(deepLink = null) }
        scope.launch { transferChannel?.unsubscribe() }
        sessionTransferJob?.cancel()
        sessionTransferJob = null
        transferChannel = null
    }
}