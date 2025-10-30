package io.github.alexmaryin.followmymus.screens.mainScreen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalWideNavigationRail
import androidx.compose.material3.Text
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.peope
import org.jetbrains.compose.resources.painterResource

@Composable
fun NavigationRailContent(
    modifier: Modifier = Modifier,
    nickname: String,
    onSettingsClick: () -> Unit,
    onSignOutClick: () -> Unit
) = ModalWideNavigationRail(
    modifier = modifier,
    state = rememberWideNavigationRailState(),
    header = {
        Row(modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.Center) {
            Icon(
                painter = painterResource(Res.drawable.peope),
                contentDescription = "Account details"
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
) {

}