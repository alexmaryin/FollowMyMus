package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.about
import followmymus.composeapp.generated.resources.app_section_label
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.AccountCaption
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.GroupCaption
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.SoftCornerBlock
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.UserListItem
import org.jetbrains.compose.resources.stringResource

@Composable
fun PreferencesUi(
    component: AccountHostComponent
) {
    val state by component.state.subscribeAsState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.surfaceBright),
        verticalArrangement = Arrangement.Top
    ) {
        println(state)
        AccountCaption()
        SoftCornerBlock {
            UserListItem(state.nickname)
            AnimatedVisibility(visible = state.isQrVisible) {

            }
        }
        GroupCaption(stringResource(Res.string.app_section_label))
        SoftCornerBlock {

        }
        GroupCaption(stringResource(Res.string.about))
        SoftCornerBlock {

        }

    }

}