package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts.AboutUi
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts.PreferencesUi
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts.PrivacyPolicyUi

@Composable
fun AccountPageUi(
    component: AccountHostComponent
) {
    Children(
        stack = component.childStack,
        animation = stackAnimation(slide() + fade())
    ) {
        when (it.instance) {
            AccountHostComponent.Child.About -> AboutUi()
            is AccountHostComponent.Child.Account -> PreferencesUi(component)
            AccountHostComponent.Child.PrivacyPolicy -> PrivacyPolicyUi()
        }
    }
}


