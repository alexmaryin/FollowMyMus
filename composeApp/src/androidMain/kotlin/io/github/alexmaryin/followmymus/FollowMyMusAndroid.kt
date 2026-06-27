package io.github.alexmaryin.followmymus

import android.app.Application
import io.github.alexmaryin.followmymus.core.FollowMyMusApp
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.plugin.module.dsl.startKoin

class FollowMyMusAndroid : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin<FollowMyMusApp> {
            androidLogger()
            androidContext(this@FollowMyMusAndroid)
//            analytics()
        }
    }
}
