package io.github.alexmaryin.followmymus.sessionManager.data.qrcode

import io.github.alexmaryin.followmymus.core.forError
import io.github.alexmaryin.followmymus.core.forSuccess
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.ChannelMessage
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionPayload
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import kotlinx.coroutines.flow.take

const val DEEP_LINK_URL_PREFIX = "https://alexmaryin.ru/session-transfer?id="

suspend fun RealtimeChannel.transferSession(sessionManager: SessionManager) {
    subscribe(true)
    broadcast("transfer_request", ChannelMessage("start"))
    val broadcast = broadcastFlow<SessionPayload>(event = "session_payload")
    broadcast.take(1).collect { payload ->
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

suspend fun RealtimeChannel.startTransferSession(sessionManager: SessionManager) {
    subscribe(true)
    val broadcast = broadcastFlow<ChannelMessage>(event = "transfer_request")
    broadcast.take(2).collect { msg ->
        println("SESSION: received message - ${msg.message}")
        when (msg.message) {
            "start" -> {
                val session = sessionManager.currentSession()
                session.forSuccess {
                    val payload = SessionPayload(
                        accessToken = it.accessToken,
                        refreshToken = it.refreshToken,
                        expiresIn = it.expiresIn,
                        tokenType = it.tokenType,
                    )
                    broadcast("session_payload", payload)
                }
                session.forError { error ->
                    println("Session error: ${error.type} ${error.message}")
                    unsubscribe()
                }
            }
            "success" -> {
                println("Session transfer success!")
                unsubscribe()
            }
            "error" -> {
                println("Session transfer error!")
                unsubscribe()
            }
        }
    }
}
