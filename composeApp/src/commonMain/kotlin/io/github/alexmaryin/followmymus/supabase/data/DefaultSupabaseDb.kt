package io.github.alexmaryin.followmymus.supabase.data

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
import io.github.alexmaryin.followmymus.supabase.domain.model.ArtistRemoteEntity
import io.github.alexmaryin.followmymus.supabase.domain.model.SupabaseError
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.*
import kotlinx.serialization.SerializationException
import org.koin.core.annotation.Factory

@Factory(binds = [SupabaseDb::class])
class DefaultSupabaseDb(
    private val supabase: SupabaseClient
) : SupabaseDb {
    override suspend fun getRemoteFavoritesArtists(): Result<List<ArtistRemoteEntity>> = safeCall {
        supabase.from(SupabaseDb.SUPABASE_NAME).select()
            .decodeList<ArtistRemoteEntity>()
    }

    override suspend fun addRemoteFavoriteArtist(artist: ArtistRemoteEntity): Result<Unit> = safeCall {
        val userArtist = artist.copy(userid = supabase.auth.currentSessionOrNull()?.user?.id)
        supabase.from(SupabaseDb.SUPABASE_NAME).upsert(userArtist)
    }

    override suspend fun removeRemoteFavoriteArtist(artistId: String): Result<Unit> = safeCall {
        supabase.from(SupabaseDb.SUPABASE_NAME)
            .delete {
                filter { ArtistRemoteEntity::artistId eq artistId }
            }
    }

    private suspend fun <T> safeCall(call: suspend () -> T) = try {
        val result = call()
        Result.Success(result)
    } catch (e: PostgrestRestException) {
        Result.Error(SupabaseError.SupabaseResponseError(e.message, e.hint))
    } catch (_: HttpRequestTimeoutException) {
        Result.Error(SupabaseError.SupabaseTimeout)
    } catch (e: HttpRequestException) {
        Result.Error(SupabaseError.SupabasNetworkError(e.message))
    } catch (_: SerializationException) {
        Result.Error(SupabaseError.InvalidJsonResponse)
    } catch (_: IllegalArgumentException) {
        Result.Error(SupabaseError.MappingError)
    }
}