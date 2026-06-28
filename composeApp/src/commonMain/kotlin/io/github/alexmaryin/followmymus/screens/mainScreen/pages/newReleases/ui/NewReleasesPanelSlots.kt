package io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.releases_page_title
import io.github.alexmaryin.followmymus.preferences.AppSettings
import io.github.alexmaryin.followmymus.preferences.getAppSettings
import io.github.alexmaryin.followmymus.preferences.rememberAppPreferences
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.DefaultScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.ScaffoldSlots
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.SnackbarMsg
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.newReleases.domain.list.NewReleasesList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import org.jetbrains.compose.resources.stringResource

class NewReleasesPanelSlots(
    component: NewReleasesList
) : ScaffoldSlots by DefaultScaffoldSlots {

    override val titleContent = @Composable {
        val preferences = rememberAppPreferences()
        val settings by preferences.getAppSettings().collectAsStateWithLifecycle(AppSettings(null, null))
        Column {
            Text(text = stringResource(Res.string.releases_page_title))
            settings.newReleasesLastSyncCompletedAt?.let { lastSync ->
                Text(
                    text = "Last synced: ${lastSync.toString().take(16).replace('T', ' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    override val snackbarMessages: Flow<SnackbarMsg> =
        component.events.receiveAsFlow().distinctUntilChanged()
}
