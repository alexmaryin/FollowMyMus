package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.releasesPanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.releases_placeholder
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun ReleasesPlaceholder() {
    Box(modifier = Modifier.fillMaxSize()
        .background(color = MaterialTheme.colorScheme.surfaceDim)) {
        Text(
            text = stringResource(Res.string.releases_placeholder),
            modifier = Modifier.align(Alignment.Center).padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

