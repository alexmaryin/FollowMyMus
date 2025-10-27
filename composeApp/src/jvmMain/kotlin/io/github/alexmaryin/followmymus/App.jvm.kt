package io.github.alexmaryin.followmymus

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.sessionManager.data.DEEP_LINK_URL_PREFIX
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.ChannelMessage
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionPayload
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import org.jetbrains.skia.Image
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import qrcode.QRCode
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
actual fun QRCodeBlock(modifier: Modifier) {
    val transferId = Uuid.random().toString()
    val deepLink = "$DEEP_LINK_URL_PREFIX$transferId"
    val painter = QRCode.ofCircles()
        .withSize(15)
        .build(deepLink)
        .render()
    val image = Image.makeFromEncoded(painter.getBytes()).toComposeImageBitmap()

    Image(
        bitmap = image,
        contentDescription = "QR code for login in mobile app",
        modifier = modifier
    )

    val channel = koinInject<RealtimeChannel> { parametersOf(transferId) }
    val sessionManager = koinInject<SessionManager>()

    LaunchedEffect(Unit) {
        channel.subscribe(true)
        val broadcast = channel.broadcastFlow<ChannelMessage>(event = "transfer_request")
        broadcast.collect { msg ->
            if (msg.message == "start") {
                val session = sessionManager.currentSession()
                session.forSuccess {
                    val payload = SessionPayload(
                        accessToken = it.accessToken,
                        refreshToken = it.refreshToken,
                        expiresIn = it.expiresIn,
                        tokenType = it.tokenType,
                        user = it.user
                    )
                    channel.broadcast("session_payload", payload)
                }
                session.forError { type, message ->
                    println("ERROR $type: $message")
                }
            } else {
                channel.unsubscribe()
            }
        }
    }
}