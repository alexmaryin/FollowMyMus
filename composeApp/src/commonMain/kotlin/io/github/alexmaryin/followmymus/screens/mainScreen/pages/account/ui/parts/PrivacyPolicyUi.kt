package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.privacy_policy_text
import io.github.alexmaryin.followmymus.core.ui.parseSimpleMarkdown
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivacyPolicyUi() {
    val text = stringResource(Res.string.privacy_policy_text)
    Text(
        text = parseSimpleMarkdown(text),
        modifier = Modifier.verticalScroll(rememberScrollState())
    )
}