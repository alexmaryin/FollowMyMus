package io.github.alexmaryin.followmymus.core.di

import com.arkivanov.decompose.ComponentContext
import io.github.alexmaryin.followmymus.BuildKonfig
import io.github.alexmaryin.followmymus.core.paging.NetworkPagingCount
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.ApiCoversEngine
import io.github.alexmaryin.followmymus.musicBrainz.data.remote.ApiSearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.data.repository.ArtistsPagingSource
import io.github.alexmaryin.followmymus.musicBrainz.data.repository.MediaPagingSource
import io.github.alexmaryin.followmymus.musicBrainz.domain.CoversEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.LocalDbRepository
import io.github.alexmaryin.followmymus.musicBrainz.domain.SearchEngine
import io.github.alexmaryin.followmymus.musicBrainz.domain.SyncRepository
import io.github.alexmaryin.followmymus.screens.login.domain.CMPLoginComponent
import io.github.alexmaryin.followmymus.screens.login.domain.LoginComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.MainPagerComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.domain.mainScreenPager.PagerComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.AccountPage
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.account.domain.nestedNavigation.AccountHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.pageHost.ArtistsHost
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.artists.domain.panelsNavigation.ArtistsHostComponent
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.pageHost.FavoritesHost
import io.github.alexmaryin.followmymus.screens.mainScreen.pages.favorites.domain.panelsNavigation.FavoritesHostComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.CMPSignUpComponent
import io.github.alexmaryin.followmymus.screens.signUp.domain.SignUpComponent
import io.github.alexmaryin.followmymus.sessionManager.data.SupabaseSessionManager
import io.github.alexmaryin.followmymus.sessionManager.domain.SessionManager
import io.github.alexmaryin.followmymus.supabase.data.DefaultSupabaseDb
import io.github.alexmaryin.followmymus.supabase.domain.SupabaseDb
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
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

expect fun getHttpEngine(): HttpClientEngine

@Module
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

    // ---- Explicit interface bindings ----
    // The Koin compiler plugin's @ComponentScan is not discovering @Single /
    // @Factory classes from other source sets in this KMP project (the generated
    // AppModule.module only contains the provider functions above). Registering
    // each interface→implementation pair here is the workaround that works.

    @Single
    fun provideSessionManager(auth: Auth): SessionManager = SupabaseSessionManager(auth)

    @Single
    fun provideSearchEngine(httpClient: HttpClient): SearchEngine = ApiSearchEngine(httpClient)

    @Single
    fun provideCoversEngine(httpClient: HttpClient): CoversEngine = ApiCoversEngine(httpClient)

    @Factory
    fun provideSupabaseDb(supabase: SupabaseClient, auth: Auth): SupabaseDb =
        DefaultSupabaseDb(supabase, auth)

    @Factory
    fun provideArtistsPagingSource(
        searchEngine: SearchEngine,
        @InjectedParam query: String,
        @InjectedParam count: NetworkPagingCount
    ): ArtistsPagingSource = ArtistsPagingSource(searchEngine, query, count)

    @Factory
    fun provideMediaPagingSource(
        searchEngine: SearchEngine,
        @InjectedParam releaseId: String
    ): MediaPagingSource = MediaPagingSource(searchEngine, releaseId)

    @Factory
    fun provideLoginComponent(
        @InjectedParam qrCode: String?,
        @InjectedParam componentContext: ComponentContext,
        @InjectedParam onSignUpClick: () -> Unit
    ): LoginComponent = CMPLoginComponent(qrCode, componentContext, onSignUpClick)

    @Factory
    fun provideSignUpComponent(
        @InjectedParam componentContext: ComponentContext,
        @InjectedParam onLoginClick: () -> Unit
    ): SignUpComponent = CMPSignUpComponent(componentContext, onLoginClick)

    @Factory
    fun providePagerComponent(
        @InjectedParam componentContext: ComponentContext,
        @InjectedParam nickName: String
    ): PagerComponent = MainPagerComponent(componentContext, nickName)

    @Factory
    fun provideArtistsHost(
        @InjectedParam componentContext: ComponentContext
    ): ArtistsHostComponent = ArtistsHost(componentContext)

    @Factory
    fun provideFavoritesHost(
        syncRepository: SyncRepository,
        @InjectedParam componentContext: ComponentContext,
        @InjectedParam nickname: String
    ): FavoritesHostComponent = FavoritesHost(syncRepository, componentContext, nickname)

    @Factory
    fun provideAccountHost(
        sessionManager: SessionManager,
        repository: LocalDbRepository,
        @InjectedParam componentContext: ComponentContext,
        @InjectedParam nickname: String
    ): AccountHostComponent = AccountPage(sessionManager, repository, componentContext, nickname)

    companion object {
        fun supabaseUrl(projectId: String) = "https://$projectId.supabase.co"
    }
}
