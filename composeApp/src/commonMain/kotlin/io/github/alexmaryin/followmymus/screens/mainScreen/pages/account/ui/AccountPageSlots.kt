package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.alexmaryin.followmymus.screens.commonUi.BackIcon
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent

class AccountPageSlots(
    private val component: AccountHostComponent
) : ScaffoldSlots by DefaultScaffoldSlots  {

    override val leadingIcon = @Composable {
        val state = component.state.subscribeAsState()
        if (state.value.backVisible) BackIcon {

        }
    }
}