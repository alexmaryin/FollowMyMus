package io.github.alexmaryin.followmymus.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class PreferenceSource(private val prefs: Prefs) {

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

    companion object {
        var instance: PreferenceSource? = null

        fun get(prefs: Prefs) = instance ?: PreferenceSource(prefs).also { instance = it }
    }
}

@Composable
fun rememberAppPreferences(prefs: Prefs) = remember { PreferenceSource.get(prefs) }