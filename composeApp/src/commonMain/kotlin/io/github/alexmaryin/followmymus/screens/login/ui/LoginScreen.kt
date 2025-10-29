package io.github.alexmaryin.followmymus.screens.login.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent

@Composable
fun LoginScreen(
    component: LoginComponent
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "LOGIN SCREEN",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}