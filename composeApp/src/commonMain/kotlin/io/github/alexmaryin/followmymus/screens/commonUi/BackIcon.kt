package io.github.alexmaryin.followmymus.screens.commonUi

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackIcon(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(Res.drawable.back),
            contentDescription = stringResource(Res.string.back)
        )
    }
}