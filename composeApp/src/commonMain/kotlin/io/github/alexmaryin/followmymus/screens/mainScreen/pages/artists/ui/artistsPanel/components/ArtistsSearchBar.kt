package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.search
import followmymus.composeapp.generated.resources.search_placeholder
import followmymus.composeapp.generated.resources.tune
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ArtistsSearchBar(
    action: (ArtistsListAction) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFun = {
        val text = query
        if (text.isNotBlank()) {
            query = ""
            action(ArtistsListAction.Search(text))
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }
    TextField(
        value = query,
        onValueChange = { query = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(Res.string.search_placeholder)) },
        leadingIcon = {
            IconButton(
                onClick = { action(ArtistsListAction.ToggleSearchTune) }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.tune),
                    contentDescription = "search options",
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = searchFun
            ) {
                Icon(
                    painter = painterResource(Res.drawable.search),
                    contentDescription = "start search",
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { searchFun() }),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        )
    )
}

@Preview
@Composable
fun PreviewSearchBar() {
    ArtistsSearchBar {}
}
