package io.github.alexmaryin.followmymus.core.di

import io.github.alexmaryin.followmymus.BuildKonfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.serialization.json.Json
import org.koin.core.annotation.*

expect fun getHttpEngine(): HttpClientEngine

@Module
@ComponentScan("io.github.alexmaryin.followmymus.**")
class AppModule() {

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

    companion object {
        fun supabaseUrl(projectId: String) = "https://$projectId.supabase.co"
    }
}
