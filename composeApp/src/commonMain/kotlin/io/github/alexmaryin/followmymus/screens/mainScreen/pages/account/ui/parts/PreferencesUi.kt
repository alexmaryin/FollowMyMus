package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.AccountAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent

@Composable
fun PreferencesUi(
    component: AccountHostComponent
) {
    Column {
        TextButton(
            onClick = { component(AccountAction.ShowAbout) }
        ) {
            Text("About")
        }
        TextButton(
            onClick = { component(AccountAction.ShowPrivacyPolicy) }
        ) {
            Text("Privacy policy")
        }
    }

}