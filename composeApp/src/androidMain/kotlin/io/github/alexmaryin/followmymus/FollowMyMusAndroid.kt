package io.github.alexmaryin.followmymus

import android.app.Application
import io.github.alexmaryin.followmymus.core.FollowMyMusApp
import io.kotzilla.sdk.analytics.koin.analytics
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.ksp.generated.startKoin

class FollowMyMusAndroid : Application() {
    override fun onCreate() {
        super.onCreate()

        FollowMyMusApp.startKoin {
            androidLogger()
            androidContext(this@FollowMyMusAndroid)
            modules()
            analytics()
        }
    }
}