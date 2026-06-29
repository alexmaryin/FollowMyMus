package io.github.alexmaryin.followmymus.core.di

import io.github.alexmaryin.followmymus.BuildKonfig
import io.github.alexmaryin.followmymus.preferences.Prefs
import io.github.alexmaryin.followmymus.preferences.createDataStore
import io.github.alexmaryin.followmymus.preferences.platformPrefsPath
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

expect fun getHttpEngine(): HttpClientEngine

@Module
@ComponentScan("io.github.alexmaryin.followmymus")
class AppModule {

    private val ktorEngine by lazy { getHttpEngine() }

    @Single
    fun provideMusicBrainzClient() = HttpClient(ktorEngine) {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("KTOR CLIENT: $message")
                }
            }
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000L
            connectTimeoutMillis = 15000L
            socketTimeoutMillis = 15000L
        }
        install(HttpRequestRetry) {
            maxRetries = 3
            delayMillis { retry -> retry * 3000L }
        }
    }

    @Single
    fun provideSupabaseClient() = createSupabaseClient(
        supabaseUrl = supabaseUrl(BuildKonfig.projectId),
        supabaseKey = BuildKonfig.publishableKey
    ) {
        coroutineDispatcher = Dispatchers.IO
        httpEngine = ktorEngine
        install(Auth)
        install(Realtime)
        install(Postgrest)
    }

    @Single
    fun provideAuth(supabase: SupabaseClient) = supabase.auth

    @Factory
    fun provideSessionTransferChannel(
        supabase: SupabaseClient,
        @InjectedParam transferId: String
    ) = supabase.channel("session_transfer:$transferId") { isPrivate = false }

    @Single
    fun provideDataStore(): Prefs = createDataStore { platformPrefsPath() }

    companion object {
        fun supabaseUrl(projectId: String) = "https://$projectId.supabase.co"
    }
}
