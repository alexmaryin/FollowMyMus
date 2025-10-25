package io.github.alexmaryin.followmymus.sessionManager.domain.model

import io.github.jan.supabase.auth.user.UserSession

data class Credentials(
    val nickname: String,
    val password: String
) {
    companion object {
        const val SUFFIX = "@followmymus.local"
    }
}

fun UserSession.getNickname() = user?.email?.removeSuffix(Credentials.SUFFIX)