package io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.ui.artistsPanel.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.search
import followmymus.composeapp.generated.resources.search_placeholder
import followmymus.composeapp.generated.resources.tune
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.artistsListPanel.ArtistsListAction
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun ArtistsSearchBar(
    action: (ArtistsListAction) -> Unit
) {
    val searchState = rememberSearchBarState()
    val queryState = rememberTextFieldState()
    val focusManager = LocalFocusManager.current
    val searchFun = { query: String ->
        queryState.clearText()
        action(ArtistsListAction.Search(query))
        focusManager.clearFocus()
    }
    SearchBar(
        state = searchState,
        modifier = Modifier.fillMaxWidth(),
        inputField = {
            SearchBarDefaults.InputField(
                textFieldState = queryState,
                searchBarState = searchState,
                onSearch = searchFun,
                placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.tune),
                        contentDescription = "search options",
                        modifier = Modifier.clickable { action(ArtistsListAction.ToggleSearchTune) }
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.search),
                        contentDescription = "start search",
                        modifier = Modifier.clickable { searchFun(queryState.text.toString()) }
                    )
                },
                colors = SearchBarDefaults.inputFieldColors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    )
}

@Preview
@Composable
fun PreviewSearchBar() {
    Surface {
        Box(modifier = Modifier.padding(6.dp)) {
            ArtistsSearchBar {}
        }

    }
}