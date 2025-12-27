package io.github.alexmaryin.followmymus.musicBrainz.data.remote

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.model.api.BrainzApiError
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

internal suspend fun <T> safeCall(call: suspend () -> T) = try {
        val result = withContext(Dispatchers.IO) { call() }
        Result.Success(result)
    } catch (_: HttpRequestTimeoutException) {
        Result.Error(BrainzApiError.Timeout)
    } catch (e: ServerResponseException) {
        Result.Error(BrainzApiError.ServerError(e.message))
    } catch (e: ClientRequestException) {
        Result.Error(BrainzApiError.NetworkError(e.message))
    } catch (_: SerializationException) {
        Result.Error(BrainzApiError.InvalidResponse)
    } catch (_: IllegalArgumentException) {
        Result.Error(BrainzApiError.MappingError)
    } catch (e: NoTransformationFoundException) {
        println(e.message)
        if (e.message.contains("coverart"))
            Result.Error(BrainzApiError.NoCoverError) else
            Result.Error(BrainzApiError.MappingError)
    } catch (e: Exception) {
        Result.Error(BrainzApiError.Unknown(e.message))
    }