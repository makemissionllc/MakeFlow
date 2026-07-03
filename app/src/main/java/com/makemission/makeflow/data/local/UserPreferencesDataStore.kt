package com.makemission.makeflow.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.userPrefDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesDataStore(private val dataStore: DataStore<Preferences>) {

    val selectedCurrency: Flow<String> = dataStore.data.map { prefs ->
        prefs[SELECTED_CURRENCY_KEY] ?: "KES"
    }

    suspend fun saveCurrency(currency: String) {
        dataStore.edit { prefs ->
            prefs[SELECTED_CURRENCY_KEY] = currency
        }
    }

    val userName: Flow<String> = dataStore.data.map { prefs ->
        prefs[PREF_USER_NAME] ?: ""
    }

    val isProfiled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PREF_IS_PROFILED] ?: false
    }

    suspend fun saveName(name: String) {
        dataStore.edit { prefs ->
            prefs[PREF_USER_NAME] = name
        }
    }

    suspend fun setProfiled() {
        dataStore.edit { prefs ->
            prefs[PREF_IS_PROFILED] = true
        }
    }

    val selectedTheme: Flow<String> = dataStore.data.map { prefs ->
        prefs[SELECTED_THEME_KEY] ?: "forest"
    }

    suspend fun saveTheme(theme: String) {
        dataStore.edit { prefs ->
            prefs[SELECTED_THEME_KEY] = theme
        }
    }

    val hapticsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HAPTICS_ENABLED_KEY] ?: true
    }

    suspend fun saveHapticsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[HAPTICS_ENABLED_KEY] = enabled
        }
    }

    val dismissedSuggestions: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[DISMISSED_SUGGESTIONS_KEY]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    }

    suspend fun dismissSuggestion(key: String) {
        dataStore.edit { prefs ->
            val current = prefs[DISMISSED_SUGGESTIONS_KEY]?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            prefs[DISMISSED_SUGGESTIONS_KEY] = (current + key).joinToString(",")
        }
    }

    val hasPromptedNotificationPermission: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[HAS_PROMPTED_NOTIFICATION_PERMISSION] ?: false
    }

    suspend fun setHasPromptedNotificationPermission() {
        dataStore.edit { prefs ->
            prefs[HAS_PROMPTED_NOTIFICATION_PERMISSION] = true
        }
    }

    suspend fun clearProfile() {
        dataStore.edit { prefs ->
            prefs.remove(PREF_USER_NAME)
            prefs.remove(PREF_IS_PROFILED)
        }
    }

    companion object {
        private val SELECTED_CURRENCY_KEY = stringPreferencesKey("selected_currency")
        private val PREF_USER_NAME = stringPreferencesKey("user_name")
        private val PREF_IS_PROFILED = booleanPreferencesKey("is_profiled")
        private val SELECTED_THEME_KEY = stringPreferencesKey("selected_theme")
        private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")
        private val DISMISSED_SUGGESTIONS_KEY = stringPreferencesKey("dismissed_suggestions")
        private val HAS_PROMPTED_NOTIFICATION_PERMISSION = booleanPreferencesKey("has_prompted_notification_permission")
    }
}
