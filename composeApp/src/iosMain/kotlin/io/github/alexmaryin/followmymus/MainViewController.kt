package io.github.alexmaryin.followmymus

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.ExperimentalDecomposeApi
import io.github.alexmaryin.followmymus.navigation.RootComponent
import io.github.alexmaryin.followmymus.navigation.ui.RootContent
import platform.UIKit.UIViewController

@OptIn(ExperimentalDecomposeApi::class)
fun MainViewController(root: RootComponent): UIViewController =
    ComposeUIViewController {
        RootContent(component = root)
    }