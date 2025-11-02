package io.github.alexmaryin.followmymus.screens.signUp.ui.parts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.label_to_login
import followmymus.composeapp.generated.resources.link_to_login
import io.github.alexmaryin.followmymus.screens.commonUi.TextWithLink
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginLink(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) = TextWithLink(
    text = stringResource(Res.string.label_to_login),
    linkText = stringResource(Res.string.link_to_login),
    modifier = modifier.fillMaxWidth().padding(16.dp),
    onClick = onClick
)
