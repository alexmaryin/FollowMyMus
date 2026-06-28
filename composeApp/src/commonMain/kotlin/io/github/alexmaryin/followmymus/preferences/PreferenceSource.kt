package io.github.alexmaryin.followmymus.preferences

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.annotation.Single

@Single
class PreferenceSource(private val prefs: Prefs) {

    /**
     * Read-only accessor for the underlying [Prefs] used by the `AppSettings`
     * extensions in `preferences/AppSettings.kt`. Internal so it can be read
     * from the same module without leaking the field's mutability.
     */
    internal val preferences: Prefs get() = prefs

    fun getAndroidDynamicMode(): Flow<DynamicMode> {
        return prefs.data.map {
            val dynamic = it[stringPreferencesKey("dynamic")] ?: DynamicMode.ON.name
            DynamicMode.valueOf(dynamic)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun changeAndroidDynamicMode(newMode: DynamicMode) {
        withContext(Dispatchers.IO) {
            try {
                prefs.edit {
                    it[stringPreferencesKey("dynamic")] = newMode.name
                }
            } catch (e: Exception) {
                println("FAILED TO SAVE PREFERENCES ON THE DISK!")
                e.printStackTrace()
            }
        }
    }

    fun getThemeMode(): Flow<ThemeMode> {
        return prefs.data.map {
            val theme = it[stringPreferencesKey("theme")] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(theme)
        }.flowOn(Dispatchers.IO)
    }

    fun getLanguage(): Flow<Language> {
        return prefs.data.map {
            val language = it[stringPreferencesKey("language")] ?: Language.SYSTEM.name
            Language.valueOf(language)
        }.flowOn(Dispatchers.IO)
    }

    suspend fun changeThemeMode(newMode: ThemeMode) {
        withContext(Dispatchers.IO) {
            try {
                prefs.edit {
                    it[stringPreferencesKey("theme")] = newMode.name
                }
            } catch (e: Exception) {
                println("FAILED TO SAVE PREFERENCES ON THE DISK!")
                e.printStackTrace()
            }
        }
    }

    suspend fun changeLanguage(newLanguage: Language) {
        withContext(Dispatchers.IO) {
            try {
                prefs.edit {
                    it[stringPreferencesKey("language")] = newLanguage.name
                }
            } catch (e: Exception) {
                println("FAILED TO SAVE PREFERENCES ON THE DISK!")
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun rememberAppPreferences(): PreferenceSource = koinInject()