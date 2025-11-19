package io.github.alexmaryin.followmymus.sessionManager.data

import followmymus.composeapp.generated.resources.Res
import followmymus.composeapp.generated.resources.network_error
import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.model.Credentials
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionError
import io.github.alexmaryin.followmymus.sessionManager.domain.model.SessionPayload
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.event.AuthEvent
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single
import kotlin.time.ExperimentalTime

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

    override fun currentSession(): Result<UserSession> {
        val session = auth.currentSessionOrNull()
        return if (session != null) Result.Success(session)
        else Result.Error(SessionError.SessionExpired)
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun transferSession(sessionPayload: SessionPayload): Result<UserInfo> = safeCall {
        auth.importSession(
            session = UserSession(
                accessToken = sessionPayload.accessToken,
                refreshToken = sessionPayload.refreshToken,
                expiresIn = sessionPayload.expiresIn,
                tokenType = sessionPayload.tokenType
            ),
            autoRefresh = false,
            source = SessionSource.External
        )
        auth.retrieveUserForCurrentSession(true)
    }

    private suspend fun <T> safeCall(call: suspend () -> T): Result<T> = try {
        val result = withContext(Dispatchers.IO) { call() }
        Result.Success(result)
    } catch (e: AuthRestException) {
        Result.Error(SessionError.AuthError(e.errorCode ?: AuthErrorCode.UnexpectedFailure), e.errorDescription)
    } catch (e: RestException) {
        Result.Error(SessionError.RestError(e.statusCode), e.error)
    } catch (e: IOException) {
        Result.Error(SessionError.NetworkError, e.message ?: getString(Res.string.network_error))
    }
}