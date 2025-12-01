package io.github.alexmaryin.followmymus.screens.mainScreen.domain

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.app_name
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource

interface ScaffoldSlots {
    val titleContent: @Composable () -> Unit
    val leadingIcon: @Composable () -> Unit
    val trailingIcon: @Composable RowScope.() -> Unit
    val fabContent: @Composable () -> Unit
    val snackbarMessages: Flow<String>
}

object DefaultScaffoldSlots : ScaffoldSlots {
    override val snackbarMessages = emptyFlow<String>()
    override val fabContent: @Composable () -> Unit = {}
    override val leadingIcon: @Composable () -> Unit = {}
    override val trailingIcon: @Composable RowScope.() -> Unit = {}
    override val titleContent: @Composable () -> Unit = {
        Text(stringResource(Res.string.app_name))
    }
}

