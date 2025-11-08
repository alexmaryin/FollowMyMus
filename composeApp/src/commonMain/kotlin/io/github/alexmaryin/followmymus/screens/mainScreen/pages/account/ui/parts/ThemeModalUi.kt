package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts

import androidx.compose.runtime.Composable
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.select_theme
import io.github.alexmaryin.followmymus.preferences.ThemeMode
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.components.OptionSelector
import org.jetbrains.compose.resources.stringResource

@Composable
fun ThemeModalUi(
    isOpened: Boolean,
    selectedOption: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (isOpened) OptionSelector(
        title = stringResource(Res.string.select_theme),
        options = ThemeMode.entries.map { stringResource(it.caption) },
        selectedOption = selectedOption,
        onSelect = { if (it != selectedOption) onSelect(it); onDismiss() },
        onDismiss = onDismiss
    )
}