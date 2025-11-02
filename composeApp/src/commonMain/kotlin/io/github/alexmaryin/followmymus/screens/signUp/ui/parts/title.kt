package io.github.alexmaryin.followmymus.screens.signUp.ui.parts

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.sign_up_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpTitle(
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(Res.string.sign_up_title),
        style = MaterialTheme.typography.displayLarge,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp)
    )
    Spacer(Modifier.height(24.dp))
}