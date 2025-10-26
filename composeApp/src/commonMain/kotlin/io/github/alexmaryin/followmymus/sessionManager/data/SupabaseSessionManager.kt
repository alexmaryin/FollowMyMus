package io.github.alexmaryin.followmymus.sessionManager.data

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionError
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.event.AuthEvent
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.Flow
import kotlinx.io.IOException
import org.koin.core.annotation.Single

@OptIn(SupabaseExperimental::class)
@Single
class SupabaseSessionManager(
    private val auth: Auth
) : SessionManager {
    override fun sessionStatus(): Flow<SessionStatus> = auth.sessionStatus

    override fun sessionEvents(): Flow<AuthEvent> = auth.events

    override suspend fun signIn(credentials: Credentials): Result<Unit> = safeCall {
        auth.signInWith(Email) {
            email = "${credentials.nickname}${Credentials.SUFFIX}"
            password = credentials.password
        }
    }

    override suspend fun signOut(): Result<Unit> = safeCall {
        auth.signOut()
    }

    override suspend fun signUp(credentials: Credentials): Result<UserInfo?> = safeCall {
        auth.signUpWith(Email) {
            email = "${credentials.nickname}${Credentials.SUFFIX}"
            password = credentials.password
        }
    }

    private suspend fun <T> safeCall(call: suspend () -> T): Result<T> = try {
        Result.Success(call())
    } catch (e: AuthRestException) {
        Result.Error(SessionError.AuthError, e.errorDescription)
    } catch (e: RestException) {
        Result.Error(SessionError.RestError, e.error)
    } catch (e: IOException) {
        Result.Error(SessionError.NetworkError, e.message ?: "Network error without message")
    }
}