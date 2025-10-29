package io.github.alexmaryin.followmymus.screens.signUp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent

@Composable
fun SignUpScreen(
    component: SignUpComponent
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "SIGN UP SCREEN",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}