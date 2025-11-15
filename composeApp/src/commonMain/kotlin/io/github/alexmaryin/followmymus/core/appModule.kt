package io.github.alexmaryin.followmymus.core

import io.github.alexmaryin.followmymus.BuildKonfig
import io.github.alexmaryin.followmymus.screens.signUp.ui.parts.LoginLink
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.logging.Logger
import kotlinx.serialization.json.Json
import org.koin.core.annotation.*

expect fun getHttpEngine(): HttpClientEngine

@Module
@ComponentScan("io.github.alexmaryin.followmymus")
class AppModule() {

    @Single
    fun provideMusicBrainzClient() = HttpClient(getHttpEngine()) {
        install(Logging) {
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }
        install(HttpTimeout)
        install(HttpRequestRetry) {
            maxRetries = 5
            delayMillis { retry -> retry * 3000L }
        }
    }

    @Single
    fun provideSupabaseClient() = createSupabaseClient(
        supabaseUrl = supabaseUrl(BuildKonfig.projectId),
        supabaseKey = BuildKonfig.publishableKey
    ) {
        install(Auth)
        install(Realtime)
    }

    @Factory
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
