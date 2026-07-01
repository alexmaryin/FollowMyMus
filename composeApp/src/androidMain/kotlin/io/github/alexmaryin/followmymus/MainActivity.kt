package io.github.alexmaryin.followmymus

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import io.github.alexmaryin.followmymus.core.system.FileHandler
import io.github.alexmaryin.followmymus.rootNavigation.MainRootComponent
import io.github.alexmaryin.followmymus.rootNavigation.ui.RootContent
import io.github.alexmaryin.followmymus.sessionManager.data.qrcode.transferSession
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class MainActivity : AppCompatActivity() {

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val id = data.getQueryParameter("id") ?: return
        val channel by inject<RealtimeChannel> { parametersOf(id) }
        val sessionManager by inject<SessionManager>()
        lifecycleScope.launch { channel.transferSession(sessionManager) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        FileHandler.bindActivity(this)

        val root = MainRootComponent(defaultComponentContext())

        handleDeepLink(intent)

        setContent {
            RootContent(root)
        }
    }
}