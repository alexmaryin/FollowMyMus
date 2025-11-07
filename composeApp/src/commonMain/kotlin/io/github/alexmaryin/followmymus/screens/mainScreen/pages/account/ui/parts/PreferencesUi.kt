package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.about
import followmymus.composeapp.generated.resources.app_section_label
import followmymus.composeapp.generated.resources.logout_dialog_text
import followmymus.composeapp.generated.resources.logout_dialog_title
import followmymus.composeapp.generated.resources.logout_shimmer
import io.github.alexmaryin.followmymus.core.ui.modifiers.animatedShimmerBrush
import io.github.alexmaryin.followmymus.screens.commonUi.ConfirmationDialog
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.AccountAction
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

    var logoutDialogVisible by remember { mutableStateOf(false) }

    if (logoutDialogVisible) ConfirmationDialog(
        title = stringResource(Res.string.logout_dialog_title),
        text = stringResource(Res.string.logout_dialog_text),
        onConfirm = {
            logoutDialogVisible = false
            component(AccountAction.Logout)
        },
        onDismiss = { logoutDialogVisible = false }
    )

    if (state.sessionLogout) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(Res.string.logout_shimmer),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).animatedShimmerBrush(true)
            )
        }
    } else Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.surfaceBright),
        verticalArrangement = Arrangement.Top
    ) {
        AccountCaption()
        SoftCornerBlock {
            UserListItem(
                nickname = state.nickname,
                onQrToggle = { component(AccountAction.ToggleQrView) },
                onLogout = { logoutDialogVisible = true }
            )
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