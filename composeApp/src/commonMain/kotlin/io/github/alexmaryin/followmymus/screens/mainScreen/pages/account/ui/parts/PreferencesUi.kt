package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import followmymus.composeapp.generated.resources.*
import io.github.alexmaryin.followmymus.BuildKonfig
import io.github.alexmaryin.followmymus.core.changeLanguage
import io.github.alexmaryin.followmymus.core.ui.isAndroid
import io.github.alexmaryin.followmymus.core.ui.isIOS
import io.github.alexmaryin.followmymus.preferences.DynamicMode
import io.github.alexmaryin.followmymus.preferences.Language
import io.github.alexmaryin.followmymus.preferences.ThemeMode
import io.github.alexmaryin.followmymus.preferences.rememberAppPreferences
import io.github.alexmaryin.followmymus.preferences.rememberPrefs
import io.github.alexmaryin.followmymus.screens.commonUi.ConfirmationDialog
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.AccountAction
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.PreferencesItem
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.TrailingIconType
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.AccountCaption
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.PreferencesGroup
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.QRGeneratedBlock
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.SoftCornerBlock
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.UserListItem
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun PreferencesUi(
    component: AccountHostComponent
) {
    val state by component.state.subscribeAsState()
    val preferences = rememberAppPreferences(rememberPrefs())
    val theme by preferences.getThemeMode().collectAsStateWithLifecycle(ThemeMode.SYSTEM)
    val language by preferences.getLanguage().collectAsStateWithLifecycle(Language.SYSTEM)
    val dynamicMode by preferences.getAndroidDynamicMode().collectAsStateWithLifecycle(DynamicMode.ON)
    val scope = rememberCoroutineScope()

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

    ThemeModalUi(
        isOpened = state.isThemeModalOpened,
        selectedOption = stringResource(theme.caption),
        onSelect = { new ->
            scope.launch { preferences.changeThemeMode(ThemeMode.fromCaption(new)) }
        },
        onDismiss = { component(AccountAction.ThemeClick) }
    )

    LanguageModalUi(
        isOpened = state.isLanguageModalOpened,
        selectedOption = stringResource(language.caption),
        onSelect = { new ->
            scope.launch { preferences.changeLanguage(Language.fromCaption(new)) }
        },
        onDismiss = { component(AccountAction.LanguageClick) }
    )

    DynamicModalUi(
        isOpened = state.isDynamicModalOpened,
        selectedOption = stringResource(dynamicMode.caption),
        onSelect = { new ->
            scope.launch { preferences.changeAndroidDynamicMode(DynamicMode.fromCaption(new)) }
        },
        onDismiss = { component(AccountAction.DynamicClick) }
    )

    if (state.sessionLogout) {
        Box(modifier = Modifier.fillMaxSize()) {
            LogoutShimmer(
                text = stringResource(Res.string.logout_shimmer),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    } else Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.surfaceContainerLow),
        verticalArrangement = Arrangement.Top
    ) {
        AccountCaption()
        SoftCornerBlock {
            UserListItem(
                nickname = state.nickname,
                isQrOpened = state.deepLink != null,
                onQrToggle = { component(AccountAction.ToggleQrView) },
                onLogout = { logoutDialogVisible = true }
            )
            AnimatedVisibility(visible = state.deepLink != null) {
                state.deepLink?.let {
                    QRGeneratedBlock(
                        deepLink = it,
                        onDownloadClick = { image -> component(AccountAction.DownloadQR(image)) },
                        onExpired = { component(AccountAction.ToggleQrView) }
                    )
                }
            }
        }

        val preferencesItems = buildList {
            add(
                PreferencesItem(
                    text = stringResource(Res.string.language_preferences_label),
                    leadingIconRes = Res.drawable.language,
                    trailingText = stringResource(
                        if (isIOS()) Language.SYSTEM.caption else language.caption
                    ),
                    onClick = {
                        if (isIOS()) scope.launch { changeLanguage(null) }
                        else component(AccountAction.LanguageClick)
                    }
                ))
            add(
                PreferencesItem(
                    text = stringResource(Res.string.theme_preferences_label),
                    leadingIconRes = Res.drawable.theme,
                    trailingText = stringResource(theme.caption),
                    onClick = { component(AccountAction.ThemeClick) }
                ))
            if (isAndroid()) add(
                PreferencesItem(
                    text = stringResource(Res.string.dynamic_preferences_label),
                    leadingIconRes = Res.drawable.dynamic_colors,
                    trailingText = stringResource(dynamicMode.caption),
                    onClick = { component(AccountAction.DynamicClick) }
                ))
        }.toTypedArray()

        PreferencesGroup(
            groupCaption = stringResource(Res.string.app_section_label),
            *preferencesItems,
        )

        PreferencesGroup(
            groupCaption = stringResource(Res.string.about),
            PreferencesItem(
                text = stringResource(Res.string.privacy_policy),
                leadingIconRes = Res.drawable.privacy,
                type = TrailingIconType.FORWARD,
                onClick = { component(AccountAction.ShowPrivacyPolicy) }
            ),
            PreferencesItem(
                text = stringResource(Res.string.about),
                leadingIconRes = Res.drawable.info,
                type = TrailingIconType.FORWARD,
                onClick = { component(AccountAction.ShowAbout) }
            ),
            PreferencesItem(
                text = stringResource(Res.string.current_version),
                leadingIconRes = Res.drawable.version,
                type = TrailingIconType.FORWARD,
                trailingText = BuildKonfig.appVersion,
                onClick = {}
            )
        )
    }
}