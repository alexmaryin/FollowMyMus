package io.github.alexmaryin.followmymus.screens.mainScreen.pages.sharedPanels.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.artists_search_header
import org.jetbrains.compose.resources.stringResource

@Composable
fun ListHeader(caption: String) {
    Surface {
        Text(
            text = caption,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primaryFixedDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(6.dp)
        )
    }
}

@Preview
@Composable
fun SearchHeaderPreview() {
    Surface {
        ListHeader(
            stringResource(
                Res.string.artists_search_header,
                26
            )
        )
    }
}