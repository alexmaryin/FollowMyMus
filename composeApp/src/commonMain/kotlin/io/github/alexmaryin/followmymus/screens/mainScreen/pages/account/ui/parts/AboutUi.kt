package io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.ui.parts

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.about_text
import io.github.alexmaryin.followmymus.core.ui.parseSimpleMarkdown
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutUi() {
val text = stringResource(Res.string.about_text)
    Text(
        text = parseSimpleMarkdown(text),
        modifier = Modifier.padding(12.dp)
            .verticalScroll(rememberScrollState())
    )
}