package io.github.alexmaryin.followmymus.screens.signUp.ui.parts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.sign_up
import followmymus.composeapp.generated.resources.sign_up_button
import io.github.alexmaryin.followmymus.core.ui.modifiers.animatedShimmerBrush
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SignUpButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        enabled = isEnabled,
        onClick = onClick,
        modifier = modifier
            .sizeIn(maxWidth = 300.dp)
            .fillMaxWidth()
            .padding(16.dp)
            .animatedShimmerBrush(showShimmer = !isEnabled, shape = ButtonDefaults.shape)
    ) {
        Icon(
            painter = painterResource(Res.drawable.sign_up),
            contentDescription = "sign up click"
        )
        Text(
            text = stringResource(Res.string.sign_up_button),
            modifier = Modifier.padding(6.dp)
        )
    }
}