package io.github.alexmaryin.followmymus.musicBrainz.domain

import io.github.alexmaryin.followmymus.core.Result
import io.github.alexmaryin.followmymus.musicBrainz.data.local.dao.FavoriteDao
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import kotlin.time.Clock

data class FavoritesExportPayload(
    val bytes: ByteArray,
    val count: Int
)

data class ImportSummary(
    val imported: Int,
    val skipped: Int,
    val failed: Int
)

@Serializable
data class FavoritesExportFile(
    val format: String = "",
    val version: Int = 0,
    val exportedAt: String = "",
    val artists: List<String>? = null
)

@Single(binds = [FavoritesImportExportRepository::class])
class FavoritesImportExportRepository(
    private val favoriteDao: FavoriteDao,
    private val searchEngine: SearchEngine,
    private val artistsRepository: ArtistsRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    suspend fun serializeExport(): Result<FavoritesExportPayload> = withContext(Dispatchers.IO) {
        try {
            val ids = favoriteDao.getFavoriteArtistsIds().first()
            val sorted = ids.sorted()
            val exportFile = FavoritesExportFile(
                format = "followmymus.favorites",
                version = 1,
                exportedAt = Clock.System.now().toString(),
                artists = sorted
            )
            val bytes = json.encodeToString(exportFile).encodeToByteArray()
            Result.Success(FavoritesExportPayload(bytes, count = sorted.size))
        } catch (e: Throwable) {
            Result.Error(FavoritesImportExportError.DataReadError(cause = e.message))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun importFromBytes(bytes: ByteArray, sourceName: String? = null): Result<ImportSummary> {
        val parsed: FavoritesExportFile = try {
            val text = bytes.decodeToString()
            json.decodeFromString<FavoritesExportFile>(text)
        } catch (_: Throwable) {
            return Result.Error(
                FavoritesImportExportError.Malformed,
                message = sourceName?.let { "Failed to parse $it" }
            )
        }

        if (parsed.format != "followmymus.favorites") {
            return Result.Error(
                FavoritesImportExportError.UnsupportedFormat,
                message = sourceName?.let { "Unsupported format in $it" }
            )
        }

        if (parsed.version != 1) {
            return Result.Error(
                FavoritesImportExportError.UnsupportedVersion(parsed.version),
                message = sourceName?.let { "Unsupported version in $it" }
            )
        }

        val artists = parsed.artists ?: return Result.Error(
            FavoritesImportExportError.MissingArtistsField,
            message = sourceName?.let { "Missing artists field in $it" }
        )

        artists.forEachIndexed { index, id ->
            if (id.isBlank()) {
                return Result.Error(
                    FavoritesImportExportError.EmptyArtistEntry(index),
                    message = sourceName?.let { "Empty artist entry at index $index in $it" }
                )
            }
        }

        val uniqueIds = artists.toSet().toList()
        if (uniqueIds.isEmpty()) {
            return Result.Success(ImportSummary(0, 0, 0))
        }

        val existingFavoriteIds = withContext(Dispatchers.IO) {
            favoriteDao.getFavoriteArtistsIds().first().toSet()
        }

        val newIds = uniqueIds.filter { it !in existingFavoriteIds }
        val skipped = uniqueIds.size - newIds.size

        if (newIds.isEmpty()) {
            return Result.Success(ImportSummary(0, skipped, 0))
        }

        val chunks = newIds.chunked(SearchEngine.BATCH_LIMIT)
        var firstError: String? = null

        val total = coroutineScope {
            chunks.asFlow().flatMapMerge(concurrency = 50) { chunk ->
                flow {
                    val dtos = try {
                        searchEngine.searchArtistsByIdBatch(chunk)
                    } catch (e: Exception) {
                        if (firstError == null) firstError = e.message
                        null
                    }

                    if (dtos == null) {
                        emit(chunk.size to 0)
                    } else {
                        val foundIds = dtos.map { it.id }.toSet()
                        val notFound = chunk.count { it !in foundIds }
                        var importedHere = 0
                        for (dto in dtos) {
                            try {
                                artistsRepository.addToFavorite(dto.id)
                                importedHere++
                            } catch (_: Exception) {
                            }
                        }
                        emit(notFound to importedHere)
                    }
                }
            }.fold(0 to 0) { (accFailed, accImported), (notFound, importedHere) ->
                (accFailed + notFound) to (accImported + importedHere)
            }
        }

        val (failed, imported) = total

        if (imported == 0 && skipped == 0 && failed > 0) {
            return Result.Error(
                FavoritesImportExportError.NetworkError(cause = firstError ?: "All requests failed"),
                message = sourceName?.let { "All requests failed for $it" }
            )
        }

        return Result.Success(ImportSummary(imported, skipped, failed))
    }
}
