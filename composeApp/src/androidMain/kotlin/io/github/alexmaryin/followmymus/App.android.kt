package io.github.alexmaryin.followmymus

import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.alexmaryin.followmymus.qrScanner.QRCodeScannerScreen
import io.github.alexmaryin.followmymus.qrScanner.transferSession
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.jan.supabase.realtime.RealtimeChannel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatform

@Composable
actual fun QRCodeBlock(modifier: Modifier) {
    val sessionManager = koinInject<SessionManager>()
    val scope = rememberCoroutineScope()
    var showCamera by remember { mutableStateOf(true) }

    if (showCamera) {
        QRCodeScannerScreen(
            modifier = Modifier.size(250.dp),
            onQrDetected = { qr ->
                try {
                    val id = qr.substringAfter("id=")
                    Log.d("QR", "Recognized QR code with session Id=$id")
                    val channel by KoinPlatform.getKoin().inject<RealtimeChannel> { parametersOf(id) }
                    scope.launch { channel.transferSession(sessionManager) }
                    showCamera = false
                } catch (e: Exception) {
                    Log.e("QR", "Error during QR recognition: ${e.message}")
                    showCamera = false
                }
            },
            onCancel = { showCamera = false }
        )
    } else {
        Button(onClick = { showCamera = true }) {
            Text("Scan QR code")
        }
    }
}