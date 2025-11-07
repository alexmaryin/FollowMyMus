package io.github.alexmaryin.followmymus.sessionManager.data.qrcode

import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.ChannelMessage
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionPayload
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow

const val DEEP_LINK_URL_PREFIX = "https://alexmaryin.ru/session-transfer?id="

suspend fun RealtimeChannel.transferSession(sessionManager: SessionManager) {
    subscribe(true)
    broadcast("transfer_request", ChannelMessage("start"))
    val broadcast = broadcastFlow<SessionPayload>(event = "session_payload")
    broadcast.collect { payload ->
        val result = sessionManager.transferSession(payload)
        result.forSuccess { userInfo ->
            println("\nSESSION TRANSFERRED AND UPDATED FOR USER:\n\n$userInfo")
            broadcast("transfer_request", ChannelMessage("success"))
        }
        result.forError {
            broadcast("transfer_request", ChannelMessage("error"))
        }
        unsubscribe()
    }
}
