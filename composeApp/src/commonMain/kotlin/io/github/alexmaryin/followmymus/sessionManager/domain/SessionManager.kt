package io.github.alexmaryin.followmymus.sessionManager.domain

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionPayload
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.event.AuthEvent
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow

@OptIn(SupabaseExperimental::class)
interface SessionManager {
    fun sessionStatus(): Flow<SessionStatus>
    fun sessionEvents(): Flow<AuthEvent>
    suspend fun signIn(credentials: Credentials): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun signUp(credentials: Credentials): Result<UserInfo?>
    fun currentSession(): Result<UserSession>
    suspend fun transferSession(sessionPayload: SessionPayload): Result<Unit>
}