package io.github.alexmaryin.followmymus.sessionManager.data

import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.ChannelMessage
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionPayload
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow

suspend fun RealtimeChannel.transferSession(sessionManager: SessionManager) {
    subscribe(true)
    broadcast("transfer_request", ChannelMessage("start"))
    val broadcast = broadcastFlow<SessionPayload>(event = "session_payload")
    broadcast.collect { payload ->
        val result = sessionManager.transferSession(payload)
        result.forSuccess {
            broadcast("transfer_request", ChannelMessage("success"))
        }
        result.forError {
            broadcast("transfer_request", ChannelMessage("error"))
        }
        unsubscribe()
    }
}
