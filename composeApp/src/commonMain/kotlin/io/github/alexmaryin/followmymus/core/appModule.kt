package io.github.alexmaryin.followmymus.core

import io.github.alexmaryin.followmymus.BuildKonfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("io.github.alexmaryin.followmymus")
class AppModule() {

    @Single
    fun provideSupabaseClient() = createSupabaseClient(
        supabaseUrl = supabaseUrl(BuildKonfig.projectId),
        supabaseKey = BuildKonfig.publishableKey
    ) { install(Auth) }

    @Factory
    fun provideAuth(supabase: SupabaseClient) = supabase.auth

    companion object {
        fun supabaseUrl(projectId: String) = "https://$projectId.supabase.co"
    }
}
