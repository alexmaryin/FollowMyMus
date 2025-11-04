package io.github.alexmaryin.followmymus

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import io.github.alexmaryin.followmymus.rootNavigation.MainRootComponent
import io.github.alexmaryin.followmymus.rootNavigation.ui.RootContent
import io.github.alexmaryin.followmymus.sessionManager.data.transferSession
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class MainActivity : ComponentActivity() {

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val channel by inject<RealtimeChannel> { parametersOf(data.getQueryParameter("id")) }
        val sessionManager by inject<SessionManager>()
        lifecycleScope.launch { channel.transferSession(sessionManager) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val root = MainRootComponent(defaultComponentContext())

        handleDeepLink(intent)

        setContent {
            RootContent(root)
        }
    }
}