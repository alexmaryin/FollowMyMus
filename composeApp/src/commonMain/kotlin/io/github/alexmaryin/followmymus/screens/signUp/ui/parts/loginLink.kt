package io.github.alexmaryin.followmymus.screens.signUp.ui.parts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.label_to_login
import followmymus.composeapp.generated.resources.link_to_login
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoginLInk(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.label_to_login),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(Res.string.link_to_login),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onClick() }
        )
    }
}